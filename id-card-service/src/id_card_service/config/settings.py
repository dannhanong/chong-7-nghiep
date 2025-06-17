import os
from typing import List
from pydantic_settings import BaseSettings
from dotenv import load_dotenv
import uuid

load_dotenv()

class Settings(BaseSettings):
    # App settings
    APP_NAME: str = "id-card-service"
    VERSION: str = "0.1.0"
    DEBUG: bool = os.getenv("DEBUG", "False").lower() == "true"
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "5000"))
    
    # OCR settings
    OCR_LANG: str = os.getenv("OCR_LANG", "en")  # Vietnamese model supports Vietnamese characters

    # Processing settings
    MAX_FILE_SIZE: int = int(os.getenv("MAX_FILE_SIZE", str(10 * 1024 * 1024)))  # 10MB
    SUPPORTED_FORMATS: List[str] = ["jpg", "jpeg", "png", "pdf"]
    MIN_CONFIDENCE: float = float(os.getenv("MIN_CONFIDENCE", "0.6"))
    
    # Image processing settings
    TARGET_WIDTH: int = int(os.getenv("TARGET_WIDTH", "1200"))
    TARGET_HEIGHT: int = int(os.getenv("TARGET_HEIGHT", "800"))
    MIN_WIDTH: int = int(os.getenv("MIN_WIDTH", "600"))
    MIN_HEIGHT: int = int(os.getenv("MIN_HEIGHT", "400"))
    
    # Model settings
    MODEL_CACHE_SIZE: int = int(os.getenv("MODEL_CACHE_SIZE", "3"))
    MODEL_PATH: str = os.getenv("MODEL_PATH", "./models/")
    PRELOAD_MODEL: bool = os.getenv("PRELOAD_MODEL", "True").lower() == "true"
    
    # API settings
    API_TITLE: str = "ID Card Service API"
    API_DESCRIPTION: str = "REST API for Vietnamese CCCD information extraction using OCR"
    API_VERSION: str = VERSION
    CORS_ORIGINS: List[str] = ["*"]  # Configure as needed for production
    
    # Logging settings
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO").upper()
    LOG_FORMAT: str = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    
    # Performance settings
    MAX_WORKERS: int = int(os.getenv("MAX_WORKERS", "4"))
    REQUEST_TIMEOUT: int = int(os.getenv("REQUEST_TIMEOUT", "60"))  # seconds

    # OpenAI API settings
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY")

    # Eureka Settings
    EUREKA_SERVER_URL: str = os.getenv("EUREKA_SERVER_URL", "http://localhost:9999/eureka")
    EUREKA_APP_NAME: str = os.getenv("EUREKA_APP_NAME", "id-card-service")
    EUREKA_INSTANCE_PORT: int = int(os.getenv("EUREKA_INSTANCE_PORT", PORT))
    EUREKA_INSTANCE_HOST: str = os.getenv("EUREKA_INSTANCE_HOST", HOST)
    EUREKA_HEARTBEAT_INTERVAL: int = int(os.getenv("EUREKA_HEARTBEAT_INTERVAL", 30))
    EUREKA_RENEWAL_INTERVAL: int = int(os.getenv("EUREKA_RENEWAL_INTERVAL", 30))
    EUREKA_INSTANCE_ID: str = os.getenv("EUREKA_INSTANCE_ID", f"{EUREKA_APP_NAME}:{uuid.uuid4()}")
    
    class Config:
        env_file = ".env"
        case_sensitive = True

settings = Settings()