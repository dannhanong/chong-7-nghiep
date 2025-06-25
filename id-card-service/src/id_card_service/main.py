import uvicorn
import logging
import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import py_eureka_client.eureka_client as eureka_client

from id_card_service.api.router import router
from id_card_service.config.settings import settings
from id_card_service.core.kafka.kafka_consumer import KafkaEventConsumer

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="ID Card Service",
    description="",
    version="0.1.0"
)

kafka_consumer = None

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(router, prefix="/id")

@app.get("/health")
def health_check():
    return {"status": "healthy"}

@app.on_event("startup")
async def startup_event():
    global kafka_consumer

    """Khi ứng dụng khởi động, đăng ký với Eureka"""
    # Khởi tạo Kafka consumer
    try:
        logger.info("Starting Kafka consumer...")
        kafka_consumer = KafkaEventConsumer()
        kafka_consumer.start()
        logger.info("Kafka consumer started successfully")
    except Exception as e:
        logger.error(f"Failed to start Kafka consumer: {str(e)}")

    try:
        logger.info("Registering service with Eureka...")
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