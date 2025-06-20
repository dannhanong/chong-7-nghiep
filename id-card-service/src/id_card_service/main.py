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
from id_card_service.core.model_manager import model_manager

# Configure logging
# logging.basicConfig(
#     level=getattr(logging, settings.LOG_LEVEL),
#     format=settings.LOG_FORMAT
# )
logger = logging.getLogger(__name__)


# @asynccontextmanager
# async def lifespan(app: FastAPI):
#     """Application lifespan events."""
#     # Startup
#     logger.info(f"Starting {settings.APP_NAME} v{settings.VERSION}")
    
#     # Preload OCR model if configured
#     if settings.PRELOAD_MODEL:
#         try:
#             logger.info("Preloading OCR model...")
#             await asyncio.get_event_loop().run_in_executor(
#                 None, model_manager.preload_default_model
#             )
#             logger.info("OCR model preloaded successfully")
#         except Exception as e:
#             logger.warning(f"Failed to preload OCR model: {str(e)}")
    
#     yield
    
#     # Shutdown
#     logger.info("Shutting down application...")
#     model_manager.clear_cache()
#     logger.info("Application shutdown complete")


app = FastAPI(
    title=settings.API_TITLE,
    description=settings.API_DESCRIPTION,
    version=settings.API_VERSION,
    # lifespan=lifespan,
    docs_url="/docs" if settings.DEBUG else None,
    redoc_url="/redoc" if settings.DEBUG else None,
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

# Include routers
app.include_router(router, prefix="/id", tags=["CCCD Processing"])

@app.get("/health", tags=["Health"])
async def health_check():
    """Basic health check endpoint."""
    return {"status": "healthy", "service": settings.APP_NAME, "version": settings.VERSION}

@app.on_event("startup")
async def startup_event():
    """Khi ứng dụng khởi động, đăng ký với Eureka"""
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
        "id_card_service.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG,
        workers=1 if settings.DEBUG else settings.MAX_WORKERS,
        log_level=settings.LOG_LEVEL.lower()
    )