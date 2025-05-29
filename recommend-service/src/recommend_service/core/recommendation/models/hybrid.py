from datetime import datetime
from bson import ObjectId
import numpy as np
import pandas as pd
from recommend_service.db.mongodb import MongoDB
from recommend_service.core.recommendation.models.content_based import ContentBasedRecommender
from recommend_service.core.recommendation.models.collaborative import CollaborativeRecommender
import logging
from typing import Dict, List, Any, Optional
from recommend_service.utils.user_utils import get_user_id_from_username, get_email_from_username
from recommend_service.config.settings import settings
import json

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

        start_time = datetime.now()
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
            user_profile = self._build_user_profile(user_id)

            content_recommendations = {}
            if user_profile:
                content_results = self.content_based.get_profile_recommendations(
                    profile_data=user_profile,
                    limit=limit  # Lấy nhiều hơn để đảm bảo đủ sau khi lọc
                )

                # Chuyển danh sách kết quả thành dictionary với job_id là key, score là value
                content_recommendations = {
                    str(job['_id']): job.get('similarity_score', 0)
                    for job in content_results
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

            process_time_ms = int((datetime.now() - start_time).total_seconds() * 1000)

            # 4. Lấy thông tin chi tiết về các job được đề xuất
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
                    "authenticated": user_id is not None
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
            Kết quả gợi ý với định dạng phân trang
        """
        start_time = datetime.now()
        limit = size * 3

        try:
            try:
                object_job_id = ObjectId(job_id)
            except Exception as e:
                logger.error(f"Invalid job_id format: {e}")
                return self._create_empty_page_result(page, size, error="Invalid job_id format")

            jobs_collection = self.db.get_collection(settings.MONGODB_JOB_DATABASE, settings.MONGODB_JOBS_COLLECTION)
            job = jobs_collection.find_one({"_id": object_job_id})

            if not job:
                logger.warning(f"Job ID {job_id} không tồn tại")
                return self._create_empty_page_result(page, size, error="Job not found")
            
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
                        collab_results = self.collaborative.recommend(username, limit=limit)
                        collab_recommendations   = {
                            job_id: score
                            for job_id, score in collab_results.items()
                        }

            final_recommendations = self._combine_recommendations(
                content_recommendations, 
                collab_recommendations,
                limit
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
                    "filters_applied": False,
                    "authenticated": username is not None,
                    "source_job_id": job_id
                }
            }
        except Exception as e:
            logger.error(f"Error in hybrid similar jobs recommendation: {e}")
            return self._create_empty_page_result(page, size, error=str(e))

    def _build_user_profile(self, user_id: str) -> Dict:
        """Xây dựng profile người dùng từ các thông tin skill, experience, education"""
        try:
            print(f"Building profile for user_id: {user_id}")
            # Lấy thông tin kỹ năng
            skills_collection = self.db.get_collection(settings.MONGODB_JOB_PROFILE_DATABASE, settings.MONGODB_JOB_PROFILE_SKILL_COLLECTION)

            if skills_collection is None:
                logger.error(f"Skills collection not found in database: {settings.MONGODB_JOB_PROFILE_DATABASE}.{settings.MONGODB_JOB_PROFILE_SKILL_COLLECTION}")
                return {}

            skills = list(skills_collection.find({"userId": user_id, "deletedAt": None}))
            
            # Lấy thông tin kinh nghiệm làm việc
            experiences_collection = self.db.get_collection(settings.MONGODB_JOB_PROFILE_DATABASE, settings.MONGODB_JOB_PROFILE_EXPERIENCE_COLLECTION)
            experiences = list(experiences_collection.find({"userId": user_id, "deletedAt": None}))

            print(f"Found {len(skills)} skills and {len(experiences)} experiences for user_id: {user_id}")
                        
            # Tổng hợp thông tin
            skill_names = [skill['skill_name'] for skill in skills]
            job_titles = [exp['company_name'] for exp in experiences]
            
            return {
                'skills': ' '.join(skill_names),
                'job_titles': ' '.join(job_titles),
                'experience': ' '.join([exp.get('description', '') for exp in experiences if 'description' in exp]),
                'skill_level': {skill['skill_name']: skill['years_experience'] for skill in skills if 'years_experience' in skill}
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

            logger.debug(f"Applying keyword filter: {keyword}")

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
                                      list_user_username: List[str]) -> Dict[str, List[Dict]]:
        """
        Lọc các công việc phổ biến cho một danh sách người dùng dựa trên tên đăng nhập
        Args:
            list_user_username: Danh sách tên đăng nhập của người dùng
        Returns:
            Dictionary với key là tên đăng nhập và value là danh sách các công việc được gợi ý
        """
        recommend_jobs_to_send = {}
        for username in list_user_username:
            all_jobs = self.recommend_jobs(
                username=username,
                filters=None,
                page=0,
                size=40
            ).get("content", [])

            filtered_jobs = [job for job in all_jobs
                             if job.get('recommendation_score', 0) >= 0.25]
            
            filtered_job_ids = {str(job['_id']) for job in filtered_jobs}

            recommend_jobs_to_send[get_email_from_username(username)] = filtered_job_ids
        return recommend_jobs_to_send