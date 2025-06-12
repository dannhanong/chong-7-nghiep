import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import logging
from recommend_service.api.router import router
from recommend_service.config.settings import settings
import py_eureka_client.eureka_client as eureka_client
from recommend_service.core.recommendation.models.semantic_content_based import SemanticContentBasedRecommender
from recommend_service.core.kafka.kafka_consumer import KafkaEventConsumer

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Job Recommendation Service",
    description="",
    version="0.1.0",
)

semantic_recommender = None
kafka_consumer = None

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router, prefix="/recommend")

@app.get("/health")
def health_check():
    return {"status": "healthy"}

@app.on_event("startup")
async def startup_event():
    """Khi ứng dụng khởi động, đăng ký với Eureka"""
    global semantic_recommender, kafka_consumer

    # Khởi tạo semantic recommender
    try:
        logger.info("Initializing Semantic Content Based Recommender...")
        semantic_recommender = SemanticContentBasedRecommender()
        semantic_recommender.fit()  # Load/create embeddings
        logger.info("Semantic Content Based Recommender initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize Semantic Content Based Recommender: {str(e)}")

    # Khởi tạo Kafka consumer
    try:
        logger.info("Starting Kafka consumer...")
        kafka_consumer = KafkaEventConsumer(semantic_recommender)
        kafka_consumer.start()
        logger.info("Kafka consumer started successfully")
    except Exception as e:
        logger.error(f"Failed to start Kafka consumer: {str(e)}")

    try:
        await eureka_client.init_async(
            eureka_server=settings.EUREKA_SERVER_URL,
            app_name=settings.EUREKA_APP_NAME,
            instance_host=settings.EUREKA_INSTANCE_HOST,
            instance_port=settings.EUREKA_INSTANCE_PORT,
            instance_id=settings.EUREKA_INSTANCE_ID,
        )
        logger.info(f"Service registered as {settings.EUREKA_APP_NAME}")
    except Exception as e:
        logger.error(f"Failed to register with Eureka: {str(e)}")

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG
    )