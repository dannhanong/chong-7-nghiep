import logging
from typing import Optional
from bson import ObjectId
from id_card_service.db.mongodb import MongoDB
from id_card_service.config.settings import settings

logger = logging.getLogger(__name__)

_username_cache = {}

def get_user_id_from_username(username: str) -> Optional[str]:
    """
    Lấy user_id từ username
    
    Args:
        username: Tên người dùng cần tìm
        
    Returns:
        user_id nếu tìm thấy, None nếu không tìm thấy
    """
    if not username:
        return None
    
    if username in _username_cache:
        return _username_cache[username]
    
    try:
        db = MongoDB()
        users_collection = db.get_collection(
            settings.MONGODB_USER_DATABASE, 
            settings.MONGODB_USERS_COLLECTION
        )
        user = users_collection.find_one({"username": username})

        if user:
            user_id = str(user["_id"])
            # _username_cache[username] = user_id
            return user_id

        logger.warning(f"User with username '{username}' not found in database.")
        return None
    
    except Exception as e:
        logger.error(f"Error retrieving user_id for username '{username}': {e}")
        return None
        
def get_username_from_user_id(user_id: str) -> Optional[str]:
    """
    Lấy username từ user_id
    
    Args:
        user_id: ID người dùng cần tìm
        
    Returns:
        username nếu tìm thấy, None nếu không tìm thấy
    """
    if not user_id:
        return None
        
    try:
        db = MongoDB()
        users_collection = db.get_collection(
            settings.MONGODB_USER_DATABASE, 
            settings.MONGODB_USERS_COLLECTION
        )
        user = users_collection.find_one({"_id": user_id})
        
        if user:
            return user.get("username")
            
        logger.warning(f"User ID {user_id} not found in database")
        return None
        
    except Exception as e:
        logger.error(f"Error getting username from user_id: {e}")
        return None
    
def get_email_from_username(username: str) -> Optional[str]:
    """
    Lấy email từ username
    
    Args:
        username: Tên người dùng cần tìm
        
    Returns:
        Email nếu tìm thấy, None nếu không tìm thấy
    """
    if not username:
        return None
    
    try:
        db = MongoDB()
        users_collection = db.get_collection(
            settings.MONGODB_USER_DATABASE, 
            settings.MONGODB_USERS_COLLECTION
        )
        user = users_collection.find_one({"username": username})

        if user:
            return user.get("email")
        
        logger.warning(f"User with username '{username}' not found in database.")
        return None
    
    except Exception as e:
        logger.error(f"Error retrieving email for username '{username}': {e}")
        return None

def clear_username_cache():
    """Xóa cache username -> user_id"""
    _username_cache.clear()

def get_all_usernames() -> list[str]:
    """
    Lấy danh sách tất cả usernames từ cơ sở dữ liệu
    
    Returns:
        Danh sách usernames
    """
    try:
        db = MongoDB()
        users_collection = db.get_collection(
            settings.MONGODB_USER_DATABASE, 
            settings.MONGODB_USERS_COLLECTION
        )
        users = users_collection.find({}, {"username": 1})

        return [user["username"] for user in users if "username" in user]
        
    except Exception as e:
        logger.error(f"Error retrieving all usernames: {e}")
        return []
    
def get_user_info_by_user_id(user_id: str) -> Optional[dict]:
    """
    Lấy thông tin người dùng từ user_id
    
    Args:
        user_id: ID người dùng cần tìm
        
    Returns:
        Thông tin người dùng nếu tìm thấy, None nếu không tìm thấy
    """
    if not user_id:
        return None
        
    try:
        db = MongoDB()
        users_collection = db.get_collection(
            settings.MONGODB_USER_DATABASE, 
            settings.MONGODB_USERS_COLLECTION
        )
        object_user_id = ObjectId(user_id) if isinstance(user_id, str) else user_id
        user = users_collection.find_one({"_id": object_user_id})

        if user:
            return {
                "user_id": str(user["_id"]),
                "name": user.get("name"),
                "email": user.get("email"),
                "phone_number": user.get("phoneNumber"),
            }
            
        logger.warning(f"User ID {user_id} not found in database")
        return None
        
    except Exception as e:
        logger.error(f"Error getting user info by user_id: {e}")
        return None
    
def update_user_identity_verify(username):
    if not username:
        return None

    try:
        db = MongoDB()
        users_collection = db.get_collection(
            settings.MONGODB_USER_DATABASE, 
            settings.MONGODB_USERS_COLLECTION
        )
        result = users_collection.update_one({"username": username}, {"$set": {"identityVerified": True}})

        if result.modified_count > 0:
            logger.info(f"User '{username}' identity verification status updated.")
            return True
        else:
            logger.warning(f"User '{username}' not found or already verified.")
            return False

    except Exception as e:
        logger.error(f"Error updating user identity verification: {e}")
        return None