import numpy as np
import pandas as pd
from sklearn.metrics.pairwise import cosine_similarity
from scipy.sparse import csr_matrix
from recommend_service.db.mongodb import MongoDB
import logging
import pickle
import os
from datetime import datetime, timedelta
from typing import Dict, List, Tuple, Optional
from recommend_service.core.recommendation.models.content_based import ContentBasedRecommender
from recommend_service.utils.user_utils import get_user_id_from_username
from recommend_service.config.settings import settings

logger = logging.getLogger(__name__)

class CollaborativeRecommender:
    def __init__(self, use_cache=True, cache_ttl=3600, cache_dir="cache"):
        """
        Khởi tạo Collaborative Recommender
        
        Args:
            use_cache: Có sử dụng cache hay không
            cache_ttl: Thời gian sống của cache (giây)
            cache_dir: Thư mục để lưu cache
        """

        self.db = MongoDB()
        self.use_cache = use_cache
        self.cache_ttl = cache_ttl
        self.cache_dir = cache_dir

        self.interaction_weights = {
            'application': 5.0,    # Ứng tuyển (mạnh nhất)
            'bookmark': 3.0,       # Bookmark/Lưu lại
            'view': 1.0,           # Xem chi tiết
            'rating': 4.0,         # Đánh giá
            'click': 0.5,          # Click vào kết quả tìm kiếm
        }

        if self.use_cache and not os.path.exists(self.cache_dir):
            os.makedirs(self.cache_dir)

    def _get_cache_path(self):
        """Tạo đường dẫn file cache cho collab model"""
        return os.path.join(self.cache_dir, "collaborative_model.pkl")
    
    def _is_cache_valid(self, cache_path):
        """Kiểm tra xem cache còn hợp lệ không dựa trên thời gian tạo"""
        if not os.path.exists(cache_path):
            return False
        
        # Cache hết hạn sau cache_ttl giây
        file_mod_time = datetime.fromtimestamp(os.path.getmtime(cache_path))
        return datetime.now() - file_mod_time < timedelta(seconds=self.cache_ttl)
    
    def fit(self, force_rebuild=False):
        """Xây dựng ma trận user-item để tính toán các đề xuất dựa trên tương tác"""
        try:
            cache_path = self._get_cache_path()
            
            # Nếu cache còn hiệu lực và không force rebuild
            if self.use_cache and self._is_cache_valid(cache_path) and not force_rebuild:
                with open(cache_path, 'rb') as f:
                    cache_data = pickle.load(f)
                    self.user_item_matrix = cache_data['user_item_matrix']
                    self.user_indices = cache_data['user_indices']
                    self.item_indices = cache_data['item_indices']
                    self.similarity_matrix = cache_data['similarity_matrix']
                logger.info("Loaded collaborative model from cache.")
                return True
            
            # Thu thập dữ liệu tương tác
            interactions = self._collect_interactions()
            
            if len(interactions) == 0:
                logger.warning("No interaction data found.")
                return False
            
            # Tạo user-item matrix
            interactions_df = pd.DataFrame(interactions)
            
            # Chuyển đổi tương tác thành numeric values (dùng interaction weights)
            interactions_df['interaction_score'] = interactions_df['interaction_type'].map(
                self.interaction_weights
            )
            
            # Nếu có nhiều tương tác cùng loại, lấy tổng điểm
            interaction_matrix = interactions_df.pivot_table(
                index='user_id', 
                columns='job_id', 
                values='interaction_score', 
                aggfunc='sum',
                fill_value=0
            )
            
            # Lưu mapping giữa user ID và index trong matrix
            self.user_indices = {uid: i for i, uid in enumerate(interaction_matrix.index)}
            self.item_indices = {jid: i for i, jid in enumerate(interaction_matrix.columns)}
            
            # Tạo sparse matrix để tiết kiệm bộ nhớ
            self.user_item_matrix = csr_matrix(interaction_matrix.values)
            
            # Tính ma trận tương đồng giữa các user
            self.similarity_matrix = cosine_similarity(self.user_item_matrix)
            
            # Lưu vào cache
            if self.use_cache:
                with open(cache_path, 'wb') as f:
                    pickle.dump({
                        'user_item_matrix': self.user_item_matrix,
                        'user_indices': self.user_indices,
                        'item_indices': self.item_indices,
                        'similarity_matrix': self.similarity_matrix
                    }, f)
                logger.info(f"Collaborative model saved to cache at {cache_path}.")
                
            return True
            
        except Exception as e:
            logger.error(f"Error building collaborative model: {e}")
            return False
        
    def _collect_interactions(self) -> List[Dict]:
        """Thu thập tất cả các tương tác người dùng với job"""
        try:
            # Lấy dữ liệu ứng tuyển
            applications = self._get_applications()
            
            # Lấy dữ liệu bookmark
            bookmarks = self._get_bookmarks()
            
            # Lấy dữ liệu view chi tiết
            views = self._get_views()
            
            # Lấy dữ liệu đánh giá
            ratings = self._get_ratings()
            
            # Lấy dữ liệu click
            clicks = self._get_clicks()
            
            # Kết hợp tất cả dữ liệu
            all_interactions = applications + bookmarks + views + ratings + clicks
            
            return all_interactions
        except Exception as e:
            logger.error(f"Error collecting interactions: {e}")
            return []
        
    def _get_applications(self) -> List[Dict]:
        """Lấy dữ liệu về việc người dùng ứng tuyển vào công việc"""
        try:
            applications_collection = self.db.get_collection("jobs_job", "applications")
            applications = list(applications_collection.find(
                {"status": {"$ne": "rejected"}},  # Không lấy các ứng tuyển bị từ chối
                {"userId": 1, "jobId": 1}
            ))
            
            return [
                {"user_id": app["userId"], "job_id": app["jobId"], "interaction_type": "application"}
                for app in applications
            ]
        except Exception as e:
            logger.error(f"Error getting applications: {e}")
            return []
    
    def _get_bookmarks(self) -> List[Dict]:
        """Lấy dữ liệu về việc người dùng bookmark/save job"""
        try:
            bookmarks_collection = self.db.get_collection("jobs_job", "bookmarks")
            bookmarks = list(bookmarks_collection.find(
                {"active": True},
                {"userId": 1, "jobId": 1}
            ))
            
            return [
                {"user_id": bm["userId"], "job_id": bm["jobId"], "interaction_type": "bookmark"}
                for bm in bookmarks
            ]
        except Exception as e:
            logger.error(f"Error getting bookmarks: {e}")
            return []
    
    def _get_views(self) -> List[Dict]:
        """Lấy dữ liệu về việc người dùng xem chi tiết job"""
        try:
            views_collection = self.db.get_collection("jobs_job", "job_views")
            views = list(views_collection.find(
                {"viewDuration": {"$gt": 10}},  # Chỉ lấy các view có thời gian > 10 giây
                {"userId": 1, "jobId": 1}
            ))
            
            return [
                {"user_id": view["userId"], "job_id": view["jobId"], "interaction_type": "view"}
                for view in views
            ]
        except Exception as e:
            logger.error(f"Error getting job views: {e}")
            return []
    
    def _get_ratings(self) -> List[Dict]:
        """Lấy dữ liệu về việc người dùng đánh giá job"""
        try:
            ratings_collection = self.db.get_collection("jobs_job", "job_ratings")
            ratings = list(ratings_collection.find(
                {"rating": {"$gte": 3}},  # Chỉ lấy đánh giá từ 3 sao trở lên
                {"userId": 1, "jobId": 1}
            ))
            
            return [
                {"user_id": rating["userId"], "job_id": rating["jobId"], "interaction_type": "rating"}
                for rating in ratings
            ]
        except Exception as e:
            logger.error(f"Error getting job ratings: {e}")
            return []
    
    def _get_clicks(self) -> List[Dict]:
        """Lấy dữ liệu về việc người dùng click vào job trong kết quả tìm kiếm"""
        try:
            clicks_collection = self.db.get_collection("jobs_job", "search_clicks")
            clicks = list(clicks_collection.find(
                {"timestamp": {"$gte": datetime.now() - timedelta(days=30)}},  # Chỉ lấy clicks trong 30 ngày gần nhất
                {"userId": 1, "jobId": 1}
            ))
            
            return [
                {"user_id": click["userId"], "job_id": click["jobId"], "interaction_type": "click"}
                for click in clicks
            ]
        except Exception as e:
            logger.error(f"Error getting search clicks: {e}")
            return []
            
    def recommend(self, username: str, limit: int = 50) -> Dict[str, float]:
        """
        Đề xuất jobs cho người dùng dựa vào tương tác của người dùng khác tương tự
        
        Args:
            user_id: ID của người dùng cần đề xuất
            limit: Số lượng đề xuất tối đa
            
        Returns:
            Dictionary {job_id: score} đã sắp xếp theo thứ tự giảm dần
        """
        try:
            user_id = None
            if username:
                user_id = get_user_id_from_username(username)
                if not user_id:
                    logger.warning(f"User {username} not found in database.")

            # Đảm bảo model đã được xây dựng
            if not hasattr(self, 'user_item_matrix'):
                success = self.fit()
                if not success:
                    logger.warning("Failed to build collaborative model.")
                    return {}
            
            # Nếu người dùng không có trong dữ liệu training
            if user_id not in self.user_indices:
                logger.info(f"User {user_id} not found in training data. Unable to make collaborative recommendations.")
                has_profile = self._has_user_profile(user_id)

                if has_profile:
                    logger.info(f"Using content-based recommendations for new user {user_id}")
                    return self._get_content_based_recommendations(user_id, limit)
                else:
                    logger.info(f"Using popular jobs for new user {user_id} without profile")
                    return self.get_popular_jobs(limit)
            
            user_idx = self.user_indices[user_id]
            
            # Lấy các user tương tự nhất với user hiện tại
            similar_users = np.argsort(self.similarity_matrix[user_idx])[::-1][1:21]  # Top 20 người dùng tương tự
            
            # Lấy các item mà user hiện tại chưa tương tác
            user_row = self.user_item_matrix[user_idx].toarray().flatten()
            uninteracted_items = np.where(user_row == 0)[0]
            
            # Điểm số đề xuất cho mỗi job
            recommendations = {}
            
            # Duyệt qua mỗi item chưa tương tác
            for item_idx in uninteracted_items:
                # Tính điểm dựa trên người dùng tương tự
                weighted_sum = 0
                similarity_sum = 0
                
                for similar_user_idx in similar_users:
                    # Lấy điểm tương tác của người dùng tương tự với item này
                    interaction_score = self.user_item_matrix[similar_user_idx, item_idx]
                    
                    # Nếu người dùng tương tự có tương tác với item này
                    if interaction_score > 0:
                        # Lấy độ tương đồng giữa người dùng hiện tại và người dùng tương tự
                        similarity = self.similarity_matrix[user_idx, similar_user_idx]
                        
                        # Tính tổng điểm có trọng số
                        weighted_sum += similarity * interaction_score
                        similarity_sum += similarity
                
                # Nếu có ít nhất một người dùng tương tự tương tác với item này
                if similarity_sum > 0:
                    # Tính điểm dự đoán
                    prediction = weighted_sum / similarity_sum
                    
                    # Lấy job_id tương ứng với item_idx
                    job_id = next((jid for jid, idx in self.item_indices.items() if idx == item_idx), None)
                    
                    if job_id:
                        recommendations[job_id] = float(prediction)
            
            # Sắp xếp theo điểm dự đoán giảm dần và giới hạn số lượng
            sorted_recommendations = {
                job_id: score 
                for job_id, score in sorted(recommendations.items(), key=lambda x: x[1], reverse=True)
                if score > 0
            }
            
            # Giới hạn số lượng recommendations
            result = {job_id: score for i, (job_id, score) in enumerate(sorted_recommendations.items()) if i < limit}
            
            return result
            
        except Exception as e:
            logger.error(f"Error getting recommendations: {e}")
            return {}
        
    def _has_user_profile(self, user_id: str) -> bool:
        """Kiểm tra xem user có profile hay không"""
        try:
            users_collection = self.db.get_collection(settings.MONGODB_USER_DATABASE, settings.MONGODB_USERS_COLLECTION)
            user = users_collection.find_one({"_id": user_id})
            
            if not user:
                return False
                
            # Kiểm tra xem có đủ thông tin profile không
            skills_collection = self.db.get_collection(settings.MONGODB_JOB_PROFILE_DATABASE, settings.MONGODB_JOB_PROFILE_SKILL_COLLECTION)
            skills_count = skills_collection.count_documents({"userId": user_id})

            experiences_collection = self.db.get_collection(settings.MONGODB_JOB_PROFILE_DATABASE, settings.MONGODB_JOB_PROFILE_EXPERIENCE_COLLECTION)
            experiences_count = experiences_collection.count_documents({"userId": user_id})
            
            # Có ít nhất skill hoặc experience
            return skills_count > 0 or experiences_count > 0
        
        except Exception as e:
            logger.error(f"Error checking user profile: {e}")
            return False
        
    def _get_content_based_recommendations(self, user_id: str, limit: int) -> Dict[str, float]:
        """
        Lấy gợi ý dựa trên nội dung từ profile người dùng
        """
        try:
            content_recommender = ContentBasedRecommender()
            results = content_recommender.get_profile_recommendations(user_id, limit)
            
            # Chuyển đổi kết quả về định dạng {job_id: score}
            return {job['_id']: job['similarity_score'] for job in results if 'similarity_score' in job}
        
        except Exception as e:
            logger.error(f"Error getting content-based recommendations: {e}")
            return self.get_popular_jobs(limit)
        
    def get_popular_jobs(self, limit: int = 10) -> Dict[str, float]:
        """
        Trả về các công việc phổ biến nhất dựa trên tương tác.
        Sử dụng cho người dùng ẩn danh hoặc mới.
        
        Args:
            limit: Số lượng công việc trả về tối đa
            
        Returns:
            Dictionary {job_id: popularity_score}
        """
        try:
            # Lấy tất cả các tương tác từ các collections
            all_interactions = self._collect_interactions()

            if not all_interactions:
                logger.warning("No interactions found for popularity calculation")
                return self._get_recent_jobs(limit)
        
            # Tính điểm phổ biến cho mỗi job
            job_popularity = {}

            for interaction in all_interactions:
                job_id = interaction['job_id']
                interaction_type = interaction['interaction_type']

                # Lấy trọng số cho loại tương tác này
                weight = self.interaction_weights.get(interaction_type, 0.5)

                # Tính điểm phổ biến dựa trên tổng trọng số
                if job_id in job_popularity:
                    job_popularity[job_id] += weight
                else:
                    job_popularity[job_id] = weight

            # Sắp xếp theo điểm phổ biến giảm dần
            sorted_jobs = sorted(job_popularity.items(), key=lambda x: x[1], reverse=True)
            return {
                job_id: score for job_id, score in sorted_jobs[:limit]
            }
        
        except Exception as e:
            logger.error(f"Error getting popular jobs: {e}")
            return self._get_recent_jobs(limit)
        
    def _get_recent_jobs(self, limit: int = 10) -> Dict[str, float]:
        """
        Trả về các công việc mới nhất khi không có dữ liệu tương tác
        
        Args:
            limit: Số lượng công việc trả về tối đa
            
        Returns:
            Dictionary {job_id: recency_score}
        """
        try:
            jobs_collection = self.db.get_collection(settings.MONGODB_JOB_DATABASE, settings.MONGODB_JOBS_COLLECTION)

            recent_jobs = list(jobs_collection.find(
                {"active": True, "status": True, "deletedAt": None},
                {"_id": 1, "createdAt": 1}
            ).sort("createdAt", -1).limit(limit))

            now = datetime.now()
            max_days = 30

            result = {}

            for job in recent_jobs:
                job_id = job["_id"]
                days_old = (now - job["createdAt"]).days if "createdAt" in job else max_days
                days_old = min(days_old, max_days)

                score = 1 - (days_old / max_days)
                result[job_id] = score
            return result
        except Exception as e:
            logger.error(f"Error getting recent jobs: {e}")
            return {}