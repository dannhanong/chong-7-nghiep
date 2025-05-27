import logging
from typing import Optional
from recommend_service.db.mongodb import MongoDB

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
        users_collection = db.get_collection("jobs_auth", "users")
        user = users_collection.find_one({"username": username})

        if user:
            user_id = str(user["_id"])
            _username_cache[username] = user_id
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
        users_collection = db.get_collection("jobs_auth", "users")
        user = users_collection.find_one({"_id": user_id})
        
        if user:
            return user.get("username")
            
        logger.warning(f"User ID {user_id} not found in database")
        return None
        
    except Exception as e:
        logger.error(f"Error getting username from user_id: {e}")
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
        users_collection = db.get_collection("jobs_auth", "users")
        users = users_collection.find({}, {"username": 1, "providerType": 1})

        return [user["username"] for user in users if "username" in user and user.get("providerType") == "LOCAL"]
        
    except Exception as e:
        logger.error(f"Error retrieving all usernames: {e}")
        return []