from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class JobRecommendationFilter(BaseModel):
    keyword: Optional[str] = None
    salary_min: Optional[int] = None
    salary_max: Optional[int] = None
    category_id: Optional[str] = None
    experience_level: Optional[str] = None

class RecommendedJob(BaseModel):
    job_id: str
    title: str
    company_id: str
    category_id: str
    salary_min: int
    salary_max: int
    experience_level: Optional[str] = None
    score: float

class RecommendationResponse(BaseModel):
    success: bool = True
    data: List[RecommendedJob] = []
    error: Optional[str] = None