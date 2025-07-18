from recommend_service.core.recommendation.models.hybrid import HybridRecommender
from recommend_service.core.recommendation.models.semantic_content_based import SemanticContentBasedRecommender
import logging

logger = logging.getLogger(__name__)

class SingletonMeta(type):
    """Metaclass đảm bảo Singleton pattern"""
    _instances = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            cls._instances[cls] = super(SingletonMeta, cls).__call__(*args, **kwargs)
        return cls._instances[cls]
    
class RecommenderService(metaclass=SingletonMeta):
    """Singleton service quản lý các recommender"""
    
    def __init__(self):
        self._semantic_recommender = None
        self._hybrid_recommender = None
        logger.info("RecommenderService instance created")
    
    @property
    def semantic_recommender(self):
        if self._semantic_recommender is None:
            logger.warning("Semantic recommender is being accessed but was not initialized")
            self._semantic_recommender = SemanticContentBasedRecommender()
        return self._semantic_recommender
    
    @semantic_recommender.setter
    def semantic_recommender(self, recommender):
        self._semantic_recommender = recommender
        logger.info("Semantic recommender has been set")
    
    @property
    def hybrid_recommender(self):
        if self._hybrid_recommender is None:
            logger.info("Initializing HybridRecommender")
            self._hybrid_recommender = HybridRecommender(
                semantic_recommender=self.semantic_recommender
            )
            logger.info("HybridRecommender initialized")
        return self._hybrid_recommender
    
    def initialize(self, semantic_recommender=None):
        """Khởi tạo các recommender với instance đã được cung cấp"""
        if semantic_recommender:
            self._semantic_recommender = semantic_recommender
            logger.info("Semantic recommender initialized from provided instance")
        
        # Lazy initialization của hybrid_recommender sẽ xảy ra khi cần
        logger.info("RecommenderService initialized successfully")