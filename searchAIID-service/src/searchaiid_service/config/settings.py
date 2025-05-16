import os
from pydantic_settings import BaseSettings
from dotenv import load_dotenv

load_dotenv()

class Settings(BaseSettings):
    APP_NAME: str = "searchaiid-service"
    DEBUG: bool = os.getenv("DEBUG", "False").lower() == "true"
    HOST: str = os.getenv("HOST", "0.0.0.0")
    PORT: int = int(os.getenv("PORT", "5000"))
    MODEL_PATH: str = os.getenv("MODEL_PATH", "5CD-AI/Vintern-1B-v3_5")
    MAX_IMAGE_SIZE: int = int(os.getenv("MAX_IMAGE_SIZE", "448"))
    MAX_IMAGE_BLOCKS: int = int(os.getenv("MAX_IMAGE_BLOCKS", "6"))

    # JWT Settings
    JWT_SECRET: str = os.getenv("JWT_SECRET", "conghoaxahoichunghiavietnam/doclaptudohanhphuc/1975/1945")
    JWT_ALGORITHM: str = os.getenv("JWT_ALGORITHM", "HS256")
    JWT_EXPIRATION_MINUTES: int = int(os.getenv("JWT_EXPIRATION_MINUTES", "60"))

    # Kafka Settings
    KAFKA_BOOTSTRAP_SERVERS: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9094")
    KAFKA_CONSUMER_GROUP: str = os.getenv("KAFKA_CONSUMER_GROUP", "searchaiid-group")
    KAFKA_OCR_REQUEST_TOPIC: str = os.getenv("KAFKA_OCR_REQUEST_TOPIC", "ocr-request")
    KAFKA_OCR_RESPONSE_TOPIC: str = os.getenv("KAFKA_OCR_RESPONSE_TOPIC", "ocr-response")
    KAFKA_AUTO_OFFSET_RESET: str = os.getenv("KAFKA_AUTO_OFFSET_RESET", "earliest")
settings = Settings()