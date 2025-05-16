from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class OCRRequest(BaseModel):
    prompt: Optional[str] = None

class OCRResponse(BaseModel):
    success: bool
    data: Dict[str, Any]
    error: Optional[str] = None