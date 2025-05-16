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
        
    def fit(self, force_rebuild=False):
        """Xây dựng mô hình TF-IDF từ dữ liệu công việc"""
        try:
            cache_path = self._get_cache_path("jobs")

            if self.use_cache and self._is_cache_valid(cache_path) and not force_rebuild:
                with open(cache_path, 'rb') as f:
                    cache_data = pickle.load(f)
                    self.jobs_df = cache_data['jobs_df']
                    self.tfidf_matrix = cache_data['tfidf_matrix']
                    self.feature_names = cache_data['feature_names']
                logger.info("Loaded TF-IDF model from cache.")
                return True
            
            jobs_collection = self.db.get_collection("jobs_job", "jobs")
            jobs_data = list(jobs_collection.find({}))

            if not jobs_data:
                logger.warning("No jobs data found in the database.")
                return False
            
            self.jobs_df = pd.DataFrame(jobs_data)

            self.jobs_df['content'] = self.jobs_df.apply(
                lambda row: ' '.join([
                    str(row.get('title', '')),
                    str(row.get('description', '')),
                    str(row.get('benefits', '')),
                    str(row.get('requirements', '')), 
                    str(row.get('skills', ''))
                ]),
                axis=1
            )

            self.tfidf_matrix = self.vectorizer.fit_transform(self.jobs_df['content'])
            self.feature_names = self.vectorizer.get_feature_names_out()

            if self.use_cache:
                with open(cache_path, 'wb') as f:
                    pickle.dump({
                        'jobs_df': self.jobs_df,
                        'tfidf_matrix': self.tfidf_matrix,
                        'feature_names': self.feature_names
                    }, f)

                logger.info(f"TF-IDF model saved to cache at {cache_path}.")
            
            return True
        
        except Exception as e:
            logger.error(f"Error building content-based model: {e}")
            return False
        
    def get_job_recommendations(self, job_id, limit=10):
        """Lấy gợi ý công việc dựa trên ID công việc"""
        try:
            if not hasattr(self, 'tfidf_matrix'):
                self.fit()

            job_idx = self.jobs_df[self.jobs_df['_id'] == job_id].index

            if len(job_idx) == 0:
                logger.warning(f"Job ID {job_id} not found in the dataset.")
                return []
            
            job_idx = job_idx[0]

            cosine_sim = cosine_similarity(
                self.tfidf_matrix[job_idx:job_idx+1], 
                self.tfidf_matrix
            )

            # Lấy điểm của tất cả công việc
            sim_scores = list(enumerate(cosine_sim[0]))

            # Sắp xếp công việc theo điểm tương đồng giảm dần
            sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)

            # Bỏ qua công việc hiện tại
            sim_scores = [s for s in sim_scores if s[0] != job_idx]

            # Lấy top công việc
            sim_scores = sim_scores[:limit]
            
            # Lấy chỉ số của công việc
            job_indices = [i[0] for i in sim_scores]
            
            # Lấy thông tin công việc và thêm điểm tương đồng
            recommendations = self.jobs_df.iloc[job_indices].copy()
            recommendations['similarity_score'] = [i[1] for i in sim_scores]
            
            return recommendations.to_dict('records')
        
        except Exception as e:
            logger.error(f"Error getting job recommendations: {e}")
            return []
        
    def get_profile_recommendations(self, profile_data, limit=10):
        """Lấy gợi ý công việc dựa trên thông tin hồ sơ người dùng"""
        try:
            # Đảm bảo mô hình đã được xây dựng
            if not hasattr(self, 'tfidf_matrix'):
                self.fit()
            
            # Tạo nội dung từ profile
            profile_content = ' '.join([
                str(profile_data.get('skills', '')),
                str(profile_data.get('experience', '')),
                str(profile_data.get('education', '')),
                str(profile_data.get('job_titles', ''))
            ])
            
            # Chuyển đổi nội dung profile sang vector TF-IDF
            profile_vector = self.vectorizer.transform([profile_content])
            
            # Tính độ tương đồng cosine với tất cả công việc
            cosine_sim = cosine_similarity(profile_vector, self.tfidf_matrix)
            
            # Lấy điểm của tất cả công việc
            sim_scores = list(enumerate(cosine_sim[0]))
            
            # Sắp xếp công việc theo độ tương đồng giảm dần
            sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)
            
            # Lấy top công việc
            sim_scores = sim_scores[:limit]
            
            # Lấy chỉ số của công việc
            job_indices = [i[0] for i in sim_scores]
            
            # Lấy thông tin công việc và thêm điểm tương đồng
            recommendations = self.jobs_df.iloc[job_indices].copy()
            recommendations['similarity_score'] = [i[1] for i in sim_scores]
            
            return recommendations.to_dict('records')
            
        except Exception as e:
            logger.error(f"Error getting profile recommendations: {e}")
            return []