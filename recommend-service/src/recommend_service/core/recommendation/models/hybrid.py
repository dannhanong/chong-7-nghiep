from datetime import datetime
from bson import ObjectId
import numpy as np
import pandas as pd
from recommend_service.db.mongodb import MongoDB
from recommend_service.core.recommendation.models.content_based import ContentBasedRecommender
from recommend_service.core.recommendation.models.collaborative import CollaborativeRecommender
from recommend_service.core.recommendation.models.semantic_content_based import SemanticContentBasedRecommender
import logging
from typing import Dict, List, Any, Optional
from recommend_service.utils.user_utils import get_user_id_from_username, get_email_from_username
from recommend_service.config.settings import settings
import json
from recommend_service.utils.category_utils import get_category_info_by_category_id
from recommend_service.utils.user_utils import get_user_info_by_user_id
import redis

logger = logging.getLogger(__name__)

class HybridRecommender:
    def __init__(self, content_weight=0.4, semantic_weight=0.4, collab_weight=0.2):
        """
        Khởi tạo hệ thống gợi ý kết hợp (hybrid)
        
        Args:
            content_weight: Trọng số cho content-based recommendations
            semantic_weight: Trọng số cho semantic recommendations
            collab_weight: Trọng số cho collaborative recommendations
        """
        self.db = MongoDB()
        self.content_based = ContentBasedRecommender(use_cache=True)
        self.collaborative = CollaborativeRecommender()
        self.semantic_recommender = SemanticContentBasedRecommender()
        self.content_weight = content_weight
        self.semantic_weight = semantic_weight
        self.collab_weight = collab_weight

        self.memory_cache = {}
        self.memory_cache_ttl = 300
        self.redis_client = self._init_redis()

    def _init_redis(self):
        """Khởi tạo redis"""
        try:
            client = redis.Redis(
                host=settings.REDIS_HOST,
                port=settings.REDIS_PORT,
                decode_responses=True,
                socket_timeout=5
            )
            client.ping()
            return client
        except Exception as e:
            logger.error(f"Không thể kết nối đến Redis: {e}")
            return None

    def recommend_jobs(self, 
                      username: None, 
                      filters: None, 
                      page=0,
                      size=10):
        """
        Gợi ý công việc dựa trên người dùng và các bộ lọc và phân trang
        Args:
            username: Tên người dùng (hoặc user_id)
            filters: Các bộ lọc (keyword, salary, category...)
            page: Trang hiện tại
            size: Số lượng công việc trên mỗi trang
        Returns:
            List các công việc được gợi ý
        """

        limit = size * 3  # Lấy nhiều hơn để đảm bảo đủ sau khi lọc

        try:
            # Trường hợp người dùng chưa đăng nhập
            user_id = None
            if username:
                user_id = get_user_id_from_username(username)
                if not user_id:
                    logger.warning(f"Username {username} không tìm thấy trong hệ thống")

            if not user_id:
                logger.info("Người dùng chưa đăng nhập. Sử dụng popular jobs.")
                return self._get_popular_jobs_paged(page, size, filters)
            
            # Lấy thông tin người dùng
            user_profile = self.build_user_profile(user_id)

            content_recommendations = {}
            # if user_profile:
            #     content_results = self.content_based.get_profile_recommendations(
            #         profile_data=user_profile,
            #         limit=limit  # Lấy nhiều hơn để đảm bảo đủ sau khi lọc
            #     )

            #     # Chuyển danh sách kết quả thành dictionary với job_id là key, score là value
            #     content_recommendations = {
            #         str(job['_id']): job.get('similarity_score', 0)
            #         for job in content_results
            #         if self._apply_filters(job, filters)
            #     }

            semantic_recommendations = {}
            if user_profile:
                semantic_results = self.semantic_recommender.get_profile_recommendations(
                    profile_data=user_profile,
                    user_id=user_id,
                    limit=limit
                )
                semantic_recommendations = {
                    str(job['_id']): job.get('similarity_score', 0)
                    for job in semantic_results
                    if self._apply_filters(job, filters)
                }

            collab_recommendations = {}
            interaction_count = self._get_user_interaction_count(user_id)
            
            if interaction_count >= 3:  # Chỉ dùng collaborative khi có đủ dữ liệu
                collab_results = self.collaborative.recommend(user_id, limit=limit)
                collab_recommendations = {
                    job_id: score 
                    for job_id, score in collab_results.items()
                }

            final_recommendations = self._combine_recommendations(
                content_recommendations, 
                collab_recommendations,
                semantic_recommendations,
                limit
            )

            enriched_jobs = self._enrich_recommendations(final_recommendations)

            # Phân trang kết quả
            total_items = len(enriched_jobs)
            total_pages = max(1, (total_items + size - 1) // size)

            if page < 0:
                page = 0
            if page >= total_pages and total_pages > 0:
                page = total_pages - 1

            start_idx = page * size
            end_idx = min(start_idx + size, total_items)

            paged_results = enriched_jobs[start_idx:end_idx]

            jobs_to_remove = []

            # Trả về user info thay vì user_id trong thông tin job
            for job in paged_results:
                if 'userId' in job:
                    if job['userId'] == user_id:
                        jobs_to_remove.append(job)
                    else:
                        user_info = get_user_info_by_user_id(job['userId'])
                        if user_info:
                            print(f"Enriching job {job['_id']} with user info: {user_info}")
                            job['user'] = user_info
                            del job['userId']
                        else:
                            job['user'] = None
                            
                if 'categoryId' in job:
                    category_info = get_category_info_by_category_id(job['categoryId'])
                    if category_info:
                        job['category'] = category_info
                        del job['categoryId']
                        
            for job in jobs_to_remove:
                paged_results.remove(job)

            return {
                "content": paged_results,
                "page": {
                    "number": page,
                    "size": size,
                    "totalElements": total_items,
                    "totalPages": total_pages,
                    "has_next": page < (total_pages - 1),
                    "has_previous": page > 0,
                    "number": page,
                    "number_of_elements": len(paged_results)
                }
            }
            
        except Exception as e:
            logger.error(f"Error in hybrid recommendation: {e}")
            return self._create_empty_page_result(page, size, error=str(e))
        
    def recommend_similar_jobs(self, job_id: str, username=None, page=0, size=10):
        """
        Gợi ý công việc tương tự dựa trên job_id
        
        Args:
            job_id: ID của công việc cần tìm tương tự
            username: Tên người dùng (tùy chọn)
            page: Trang hiện tại
            size: Số lượng công việc trên mỗi trang
            
        Returns:
            Chi tiết công việc và danh sách các công việc tương tự có phân trang
        """
        limit = size * 3

        try:
            try:
                object_job_id = ObjectId(job_id)
            except Exception as e:
                logger.error(f"Invalid job_id format: {e}")
                return self._create_empty_page_result(page, size, error="Invalid job_id format")

            jobs_collection = self.db.get_collection(settings.MONGODB_JOB_DATABASE, settings.MONGODB_JOBS_COLLECTION)
            original_job = jobs_collection.find_one({"_id": object_job_id})

            if not original_job:
                logger.warning(f"Job ID {job_id} không tồn tại")
                return self._create_empty_page_result(page, size, error="Job not found")

            original_job_serialized = self._serialize_mongodb_doc(original_job)

            content_results = self.content_based.get_job_recommendations(
                job_id=job_id,
                limit=limit,
            )

            # Chuyển danh sách kết quả thành dictionary với job_id là key, score là value
            content_recommendations = {
                str(job['_id']): job.get('similarity_score', 0)
                for job in content_results
            }

            collab_recommendations = {}
            if username:
                user_id = get_user_id_from_username(username)
                if user_id:
                    interaction_count = self._get_user_interaction_count(user_id)

                    if interaction_count >= 3:
                        collab_results = self.collaborative.recommend(username=username, limit=limit)
                        collab_recommendations = {
                            job_id: score
                            for job_id, score in collab_results.items()
                        }

            final_recommendations = self._combine_recommendations_similar(
                content_recommendations,
                collab_recommendations,
                limit=limit,
            )

            # Lấy thông tin chi tiết
            enriched_jobs = self._enrich_recommendations(final_recommendations)
            
            # Phân trang kết quả
            total_items = len(enriched_jobs)
            total_pages = max(1, (total_items + size - 1) // size)
            
            if page < 0:
                page = 0
            if page >= total_pages and total_pages > 0:
                page = total_pages - 1
                
            start_idx = page * size
            end_idx = min(start_idx + size, total_items)
            
            paged_results = enriched_jobs[start_idx:end_idx]

            # Trả về user info thay vì user_id trong thông tin job
            for similar_job in paged_results:
                if 'userId' in similar_job:
                    user_info = get_user_info_by_user_id(similar_job['userId'])
                    if user_info:
                        print(f"Enriching job {similar_job['_id']} with user info: {user_info}")
                        similar_job['user'] = user_info
                        del similar_job['userId']
                    else:
                        similar_job['user'] = None

                if 'categoryId' in similar_job:
                    category_info = get_category_info_by_category_id(similar_job['categoryId'])
                    if category_info:
                        similar_job['category'] = category_info
                        del similar_job['categoryId']

                del similar_job['_class']

            return {
                "job": original_job_serialized,
                "similars": paged_results,
            }
        except Exception as e:
            logger.error(f"Error in hybrid similar jobs recommendation: {e}")
            return self._create_empty_page_result(page, size, error=str(e))

    def build_user_profile(self, user_id: str) -> Dict:
        """Xây dựng profile người dùng từ các thông tin skill, experience, education"""
        # profile_cache_key = f"user_job_profile:{user_id}"

        # if self.redis_client:
        #     cached_profile = self.redis_client.get(profile_cache_key)
        #     if cached_profile:
        #         try:
        #             return json.loads(cached_profile)
        #         except json.JSONDecodeError as e:
        #             logger.error(f"Error decoding cached profile for user {user_id}: {e}")

        try:
            print(f"Building profile for user_id: {user_id}")
            # Lấy thông tin kỹ năng
            skills_collection = self.db.get_collection(
                settings.MONGODB_JOB_PROFILE_DATABASE, 
                settings.MONGODB_JOB_PROFILE_SKILL_COLLECTION
            )

            if skills_collection is None:
                logger.error(f"Skills collection not found in database: {settings.MONGODB_JOB_PROFILE_DATABASE}.{settings.MONGODB_JOB_PROFILE_SKILL_COLLECTION}")
                return {}

            skills = list(skills_collection.find({"userId": user_id, "deletedAt": None}))
            
            # Lấy thông tin kinh nghiệm làm việc
            experiences_collection = self.db.get_collection(
                settings.MONGODB_JOB_PROFILE_DATABASE, 
                settings.MONGODB_JOB_PROFILE_EXPERIENCE_COLLECTION
            )
            
            experiences = list(experiences_collection.find({"userId": user_id, "deletedAt": None}))

            print(f"Found {len(skills)} skills and {len(experiences)} experiences for user_id: {user_id}")
                        
            # Tổng hợp thông tin
            skill_names = [skill['skill_name'] for skill in skills]
            job_titles = [exp['company_name'] for exp in experiences]
            
            profile = {
                'skills': ' '.join(skill_names),
                'job_titles': ' '.join(job_titles),
                'experience': ' '.join([exp.get('description', '') for exp in experiences if 'description' in exp]),
                'skill_level': {skill['skill_name']: skill['years_experience'] for skill in skills if 'years_experience' in skill}
            }

            print(f"Built profile for user {user_id}: {json.dumps(profile, indent=2)}")

            # if self.redis_client and profile:
            #     self.redis_client.setex(
            #         profile_cache_key,
            #         21600,
            #         json.dumps(profile)
            #     )

            return profile
        except Exception as e:
            logger.error(f"Error building user profile: {e}")
            return {}
        
    def _apply_filters(self, job: Dict, filters: Optional[Dict]) -> bool:
        """
        Áp dụng các bộ lọc cho job
        
        Args:
            job: Thông tin công việc
            filters: Các bộ lọc (keyword, salary, category...)
        
        Returns:
            True nếu job thỏa mãn tất cả điều kiện lọc
        """
        if not filters:
            return True
            
        # Lọc theo từ khóa
        if filters.get('keyword'):
            keyword = str(filters['keyword']).lower().strip()
            if keyword:
                job_text = f"{job.get('title', '')} {job.get('description', '')} {job.get('skills', '')}".lower()
                if keyword not in job_text:
                    return False
                
        # Lọc theo danh mục
        if filters.get('category_ids'):
            job_category_id = str(job.get('categoryId', ''))
    
            if filters.get('category_ids'):
                category_ids = filters['category_ids']
                
                # Handle different input formats
                if isinstance(category_ids, str):
                    category_list = [cat.strip() for cat in category_ids.split(',') if cat.strip()]
                elif isinstance(category_ids, list):
                    category_list = [str(cat) for cat in category_ids]
                else:
                    logger.warning(f"Invalid category_ids format: {type(category_ids)}")
                    return False
                
                # Check if job's category is in the allowed list
                if job_category_id not in category_list:
                    return False
                
        # Lọc theo mức lương
        if 'salary_min' in filters and job.get('salaryMin', 0) < filters['salary_min']:
            return False
            
        if 'salary_max' in filters and filters['salary_max'] > 0 and job.get('salaryMax', float('inf')) > filters['salary_max']:
            return False
            
        return True
    
    def _get_user_interaction_count(self, user_id: str) -> int:
        """Đếm số tương tác của người dùng với job (ứng tuyển, xem chi tiết, bookmark...)"""
        try:
            interactions_collection = self.db.get_collection("jobs_job", "interactions")
            return interactions_collection.count_documents({"userId": user_id})
        except Exception as e:
            logger.error(f"Error counting user interactions: {e}")
            return 0
        
    def _combine_recommendations(self, 
                               content_recs: Dict[str, float], 
                               collab_recs: Dict[str, float],
                               semantic_recs: Dict[str, float],
                               limit: int) -> Dict[str, float]:
        """
        Kết hợp kết quả từ 3 phương pháp recommendation với trọng số
        
        Args:
            content_recs: Dictionary {job_id: score} từ content-based
            collab_recs: Dictionary {job_id: score} từ collaborative
            semantic_recs: Dictionary {job_id: score} từ semantic
            limit: Số lượng kết quả tối đa

        Returns:
            Dictionary {job_id: final_score} đã được kết hợp và sắp xếp
        """
        combined = {}
        
        # Chuẩn hóa điểm số (min-max normalization)
        def normalize_scores(scores):
            if not scores:
                return {}
            min_score = min(scores.values())
            max_score = max(scores.values())
            if max_score == min_score:
                return {k: 1.0 for k in scores}
            return {k: (v - min_score) / (max_score - min_score) for k, v in scores.items()}
        
        normalized_content = normalize_scores(content_recs)
        normalized_semantic = normalize_scores(semantic_recs)
        normalized_collab = normalize_scores(collab_recs)

        effective_content_weight = self.content_weight if content_recs else 0
        effective_semantic_weight = self.semantic_weight if semantic_recs else 0
        effective_collab_weight = self.collab_weight if collab_recs else 0

        total_weight = effective_content_weight + effective_semantic_weight + effective_collab_weight

        if total_weight == 0:
            return {}
        
        effective_content_weight = self.content_weight / total_weight
        effective_semantic_weight = self.semantic_weight / total_weight
        effective_collab_weight = self.collab_weight / total_weight
        
        # Kết hợp với trọng số
        all_job_ids = set(normalized_content.keys()) | set(normalized_semantic.keys()) | set(normalized_collab.keys())

        for job_id in all_job_ids:
            content_score = normalized_content.get(job_id, 0) * effective_content_weight
            semantic_score = normalized_semantic.get(job_id, 0) * effective_semantic_weight
            collab_score = normalized_collab.get(job_id, 0) * effective_collab_weight
            combined[job_id] = content_score + semantic_score + collab_score

        # Sắp xếp theo điểm số và giới hạn số lượng
        sorted_recommendations = sorted(combined.items(), key=lambda x: x[1], reverse=True)
        return {k: v for k, v in sorted_recommendations[:limit]}
    
    def _combine_recommendations_similar(self, 
                               content_recs: Dict[str, float],
                               semantic_recs: Dict[str, float],
                               limit: int) -> Dict[str, float]:
        """
        Kết hợp kết quả từ 2 phương pháp recommendation với trọng số

        Args:
            content_recs: Dictionary {job_id: score} từ content-based
            semantic_recs: Dictionary {job_id: score} từ semantic
            limit: Số lượng kết quả tối đa

        Returns:
            Dictionary {job_id: final_score} đã được kết hợp và sắp xếp
        """
        combined = {}
        
        # Chuẩn hóa điểm số (min-max normalization)
        def normalize_scores(scores):
            if not scores:
                return {}
            min_score = min(scores.values())
            max_score = max(scores.values())
            if max_score == min_score:
                return {k: 1.0 for k in scores}
            return {k: (v - min_score) / (max_score - min_score) for k, v in scores.items()}
        
        normalized_content = normalize_scores(content_recs)
        normalized_semantic = normalize_scores(semantic_recs)

        effective_content_weight = self.content_weight if content_recs else 0
        effective_semantic_weight = self.semantic_weight if semantic_recs else 0

        total_weight = effective_content_weight + effective_semantic_weight

        if total_weight == 0:
            return {}
        
        effective_content_weight = self.content_weight / total_weight
        effective_semantic_weight = self.semantic_weight / total_weight

        # Kết hợp với trọng số
        all_job_ids = set(normalized_content.keys()) | set(normalized_semantic.keys())

        for job_id in all_job_ids:
            content_score = normalized_content.get(job_id, 0) * effective_content_weight
            semantic_score = normalized_semantic.get(job_id, 0) * effective_semantic_weight
            combined[job_id] = content_score + semantic_score

        # Sắp xếp theo điểm số và giới hạn số lượng
        sorted_recommendations = sorted(combined.items(), key=lambda x: x[1], reverse=True)
        return {k: v for k, v in sorted_recommendations[:limit]}
    
    def _enrich_recommendations(self, recommendations: Dict[str, float]) -> List[Dict]:
        """
        Thêm thông tin chi tiết cho các job được đề xuất và chuyển đổi ObjectId thành chuỗi
        """
        if not recommendations:
            return []
        try:
            # Lấy thông tin chi tiết từ database
            jobs_collection = self.db.get_collection(settings.MONGODB_JOB_DATABASE, settings.MONGODB_JOBS_COLLECTION)
            job_ids = [ObjectId(job_id) if not isinstance(job_id, ObjectId) else job_id 
                    for job_id in recommendations.keys()]
                        
            jobs = list(jobs_collection.find({
                "_id": {"$in": job_ids},
                "active": True,
                "status": True,
                "deletedAt": None
            }))
            
            # Chuyển đổi các jobs và thêm điểm đề xuất
            serialized_jobs = []
            for job in jobs:
                # Chuyển đổi ObjectId thành str
                job_dict = self._serialize_mongodb_doc(job)
                
                # Thêm điểm đề xuất (dùng str(job['_id']) để so sánh với key trong recommendations)
                original_id = str(job['_id'])

                job_dict['recommendation_score'] = recommendations.get(original_id, 0)
                
                serialized_jobs.append(job_dict)
                
            # Sắp xếp theo điểm đề xuất
            return sorted(serialized_jobs, key=lambda x: x['recommendation_score'], reverse=True)
            
        except Exception as e:
            logger.error(f"Error enriching recommendations: {e}")
            return []
        
    def _serialize_mongodb_doc(self, doc: Any) -> Any:
        """
        Chuyển đổi đệ quy document MongoDB sang định dạng có thể JSON serializable
        """
        if isinstance(doc, dict):
            return {k: self._serialize_mongodb_doc(v) for k, v in doc.items()}
        elif isinstance(doc, list):
            return [self._serialize_mongodb_doc(item) for item in doc]
        elif isinstance(doc, ObjectId):
            return str(doc)
        elif isinstance(doc, datetime):
            return doc.isoformat()
        else:
            return doc
        
    def _get_popular_jobs_paged(self, page, size, filters=None):
        """Lấy danh sách job phổ biến với phân trang và bộ lọc"""
        try:
            limit = size * 3  # Lấy nhiều hơn để đảm bảo đủ sau khi lọc
            start_time = datetime.now()

            popular_jobs_dict = self.collaborative.get_popular_jobs(limit=limit)

            all_jobs = self._enrich_recommendations(popular_jobs_dict)

            if filters:
                filtered_jobs = [job for job in all_jobs if self._apply_filters(job, filters)]
            else:
                filtered_jobs = all_jobs

            # Phân trang kết quả
            total_items = len(filtered_jobs)
            total_pages = max(1, (total_items + size - 1) // size)

            if page < 0:
                page = 0
            if page >= total_pages and total_pages > 0:
                page = total_pages - 1

            start_idx = page * size
            end_idx = min(start_idx + size, total_items)

            paged_results = filtered_jobs[start_idx:end_idx]
            process_time_ms = int((datetime.now() - start_time).total_seconds() * 1000)

            return {
                "content": paged_results,
                "page_info": {
                    "page": page,
                    "size": size,
                    "total_elements": total_items,
                    "total_pages": total_pages,
                    "has_next": page < (total_pages - 1),
                    "has_previous": page > 0,
                    "number": page,
                    "number_of_elements": len(paged_results)
                },
                "metadata": {
                    "query_time_ms": process_time_ms,
                    "filters_applied": filters is not None,
                    "authenticated": False
                }
            }
        except Exception as e:
            logger.error(f"Error in getting popular jobs: {e}")
            return self._create_empty_page_result(page, size, error=str(e))
        
    def _create_empty_page_result(self, page, size, error=None):
        """Tạo kết quả phân trang trống khi không có dữ liệu hoặc có lỗi"""
        result = {
            "content": [],
            "page_info": {
                "page": page,
                "size": size,
                "total_elements": 0,
                "total_pages": 0,
                "has_next": False,
                "has_previous": False,
                "number": page,
                "number_of_elements": 0
            },
            "metadata": {
                "query_time_ms": 0,
                "filters_applied": False,
                "authenticated": False
            }
        }
        
        if error:
            result["metadata"]["error"] = error
            result["metadata"]["status"] = "error"
        
        return result
    
    def recommend_jobs_for_user_gmail(self,
                                      list_user_username: List[str]) -> List[Dict]:
        """
        Lọc các công việc phổ biến cho một danh sách người dùng dựa trên tên đăng nhập
        Args:
            list_user_username: Danh sách tên đăng nhập của người dùng
        Returns:
            Dictionary với key là tên đăng nhập và value là danh sách các công việc được gợi ý
        """
        recommend_jobs_to_send = []
        for username in list_user_username:
            all_jobs = self.recommend_jobs(
                username=username,
                filters=None,
                page=0,
                size=40
            ).get("content", [])

            filtered_jobs = [job for job in all_jobs
                             if job.get('recommendation_score', 0) >= 0.47]
            
            filtered_job_ids = {str(job['_id']) for job in filtered_jobs}

            # recommend_jobs_to_send[get_email_from_username(username)] = filtered_job_ids if filtered_job_ids else None
            recommend_jobs_to_send.append({
                "username": username,
                "job_ids": filtered_job_ids if filtered_job_ids else None
            })
        return recommend_jobs_to_send