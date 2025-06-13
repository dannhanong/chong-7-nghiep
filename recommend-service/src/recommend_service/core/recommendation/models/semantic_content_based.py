import json
from sentence_transformers import SentenceTransformer
import numpy as np
import pandas as pd
from sklearn.metrics.pairwise import cosine_similarity
from recommend_service.db.mongodb import MongoDB
import logging
import pickle
import os
from datetime import datetime
from recommend_service.config.settings import settings

logger = logging.getLogger(__name__)

class SemanticContentBasedRecommender:
    def __init__(self, use_cache=True, cache_ttl=3600, cache_dir="cache"):
        self._model = None
        self.db = MongoDB()
        self.use_cache = use_cache
        self.cache_ttl = cache_ttl
        self.cache_dir = cache_dir

        if self.use_cache and not os.path.exists(self.cache_dir):
            os.makedirs(self.cache_dir)

    @property
    def model(self):
        """Lazy load model chỉ khi cần thiết"""
        if self._model is None:
            self._model = SentenceTransformer('keepitreal/vietnamese-sbert')
        return self._model

    def fit(self, limit=10000, filter_criteria=None):
        """Xây dựng embedding matrix cho tất cả jobs đã lưu trong mongoDB"""
        try:
            if filter_criteria is None:
                filter_criteria = {"active": True, "status": True, "deletedAt": None}

            jobs_collection = self.db.get_collection(
                settings.MONGODB_JOB_DATABASE, 
                settings.MONGODB_JOBS_COLLECTION
            )
            job_data = list(jobs_collection.find(filter_criteria).limit(limit))

            if not job_data:
                logger.warning("No active jobs found in the database.")
                return False

            job_embeddings_collection = self.db.get_collection(
                settings.MONGODB_JOB_DATABASE, 
                settings.MONGODB_JOB_EMBEDDINGS_COLLECTION
            )

            job_embedding_docs = list(job_embeddings_collection.find({}))
            job_embeddings_map = {doc["job_id"]: doc["embedding"] for doc in job_embedding_docs}

            # Lọc jobs có embeddings và tạo DataFrame
            jobs_with_embeddings = []
            embeddings_matrix = []
            
            for job in job_data:
                job_id = str(job["_id"])
                if job_id in job_embeddings_map:
                    jobs_with_embeddings.append(job)
                    embeddings_matrix.append(job_embeddings_map[job_id])
                
            if not jobs_with_embeddings:
                logger.warning("No job embeddings found in MongoDB.")
                return False
                
            # Tạo DataFrame và embedding matrix
            self.jobs_df = pd.DataFrame(jobs_with_embeddings)
            self.job_embeddings = np.array(embeddings_matrix)
            
            logger.info(f"Loaded {len(jobs_with_embeddings)} job embeddings from MongoDB. Shape: {self.job_embeddings.shape}")
            return True
        except Exception as e:
            logger.error(f"Error creating semantic embeddings: {e}")
            return False
        
    def get_profile_recommendations(self, user_id=None, profile_data=None, limit=50):
        """Gợi ý công việc dựa trên semantic similarity"""
        try:    
            if not hasattr(self, 'job_embeddings') or self.job_embeddings is None:
                success = self.fit()
                if not success:
                    logger.error("Failed to load job embeddings. Cannot provide recommendations.")
                    return []

            if self.job_embeddings.shape[0] == 0:
                logger.warning("No job embeddings available for recommendations.")
                return []
            
            profile_embedding = None

            if user_id:
                try:
                    profile_embeddings_collection = self.db.get_collection(
                        settings.MONGODB_JOB_DATABASE, 
                        settings.MONGODB_PROFILE_EMBEDDINGS_COLLECTION
                    )
                    profile_doc = profile_embeddings_collection.find_one({"user_id": user_id})
                    
                    if profile_doc and "embedding" in profile_doc:
                        profile_embedding = np.array(profile_doc["embedding"])
                        logger.info(f"Using stored profile embedding for user: {user_id}")
                except Exception as e:
                    logger.error(f"Error retrieving profile embedding: {e}")
                    
            # Nếu không có sẵn embedding, tính toán mới nếu có profile_data
            if profile_embedding is None:
                if not profile_data:
                    logger.warning("No profile data provided and no stored embedding found.")
                    return []
                    
                # Tạo profile content và encode
                profile_content = self._build_semantic_profile(profile_data)
                profile_embedding = self.model.encode([profile_content])[0]
                
                # Lưu embedding vào MongoDB nếu có user_id
                if user_id:
                    self._store_profile_embedding(user_id, profile_data, profile_embedding)
                    
            # Đảm bảo embedding là 2D array
            if profile_embedding.ndim == 1:
                profile_embedding = profile_embedding.reshape(1, -1)
                
            # Tính similarity
            similarities = cosine_similarity(profile_embedding, self.job_embeddings)[0]
            
            # Sắp xếp và lấy top jobs
            sim_scores = list(enumerate(similarities))
            sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)
            
            # Log để debug
            logger.info("Top 5 semantic recommendations:")
            for i, (idx, score) in enumerate(sim_scores[:5]):
                job = self.jobs_df.iloc[idx]
                logger.info(f"{i+1}. {job.get('title')} - Score: {score:.4f}")
            
            # Lấy kết quả
            job_indices = [i[0] for i in sim_scores[:limit] if i[0] < len(self.jobs_df)]
            recommendations = self.jobs_df.iloc[job_indices].copy()
            recommendations['similarity_score'] = [i[1] for i in sim_scores[:limit] if i[0] < len(self.jobs_df)]
            recommendations['recommendation_score'] = recommendations['similarity_score']
            
            return recommendations.to_dict('records')
        except Exception as e:
            logger.error(f"Error in semantic recommendations: {e}")
            return []
        
    def get_job_recommendations(self, job_id, limit=50):
        """
        Tìm công việc tương tự dựa trên job_id.
        
        Args:
            job_id: ID của công việc cần tìm tương tự
            limit: Số lượng kết quả tối đa
            
        Returns:
            list: Danh sách công việc tương tự
        """
        try:
            # Đảm bảo đã tải job embeddings
            if not hasattr(self, 'job_embeddings') or self.job_embeddings is None:
                success = self.fit()
                if not success:
                    return []
                
            # Tìm job embedding trong MongoDB
            job_embeddings_collection = self.db.get_collection(
                settings.MONGODB_JOB_DATABASE,
                settings.MONGODB_JOB_EMBEDDINGS_COLLECTION
            )
            
            job_doc = job_embeddings_collection.find_one({"job_id": job_id})
            if not job_doc or "embedding" not in job_doc:
                logger.warning(f"No embedding found for job_id: {job_id}")
                return []
                
            # Lấy embedding
            job_embedding = np.array(job_doc["embedding"]).reshape(1, -1)
            
            # Tính similarity với tất cả job embeddings
            similarities = cosine_similarity(job_embedding, self.job_embeddings)[0]
            
            # Sắp xếp và lấy top jobs
            sim_scores = list(enumerate(similarities))
            sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)
            
            # Bỏ qua job gốc (thường ở vị trí đầu tiên)
            filtered_scores = []
            for idx, (job_idx, score) in enumerate(sim_scores):
                if job_idx < len(self.jobs_df) and str(self.jobs_df.iloc[job_idx].get('_id')) != job_id:
                    filtered_scores.append((job_idx, score))
                    if len(filtered_scores) >= limit:
                        break
                        
            # Lấy kết quả
            job_indices = [i[0] for i in filtered_scores]
            if not job_indices:
                return []
                
            recommendations = self.jobs_df.iloc[job_indices].copy()
            recommendations['similarity_score'] = [i[1] for i in filtered_scores]
            recommendations['recommendation_score'] = recommendations['similarity_score']
            
            return recommendations.to_dict('records')
            
        except Exception as e:
            logger.error(f"Error in job recommendations: {e}", exc_info=True)
            return []
    
    def _store_profile_embedding(self, user_id, profile_data, profile_embedding):
        """Lưu profile embedding vào MongoDB"""
        try:
            # Tạo hash để phát hiện thay đổi
            profile_hash = self._get_profile_hash(profile_data)
            
            # Lưu vào MongoDB
            profile_embeddings_collection = self.db.get_collection(
                settings.MONGODB_JOB_DATABASE, 
                settings.MONGODB_PROFILE_EMBEDDINGS_COLLECTION
            )
            
            result = profile_embeddings_collection.update_one(
                {"user_id": user_id},
                {"$set": {
                    "embedding": profile_embedding.tolist() if isinstance(profile_embedding, np.ndarray) else profile_embedding,
                    "profile_hash": profile_hash,
                    "updated_at": datetime.now()
                }},
                upsert=True
            )
            
            logger.info(f"Đã lưu profile embedding cho user: {user_id}")
            return True
            
        except Exception as e:
            logger.error(f"Lỗi lưu profile embedding: {e}")
            return False

    def _get_profile_hash(self, profile_data):
        """Tạo hash để phát hiện thay đổi profile"""
        import hashlib
        
        hash_data = ""
        for key in sorted(profile_data.keys()):
            hash_data += f"{key}:{profile_data.get(key, '')}"
            
        return hashlib.md5(hash_data.encode()).hexdigest()
    
    def _build_semantic_profile(self, profile_data):
        """
        Xây dựng profile content với ngữ cảnh ngữ nghĩa.
        
        Args:
            profile_data: Dữ liệu profile
            
        Returns:
            str: Profile content dạng text
        """
        skills = str(profile_data.get('skills', ''))
        experience = str(profile_data.get('experience', ''))
        education = str(profile_data.get('education', ''))
        job_titles = str(profile_data.get('job_titles', ''))
        
        return f"""
        Ứng viên có kỹ năng: {skills}
        Kinh nghiệm làm việc: {experience}
        Học vấn: {education}
        Các vị trí đã làm: {job_titles}
        """