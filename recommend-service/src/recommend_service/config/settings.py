import os
from pydantic_settings import BaseSettings
from dotenv import load_dotenv

load_dotenv()

class Settings(BaseSettings):
    APP_NAME: str = "recommend-service"
    DEBUG: bool = os.getenv("DEBUG", "False").lower() == "true"
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "5000"))

    # Cài đặt MongoDB
    MONGODB_URI: str = os.getenv("MONGODB_URI", "mongodb://root:123456@103.216.117.244:27017")
    MONGODB_JOB_DATABASE: str = os.getenv("MONGODB_JOB_DATABASE", "jobs_job")
    MONGODB_JOBS_COLLECTION: str = os.getenv("MONGODB_JOBS_COLLECTION", "jobs")
    MONGODB_USER_DATABASE: str = os.getenv("MONGODB_USER_DATABASE", "links_auth")
    MONGODB_USERS_COLLECTION: str = os.getenv("MONGODB_USERS_COLLECTION", "users")
    MONGODB_JOB_PROFILE_DATABASE: str = os.getenv("MONGODB_JOB_PROFILE_DATABASE")
    MONGODB_JOB_PROFILE_SKILL_COLLECTION: str =  os.getenv("MONGODB_JOB_PROFILE_SKILL_COLLECTION")
    MONGODB_JOB_PROFILE_EXPERIENCE_COLLECTION: str = os.getenv("MONGODB_JOB_PROFILE_EXPERIENCE_COLLECTION")

    # Cài đặt cho hệ thống gợi ý
    RECOMMENDATION_LIMIT: int = int(os.getenv("RECOMMENDATION_LIMIT", "10"))
    RECOMMENDATION_THRESHOLD: float = float(os.getenv("RECOMMENDATION_THRESHOLD", "0.5"))
    USE_CACHE: bool = os.getenv("USE_CACHE", "True").lower() == "true"
    CACHE_TTL: int = int(os.getenv("CACHE_TTL", "3600"))

    # JWT Settings
    JWT_SECRET: str = os.getenv("JWT_SECRET", "conghoaxahoichunghiavietnam/doclaptudohanhphuc/1975/1945")
    JWT_ALGORITHM: str = os.getenv("JWT_ALGORITHM", "HS256")
    JWT_EXPIRATION_MINUTES: int = int(os.getenv("JWT_EXPIRATION_MINUTES", "60"))
settings = Settings()