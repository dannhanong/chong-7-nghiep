"""API request and response schemas."""

from typing import Optional
from pydantic import BaseModel, Field

# Re-export for API use
__all__ = [
    "CCCDInfo",
    "ExtractionResponse", 
    "ProcessingInfo",
    "HealthResponse",
    "ExtractRequest",
    "BatchExtractRequest", 
    "BatchExtractResponse",
    "ErrorResponse",
    "UploadParams"
]


class UploadParams(BaseModel):
    """Parameters for file upload endpoint."""
    
    side: Optional[str] = Field(
        "auto", 
        description="CCCD side to process: 'front', 'back', or 'auto'",
        regex="^(front|back|auto)$"
    )
    enhance_image: bool = Field(
        True, 
        description="Whether to apply image enhancement preprocessing"
    )
    min_confidence: float = Field(
        0.5, 
        ge=0.0, 
        le=1.0, 
        description="Minimum OCR confidence threshold (0.0-1.0)"
    )
    
    class Config:
        schema_extra = {
            "example": {
                "side": "auto",
                "enhance_image": True,
                "min_confidence": 0.6
            }
        }