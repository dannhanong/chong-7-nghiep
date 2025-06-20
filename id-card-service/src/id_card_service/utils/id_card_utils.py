import logging
from id_card_service.db.mongodb import MongoDB
from id_card_service.config.settings import settings

logger = logging.getLogger(__name__)

def get_id_card_by_user_id(user_id: str) -> dict:
    """
    Lấy thông tin CCCD từ user_id
    Args:
        user_id: ID người dùng cần tìm
    Returns:
        Thông tin CCCD nếu tìm thấy, None nếu không tìm thấy
    """
    if not user_id:
        return None
    
    try:
        db = MongoDB()
        identity_cards_collection = db.get_collection(
            settings.MONGODB_USER_DATABASE, 
            settings.MONGODB_IDENTITY_CARDS_COLLECTION
        )
        identity_card = identity_cards_collection.find_one({"userId": user_id})

        if identity_card:
            return identity_card
        
        logger.warning(f"Identity card for user_id '{user_id}' not found in database.")
        return None
    
    except Exception as e:
        logger.error(f"Error retrieving identity card for user_id '{user_id}': {e}")
        return None 