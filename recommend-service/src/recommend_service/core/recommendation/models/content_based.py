from bson import ObjectId
import numpy as np;
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import pandas as pd
from recommend_service.db.mongodb import MongoDB
import logging
import pickle
import os
from datetime import datetime
import hashlib
from recommend_service.utils.user_utils import get_user_id_from_username
from recommend_service.config.settings import settings

logger = logging.getLogger(__name__)

class ContentBasedRecommender:
    def __init__(self, use_cache=True, cache_ttl=3600, cache_dir="cache"):
        self.vectorizer = TfidfVectorizer(
            analyzer='word',
            min_df=0.0,
            max_df=1.0,
            ngram_range=(1, 2),
            stop_words='english',
        )
        self.use_cache = use_cache
        self.cache_ttl = cache_ttl
        self.cache_dir = cache_dir
        self.db = MongoDB()

        if self.use_cache and not os.path.exists(self.cache_dir):
            os.makedirs(self.cache_dir)

    def _get_cache_path(self, collection_name):
        """Tạo đường dẫn file cache dựa trên tên collection"""
        return os.path.join(self.cache_dir, f"{collection_name}_model.pkl")
    
    def _is_cache_valid(self, cache_path):
        """Kiểm tra xem cache còn hợp lệ không dựa trên thời gian tạo"""
        if not os.path.exists(cache_path):
            return False
        created_time = os.path.getmtime(cache_path)
        current_time = datetime.now().timestamp()
        return (current_time - created_time) < self.cache_ttl
    
    def _preprocess_text(self, text):
        """Tiền xử lý văn bản tiếng Việt"""
        if not text:
            return ""
        
        text = text.lower()
        return text
        
    def fit(self, force_rebuild=False):
        """Xây dựng mô hình TF-IDF từ dữ liệu công việc"""
        try:
            cache_path = self._get_cache_path("jobs")

            # Thêm log chi tiết hơn
            logger.info(f"Starting fit() method. Force rebuild: {force_rebuild}")

            # Kiểm tra cache
            if self.use_cache and self._is_cache_valid(cache_path) and not force_rebuild:
                try:
                    with open(cache_path, 'rb') as f:
                        cache_data = pickle.load(f)
                        self.jobs_df = cache_data['jobs_df']
                        self.tfidf_matrix = cache_data['tfidf_matrix']
                        self.feature_names = cache_data['feature_names']
                    
                    # Kiểm tra dữ liệu đã load
                    if hasattr(self, 'tfidf_matrix') and hasattr(self.vectorizer, 'vocabulary_'):
                        logger.info("Loaded TF-IDF model from cache successfully.")
                        return True
                    else:
                        logger.warning("Cache loaded but data is incomplete. Rebuilding model.")
                except Exception as ce:
                    logger.error(f"Error loading cache: {ce}")
                    # Tiếp tục xây dựng mô hình mới nếu cache lỗi
            
            # Code để lấy dữ liệu từ MongoDB
            jobs_collection = self.db.get_collection(settings.MONGODB_JOB_DATABASE, settings.MONGODB_JOBS_COLLECTION)
            jobs_data = list(jobs_collection.find({}))

            logger.info(f"Total jobs fetched from database: {len(jobs_data)}")

            if not jobs_data:
                logger.warning("No jobs data found in the database.")
                return False
            
            # Tạo DataFrame
            self.jobs_df = pd.DataFrame(jobs_data)
            
            logger.info(f"Created DataFrame with shape: {self.jobs_df.shape}")
            
            # Kiểm tra dữ liệu
            if self.jobs_df.empty:
                logger.warning("Jobs DataFrame is empty")
                return False

            # Tạo cột content
            self.jobs_df['content'] = self.jobs_df.apply(
                lambda row: ' '.join([
                    str(row.get('title', '')) * 3,
                    str(row.get('description', '')),
                    str(row.get('benefits', '')),
                    str(row.get('experienceLevel', '')) * 2
                ]),
                axis=1
            )

            print(self.jobs_df['content'].head())

            # Kiểm tra nội dung
            if self.jobs_df['content'].str.strip().str.len().sum() == 0:
                logger.warning("All job content is empty")
                return False
                
            logger.info("Fitting TF-IDF vectorizer...")
            self.tfidf_matrix = self.vectorizer.fit_transform(self.jobs_df['content'])
            self.feature_names = self.vectorizer.get_feature_names_out()
            
            logger.info(f"TF-IDF matrix created with shape {self.tfidf_matrix.shape}")
            
            # Kiểm tra xem vectorizer đã được fitted chưa
            if not hasattr(self.vectorizer, 'vocabulary_'):
                logger.error("Vectorizer was not fitted properly")
                return False
            
            # Lưu cache
            if self.use_cache:
                try:
                    with open(cache_path, 'wb') as f:
                        pickle.dump({
                            'jobs_df': self.jobs_df,
                            'tfidf_matrix': self.tfidf_matrix,
                            'feature_names': self.feature_names
                        }, f)
                    logger.info(f"TF-IDF model saved to cache at {cache_path}.")
                except Exception as se:
                    logger.error(f"Error saving cache: {se}")
                    # Tiếp tục vì đã xây dựng mô hình thành công
            
            return True
        
        except Exception as e:
            logger.error(f"Error building content-based model: {e}")
            return False
        
    def get_profile_recommendations(self, profile_data, limit=50):
        """Lấy gợi ý công việc dựa trên thông tin hồ sơ người dùng"""
        try:
            # Đảm bảo mô hình đã được xây dựng
            if not hasattr(self, 'tfidf_matrix'):
                logger.info("TF-IDF matrix not found, fitting model...")
                success = self.fit()
                if not success:
                    logger.error("Failed to build TF-IDF model")
                    return []
                
            logger.info(f"Profile data received: {profile_data}")
            
            # Xử lý dữ liệu profile
            skills = self._preprocess_text(str(profile_data.get('skills', '')))
            experience = self._preprocess_text(str(profile_data.get('experience', '')))
            education = self._preprocess_text(str(profile_data.get('education', '')))
            job_titles = self._preprocess_text(str(profile_data.get('job_titles', '')))
            
            logger.info(f"Processed skills: {skills}")
            
            # Tạo nội dung từ profile
            profile_content = ' '.join([
                skills * 3,
                experience,
                education,
                job_titles * 2
            ])
            
            logger.info(f"Profile content: {profile_content[:100]}...")
            
            # Chuyển đổi nội dung profile sang vector TF-IDF
            profile_vector = self.vectorizer.transform([profile_content])
            
            # Log số features được sử dụng
            non_zero = profile_vector.getnnz()
            logger.info(f"Profile vector has {non_zero} non-zero features out of {profile_vector.shape[1]}")
            
            # Log các từ khóa quan trọng nhất trong profile
            if hasattr(self, 'feature_names') and hasattr(self.vectorizer, 'vocabulary_'):
                feature_array = profile_vector.toarray()[0]
                top_indices = feature_array.argsort()[-10:][::-1]  # Top 10 features
                top_features = [(self.feature_names[i], feature_array[i]) for i in top_indices if feature_array[i] > 0]
                
                logger.info(f"Top features in profile: {top_features}")
            
            # Tính độ tương đồng cosine với tất cả công việc
            cosine_sim = cosine_similarity(profile_vector, self.tfidf_matrix)
            
            # Lấy điểm của tất cả công việc
            sim_scores = list(enumerate(cosine_sim[0]))
            
            # Sắp xếp công việc theo độ tương đồng giảm dần
            sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)
            
            # Log top 5 công việc được gợi ý
            logger.info("Top 5 job recommendations:")
            for i, (idx, score) in enumerate(sim_scores[:5]):
                job = self.jobs_df.iloc[idx]
                job_title = job.get('title', 'Unknown')
                job_id = str(job.get('_id', 'Unknown'))
                
                # Log thêm thông tin về công việc này
                job_content = job.get('content', '')[:50]
                logger.info(f"{i+1}. Job: {job_title} (ID: {job_id}) - Score: {score:.4f}")
                logger.info(f"   Content preview: {job_content}...")
                
                # Kiểm tra spring boot vs php trong nội dung
                contains_spring = "spring boot" in job_content.lower()
                contains_php = "php" in job_content.lower()
                logger.info(f"   Contains Spring Boot: {contains_spring}, Contains PHP: {contains_php}")

            job_indices = [i[0] for i in sim_scores[:limit]]

            recommendations = self.jobs_df.iloc[job_indices].copy()
            recommendations['similarity_score'] = [i[1] for i in sim_scores[:limit]]
            # Quan trọng: Cập nhật recommendation_score từ similarity_score
            recommendations['recommendation_score'] = recommendations['similarity_score']

            return recommendations.to_dict('records')
            
        except Exception as e:
            logger.error(f"Error getting profile recommendations: {e}", exc_info=True)
            return []
        
    def get_job_recommendations(self, job_id, limit=10):
        """Lấy các công việc tương tự với công việc có ID cho trước"""
        try:
            # Đảm bảo mô hình đã được xây dựng
            if not hasattr(self, 'tfidf_matrix'):
                logger.info("TF-IDF matrix not found, fitting model...")
                success = self.fit()
                if not success:
                    logger.error("Failed to build TF-IDF model")
                    return []
            
            # Chuyển đổi job_id thành ObjectId nếu cần
            if isinstance(job_id, str):
                try:
                    job_id_obj = ObjectId(job_id)
                except:
                    logger.error(f"Invalid job_id format: {job_id}")
                    return []
            else:
                job_id_obj = job_id
                
            # Tìm job trong DataFrame
            job_idx = None
            for idx, job in self.jobs_df.iterrows():
                if str(job.get('_id')) == str(job_id):
                    job_idx = idx
                    break
                    
            if job_idx is None:
                logger.warning(f"Job with ID {job_id} not found in DataFrame")
                return []
                
            # Lấy vector của job này
            job_vector = self.tfidf_matrix[job_idx]
            
            # Tính độ tương đồng với tất cả công việc khác
            cosine_sim = cosine_similarity(job_vector, self.tfidf_matrix)
            
            # Lấy điểm của tất cả công việc
            sim_scores = list(enumerate(cosine_sim[0]))
            
            # Sắp xếp công việc theo độ tương đồng giảm dần
            sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)
            
            # Loại bỏ chính job đang xét
            sim_scores = [score for score in sim_scores if score[0] != job_idx]
            
            # Lấy top công việc
            sim_scores = sim_scores[:limit]
            
            # Log top 5 công việc được gợi ý
            logger.info(f"Top 5 similar jobs to job {job_id}:")
            for i, (idx, score) in enumerate(sim_scores[:5]):
                job = self.jobs_df.iloc[idx]
                job_title = job.get('title', 'Unknown')
                job_id = str(job.get('_id', 'Unknown'))
                logger.info(f"{i+1}. Job: {job_title} (ID: {job_id}) - Score: {score:.4f}")
            
            # Lấy chỉ số của công việc
            job_indices = [i[0] for i in sim_scores]
            
            # Lấy thông tin công việc và thêm điểm tương đồng
            recommendations = self.jobs_df.iloc[job_indices].copy()
            recommendations['similarity_score'] = [i[1] for i in sim_scores]
            recommendations['recommendation_score'] = recommendations['similarity_score']
            
            return recommendations.to_dict('records')
            
        except Exception as e:
            logger.error(f"Error getting job recommendations: {e}")
            return []