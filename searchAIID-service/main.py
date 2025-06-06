import uvicorn
from fastapi import FastAPI

from searchaiid_service.api.router import router
from searchaiid_service.config.settings import settings

app = FastAPI(
    title="SearchAIID Service",
    description="",
    version="0.1.0",
)

app.include_router(router, prefix="/idai")

@app.get("/health")
def health_check():
    return {"status": "healthy"}

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG
    )