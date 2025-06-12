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
        self.model = SentenceTransformer('keepitreal/vietnamese-sbert')
        self.db = MongoDB()
        self.use_cache = use_cache
        self.cache_ttl = cache_ttl
        self.cache_dir = cache_dir

        if self.use_cache and not os.path.exists(self.cache_dir):
            os.makedirs(self.cache_dir)

    def _get_cache_path(self, model_name):
        return os.path.join(self.cache_dir, f"{model_name}_cache.pkl")
    
    def _is_cache_valid(self, cache_path):
        if not os.path.exists(cache_path):
            return False
        
        try:
            created_time = os.path.getmtime(cache_path)
            current_time = datetime.now().timestamp()
            return (current_time - created_time) < self.cache_ttl
        except Exception as e:
            logger.error(f"Error checking cache validity: {e}")
            return False
        
    def _get_data_hash(self, job_data):
        """Tạo hash của dữ liệu để detect changes"""
        import hashlib
        
        # Create a simple hash based on number of jobs and some key fields
        hash_data = f"{len(job_data)}"
        if job_data:
            # Include first and last job's key info for change detection
            first_job = job_data[0]
            last_job = job_data[-1]
            hash_data += f"{first_job.get('_id', '')}{first_job.get('title', '')}"
            hash_data += f"{last_job.get('_id', '')}{last_job.get('title', '')}"
        
        return hashlib.md5(hash_data.encode()).hexdigest()

    def fit(self, force_rebuild=False):
        """Xây dựng embedding matrix cho tất cả jobs"""
        try:
            cache_path = self._get_cache_path("semantic_jobs")

            jobs_collection = self.db.get_collection(settings.MONGODB_JOB_DATABASE, settings.MONGODB_JOBS_COLLECTION)
            job_data = list(jobs_collection.find({"active": True, "status": True, "deletedAt": None}))

            if not job_data:
                logger.warning("No active jobs found in the database.")
                return False
            current_data_hash = self._get_data_hash(job_data)

            if (not force_rebuild and
                self.use_cache and
                self._is_cache_valid(cache_path)):

                try:
                    with open(cache_path, 'rb') as f:
                        cache_data = pickle.load(f)

                    cached_hash = cache_data.get('data_hash', '')
                    if cached_hash == current_data_hash:
                        self.job_embeddings = cache_data['job_embeddings']
                        self.jobs_df = cache_data['jobs_df']
                        self.feature_names = cache_data.get('feature_names', [])
                        
                        logger.info(f"Loaded semantic embeddings from cache. Shape: {self.job_embeddings.shape}")
                        return True
                    else:
                        logger.info("Data has changed, rebuilding embeddings...")

                except Exception as e:
                    logger.error(f"Error loading cache: {e}")

            logger.info("Building semantic embeddings from scratch...")

            self.jobs_df = pd.DataFrame(job_data)

            job_contents = []
            for _, job in self.jobs_df.iterrows():
                content = ' '.join([
                    f"Vị trí: {job.get('title', '')}",
                    f"Mô tả: {job.get('description', '')}",
                    f"Yêu cầu: {job.get('requirements', '')}",
                    f"Kỹ năng: {job.get('skills', '')}",
                    f"Kinh nghiệm: {job.get('experienceLevel', '')}"
                ])
                job_contents.append(content)

            logger.info("Creating semantic embeddings for jobs...")
            self.job_embeddings = self.model.encode(
                job_contents, 
                show_progress_bar=True,
                batch_size=32  # Optimize batch size for performance
            )

            self.feature_names = [f"job_{i}" for i in range(len(job_contents))]

            logger.info(f"Created embeddings with shape: {self.job_embeddings.shape}")

            if self.use_cache:
                try:
                    cache_data = {
                        'job_embeddings': self.job_embeddings,
                        'jobs_df': self.jobs_df,
                        'feature_names': self.feature_names,
                        'data_hash': current_data_hash,
                        'created_at': datetime.now().isoformat(),
                        'model_name': 'keepitreal/vietnamese-sbert'
                    }
                    
                    with open(cache_path, 'wb') as f:
                        pickle.dump(cache_data, f)
                    
                    logger.info(f"Semantic embeddings saved to cache: {cache_path}")
                    
                except Exception as e:
                    logger.warning(f"Error saving cache: {e}")
            
            return True
        
        except Exception as e:
            logger.error(f"Error creating semantic embeddings: {e}")
            return False
        
    def get_profile_recommendations(self, profile_data, limit=50):
        """Gợi ý công việc dựa trên semantic similarity"""
        try:
            if not hasattr(self, 'job_embeddings'):
                self.fit()
            
            # Tạo profile content với ngữ cảnh
            profile_content = self._build_semantic_profile(profile_data)
            
            # Tạo embedding cho profile
            profile_embedding = self.model.encode([profile_content])
            
            # Tính semantic similarity
            similarities = cosine_similarity(profile_embedding, self.job_embeddings)[0]
            
            # Sắp xếp và lấy top jobs
            sim_scores = list(enumerate(similarities))
            sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)
            
            # Log để debug
            logger.info("Top 5 semantic recommendations:")
            for i, (idx, score) in enumerate(sim_scores[:5]):
                job = self.jobs_df.iloc[idx]
                logger.info(f"{i+1}. {job.get('title')} - Score: {score:.4f}")
            
            job_indices = [i[0] for i in sim_scores[:limit]]
            recommendations = self.jobs_df.iloc[job_indices].copy()
            recommendations['similarity_score'] = [i[1] for i in sim_scores[:limit]]
            recommendations['recommendation_score'] = recommendations['similarity_score']
            
            return recommendations.to_dict('records')
            
        except Exception as e:
            logger.error(f"Error in semantic recommendations: {e}")
            return []
        
    def _build_semantic_profile(self, profile_data):
        """Xây dựng profile content với ngữ cảnh ngữ nghĩa"""
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