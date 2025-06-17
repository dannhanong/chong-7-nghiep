import uvicorn
from id_card_service.config.settings import settings

def main():
    try:
        uvicorn.run(
            "id_card_service.main:app",
            host=settings.HOST,
            port=settings.PORT,
            reload=True,
            log_level="info"
        )

    except ImportError as e:
        print(f"Error importing modules: {e}")

if __name__ == "__main__":
    main()