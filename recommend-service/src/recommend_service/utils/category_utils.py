import logging
from typing import Optional
from recommend_service.db.mongodb import MongoDB
from bson import ObjectId

logger = logging.getLogger(__name__)

def get_category_info_by_category_id(category_id: str) -> Optional[dict]:
    """
    Lấy thông tin category từ category_id
    
    Args:
        category_id: ID của category cần tìm
        
    Returns:
        Thông tin category nếu tìm thấy, None nếu không tìm thấy
    """
    if not category_id:
        return None
    
    try:
        db = MongoDB()
        categories_collection = db.get_collection("jobs_job", "categories")
        category = categories_collection.find_one({"_id": ObjectId(category_id)})
        
        if category:
            return {
                "id": str(category["_id"]),
                "name": category.get("name", "")
            }
        
        logger.warning(f"Category with ID '{category_id}' not found in database.")
        return None
    
    except Exception as e:
        logger.error(f"Error retrieving category info for ID '{category_id}': {e}")
        return None