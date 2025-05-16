import numpy as np
import pandas as pd
from recommend_service.db.mongodb import MongoDB
from recommend_service.core.recommendation.models.content_based import ContentBasedRecommender
from recommend_service.core.recommendation.models.collaborative import CollaborativeRecommender
import logging
from typing import Dict, List, Any, Optional

logger = logging.getLogger(__name__)

class HybridRecommender:
    def __init__(self, content_weight=0.7, collab_weight=0.3):
        """
        Khởi tạo hệ thống gợi ý kết hợp (hybrid)
        
        Args:
            content_weight: Trọng số cho content-based recommendations
            collab_weight: Trọng số cho collaborative recommendations
        """
        self.db = MongoDB()
        self.content_based = ContentBasedRecommender(use_cache=True)
        self.collaborative = CollaborativeRecommender()
        self.content_weight = content_weight
        self.collab_weight = collab_weight

    def recommend_jobs(self, 
                      user_id: str, 
                      filters: Optional[Dict] = None, 
                      limit: int = 10) -> List[Dict[str, Any]]:
        """
        Trả về danh sách các job được đề xuất cho người dùng
        
        Args:
            user_id: ID của người dùng cần gợi ý
            filters: Dictionary chứa các tiêu chí lọc (keyword, salary, category...)
            limit: Số lượng gợi ý tối đa trả về
            
        Returns:
            List các job được đề xuất kèm điểm tương đồng
        """
        try:
            # Trường hợp người dùng chưa đăng nhập
            if not user_id:
                logger.info("Anonymous user. Using popular jobs and filters.")
                popular_jobs = self.collaborative.get_popular_jobs(limit*2)
                
                # Lấy thông tin chi tiết về job
                enriched_jobs = self._enrich_recommendations(popular_jobs)
                
                # Áp dụng bộ lọc
                filtered_jobs = [
                    job for job in enriched_jobs 
                    if self._apply_filters(job, filters)
                ]
                
                return filtered_jobs[:limit]
            
            # Lấy thông tin người dùng
            user_profile = self._build_user_profile(user_id)
            
            if not user_profile:
                logger.warning(f"Không tìm thấy hồ sơ người dùng: {user_id}")
                return []
                
            # 1. Content-based recommendations dựa vào profile người dùng
            content_recommendations = {}
            if user_profile:
                content_results = self.content_based.get_profile_recommendations(
                    profile_data=user_profile,
                    limit=limit*2  # Lấy nhiều hơn để đảm bảo đủ sau khi lọc
                )
                
                # Chuyển danh sách kết quả thành dictionary với job_id là key, score là value
                content_recommendations = {
                    job['_id']: job['similarity_score'] 
                    for job in content_results
                    if self._apply_filters(job, filters)  # Áp dụng bộ lọc
                }
            
            # 2. Collaborative recommendations dựa vào hành vi người dùng
            collab_recommendations = {}
            interaction_count = self._get_user_interaction_count(user_id)
            
            if interaction_count >= 3:  # Chỉ dùng collaborative khi có đủ dữ liệu
                collab_results = self.collaborative.recommend(user_id, limit=limit*2)
                collab_recommendations = {
                    job_id: score 
                    for job_id, score in collab_results.items()
                }
            
            # 3. Kết hợp kết quả từ hai phương pháp
            final_recommendations = self._combine_recommendations(
                content_recommendations, 
                collab_recommendations,
                limit
            )
            
            # 4. Lấy thông tin chi tiết về các job được đề xuất
            return self._enrich_recommendations(final_recommendations)
            
        except Exception as e:
            logger.error(f"Error in hybrid recommendation: {e}")
            return []
        
    def _build_user_profile(self, user_id: str) -> Dict:
        """Xây dựng profile người dùng từ các thông tin skill, experience, education"""
        try:
            # Lấy thông tin kỹ năng
            skills_collection = self.db.get_collection("jobs_identity", "skills")
            skills = list(skills_collection.find({"userId": user_id, "deletedAt": None}))
            
            # Lấy thông tin kinh nghiệm làm việc
            experiences_collection = self.db.get_collection("jobs_identity", "experiences")
            experiences = list(experiences_collection.find({"userId": user_id, "deletedAt": None}))
            
            # Lấy thông tin học vấn
            educations_collection = self.db.get_collection("jobs_identity", "educations")
            educations = list(educations_collection.find({"userId": user_id, "deletedAt": None}))
            
            # Tổng hợp thông tin
            skill_names = [skill['name'] for skill in skills]
            job_titles = [exp['companyName'] for exp in experiences]
            education = [f"{edu['degree']} {edu['fieldOfStudy']} {edu['school']}" for edu in educations]
            
            return {
                'skills': ' '.join(skill_names),
                'job_titles': ' '.join(job_titles),
                'education': ' '.join(education),
                'experience': ' '.join([exp.get('description', '') for exp in experiences if 'description' in exp]),
                'skill_level': {skill['name']: skill['level'] for skill in skills if 'level' in skill}
            }
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
        if 'keyword' in filters and filters['keyword']:
            keyword = filters['keyword'].lower()
            job_text = f"{job.get('title', '')} {job.get('description', '')} {job.get('skills', '')}".lower()
            if keyword not in job_text:
                return False
                
        # Lọc theo mức lương
        if 'salary_min' in filters and job.get('salaryMin', 0) < filters['salary_min']:
            return False
            
        if 'salary_max' in filters and filters['salary_max'] > 0 and job.get('salaryMax', float('inf')) > filters['salary_max']:
            return False
            
        # Lọc theo danh mục
        if 'category_id' in filters and filters['category_id'] != job.get('categoryId'):
            return False
            
        # Lọc theo experience level
        if 'experience_level' in filters and filters['experience_level'] != job.get('experienceLevel'):
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
                               limit: int) -> Dict[str, float]:
        """
        Kết hợp kết quả từ 2 phương pháp recommendation với trọng số
        
        Args:
            content_recs: Dictionary {job_id: score} từ content-based
            collab_recs: Dictionary {job_id: score} từ collaborative
            limit: Số lượng kết quả tối đa
            
        Returns:
            Dictionary {job_id: final_score} đã được kết hợp và sắp xếp
        """
        combined = {}
        
        # Trường hợp không có đủ dữ liệu collaborative
        if not collab_recs:
            # Nếu không có dữ liệu collaborative, dùng 100% content-based
            sorted_content = sorted(content_recs.items(), key=lambda x: x[1], reverse=True)
            return {k: v for k, v in sorted_content[:limit]}
            
        # Trường hợp không có đủ dữ liệu content-based  
        if not content_recs:
            # Nếu không có dữ liệu content-based, dùng 100% collaborative
            sorted_collab = sorted(collab_recs.items(), key=lambda x: x[1], reverse=True)
            return {k: v for k, v in sorted_collab[:limit]}
            
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
        normalized_collab = normalize_scores(collab_recs)
        
        # Kết hợp với trọng số
        all_job_ids = set(normalized_content.keys()) | set(normalized_collab.keys())
        
        for job_id in all_job_ids:
            content_score = normalized_content.get(job_id, 0) * self.content_weight
            collab_score = normalized_collab.get(job_id, 0) * self.collab_weight
            combined[job_id] = content_score + collab_score
            
        # Sắp xếp theo điểm số và giới hạn số lượng
        sorted_recommendations = sorted(combined.items(), key=lambda x: x[1], reverse=True)
        return {k: v for k, v in sorted_recommendations[:limit]}
    
    def _enrich_recommendations(self, recommendations: Dict[str, float]) -> List[Dict]:
        """
        Thêm thông tin chi tiết cho các job được đề xuất
        
        Args:
            recommendations: Dictionary {job_id: score}
            
        Returns:
            List các job với thông tin đầy đủ và điểm đề xuất
        """
        if not recommendations:
            return []
            
        try:
            # Lấy thông tin chi tiết từ database
            jobs_collection = self.db.get_collection("jobs_job", "jobs")
            job_ids = list(recommendations.keys())
            
            jobs = list(jobs_collection.find({
                "_id": {"$in": job_ids},
                "active": True,
                "status": True,
                "deletedAt": None
            }))
            
            # Thêm điểm đề xuất vào kết quả
            for job in jobs:
                job['recommendation_score'] = recommendations.get(job['_id'], 0)
                
            # Sắp xếp theo điểm đề xuất
            return sorted(jobs, key=lambda x: x['recommendation_score'], reverse=True)
            
        except Exception as e:
            logger.error(f"Error enriching recommendations: {e}")
            return []