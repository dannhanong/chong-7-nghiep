import os
import tempfile
from typing import Optional
from fastapi import APIRouter, UploadFile, File, HTTPException, Request, Query, Depends
from recommend_service.core.auth.jwt_service import jwt_service
from recommend_service.core.recommendation.models.hybrid import HybridRecommender
from recommend_service.utils.user_utils import get_all_usernames
import logging
import uuid

logger = logging.getLogger(__name__)
router = APIRouter()

def get_current_username(request: Request) -> Optional[str]:
    return jwt_service.get_username_from_request(request)

@router.get("/jobs")
async def get_job_recommendations(
    request: Request,
    page: int = Query(0, description="Trang (bắt đầu từ 0)"),
    size: int = Query(10, description="Số phần tử mỗi trang"),
    keyword: Optional[str] = None,
    category_id: Optional[str] = None,
    salary_min: Optional[int] = None,
    salary_max: Optional[int] = None,
    experience_level: Optional[str] = None,
):
    """
    Gợi ý công việc dựa trên thông tin người dùng và bộ lọc
    
    - Người dùng đã đăng nhập: Gợi ý cá nhân hóa
    - Người dùng chưa đăng nhập: Hiển thị công việc phổ biến
    """
    try:
        username = get_current_username(request)

        print (f"Username: {username}")
        # Tạo bộ lọc từ query params
        filters = {}
        if keyword:
            filters["keyword"] = keyword
        if category_id:
            filters["category_id"] = category_id
        if salary_min:
            filters["salary_min"] = salary_min
        if salary_max:
            filters["salary_max"] = salary_max
        if experience_level:
            filters["experience_level"] = experience_level
            
        # Chỉ truyền filters khi có ít nhất 1 bộ lọc
        if not filters:
            filters = None
        
        # Gọi HybridRecommender
        recommender = HybridRecommender()
        recommendations = recommender.recommend_jobs(
            username=username,
            filters=filters,
            page=page,
            size=size
        )
        
        return recommendations
    except Exception as e:
        logger.error(f"Error in get_profile_recommendations: {str(e)}")
        raise HTTPException(status_code=500, detail="Internal Server Error")

@router.get("/similar-jobs/{job_id}")
async def get_similar_jobs(
    request: Request,
    job_id: str,
    page: int = Query(0, description="Trang (bắt đầu từ 0)"),
    size: int = Query(10, description="Số phần tử mỗi trang")
):
    """
    Gợi ý công việc tương tự dựa trên job_id
    - Người dùng đã đăng nhập: Gợi ý cá nhân hóa
    - Người dùng chưa đăng nhập: Gợi ý công việc tương tự
    """
    try:
        username = get_current_username(request)

        recommender = HybridRecommender()
        recommendations = recommender.recommend_similar_jobs(
            job_id=job_id,
            username=username,
            page=page,
            size=size
        )
        
        return recommendations
    except Exception as e:
        logger.error(f"Error in get_similar_jobs: {str(e)}")
        raise HTTPException(status_code=500, detail="Internal Server Error")
    
@router.get("/gmail-jobs")
async def get_gmail_job_recommendations():
    """
    Gợi ý công việc dựa trên email Gmail đã đăng nhập
    """
    try:
        recommender = HybridRecommender()
        # Lấy danh sách tất cả usernames từ cơ sở dữ liệu
        list_user_username = get_all_usernames()
        print(f"List of usernames: {list_user_username}")

        recommendations = recommender.recommend_jobs_for_user_gmail(
            list_user_username=list_user_username
        )
        
        return recommendations
    except Exception as e:
        logger.error(f"Error in get_gmail_job_recommendations: {str(e)}")
        raise HTTPException(status_code=500, detail="Internal Server Error")