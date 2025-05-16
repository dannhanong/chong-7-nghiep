from typing import List, Dict, Any, Optional
from recommend_service.db.mongodb import MongoDB
from recommend_service.config.settings import settings
from recommend_service.core.recommendation.models.hybrid import HybridRecommender
import logging

logger = logging.getLogger(__name__)

class RecommendationService:
    def __init__(self):
        self.mongodb = MongoDB()
        self.recommender = HybridRecommender()
        self.limit = settings.RECOMMENDATION_LIMIT
        
    def get_job_recommendations(
        self, 
        user_id: str, 
        filters: Optional[Dict] = None, 
        limit: Optional[int] = None
    ) -> List[Dict]:
        """
        Lấy các gợi ý công việc cho người dùng
        
        Args:
            user_id: ID của người dùng
            filters: Các bộ lọc (từ khóa, lương, danh mục...)
            limit: Số lượng kết quả tối đa
            
        Returns:
            Danh sách các job được đề xuất
        """
        if not limit:
            limit = self.limit
            
        try:
            # Kiểm tra xem user_id có tồn tại không
            users_collection = self.mongodb.get_collection(
                settings.MONGODB_USER_DATABASE, 
                settings.MONGODB_USERS_COLLECTION
            )
            user = users_collection.find_one({"_id": user_id})
            
            if not user:
                logger.warning(f"User {user_id} not found")
                return []
                
            # Gọi hybrid recommender
            recommendations = self.recommender.recommend_jobs(
                user_id=user_id,
                filters=filters,
                limit=limit
            )
            
            # Chỉ trả về các thông tin cần thiết
            result = []
            for job in recommendations:
                result.append({
                    "job_id": job.get("_id"),
                    "title": job.get("title"),
                    "company_id": job.get("companyId"),
                    "category_id": job.get("categoryId"),
                    "salary_min": job.get("salaryMin"),
                    "salary_max": job.get("salaryMax"),
                    "experience_level": job.get("experienceLevel"),
                    "score": job.get("recommendation_score")
                })
                
            return result
            
        except Exception as e:
            logger.error(f"Error getting recommendations: {e}")
            return []
            
    def get_similar_jobs(self, job_id: str, limit: Optional[int] = None) -> List[Dict]:
        """
        Lấy các công việc tương tự với một công việc cụ thể
        
        Args:
            job_id: ID của công việc cần tìm tương tự
            limit: Số lượng kết quả tối đa
            
        Returns:
            Danh sách các công việc tương tự
        """
        if not limit:
            limit = self.limit
            
        try:
            # Sử dụng content-based recommender để tìm job tương tự
            content_based = self.recommender.content_based
            similar_jobs = content_based.get_job_recommendations(job_id, limit)
            
            result = []
            for job in similar_jobs:
                result.append({
                    "job_id": job.get("_id"),
                    "title": job.get("title"),
                    "company_id": job.get("companyId"),
                    "category_id": job.get("categoryId"),
                    "salary_min": job.get("salaryMin"),
                    "salary_max": job.get("salaryMax"),
                    "experience_level": job.get("experienceLevel"),
                    "similarity_score": job.get("similarity_score")
                })
                
            return result
            
        except Exception as e:
            logger.error(f"Error getting similar jobs: {e}")
            return []

# Singleton instance
recommendation_service = RecommendationService()