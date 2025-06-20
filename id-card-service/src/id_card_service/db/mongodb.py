from pymongo import MongoClient
from id_card_service.config.settings import settings
import logging

logger = logging.getLogger(__name__)

class MongoDB:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(MongoDB, cls).__new__(cls)
            cls._instance._init_connection()

        return cls._instance
    
    def _init_connection(self):
        try:
            self.client = MongoClient(settings.MONGODB_URI)
        except Exception as e:
            logger.error(f"Failed to connect to MongoDB: {e}")
            self.client = None
            self.db = None
    
    def get_collection(self, database, collection):
        if not self.client:
            raise ConnectionError("MongoDB connection not established")
        
        db = self.client[database]
        return db[collection]
    
    def close(self):
        if self.client:
            self.client.close()
            logger.info("MongoDB connection closed")
