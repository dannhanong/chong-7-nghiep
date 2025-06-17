import os
import time
import logging
import numpy as np
from typing import List, Dict, Any
from fastapi import APIRouter, UploadFile, File, HTTPException, Form
from pydantic import BaseModel
import io
from PIL import Image
import numpy as np

from ..core.model_manager import model_manager
from ..config.settings import settings

logger = logging.getLogger(__name__)
router = APIRouter()


class IDCardNumberInfo(BaseModel):
    """Information about extracted ID card number."""
    number: str = None
    confidence: float = 0.0

# class OtherTextInfo(BaseModel):
#     """Information about other extracted text."""
#     texts: List[str] = []
#     confidence_scores: List[float] = []
#     average_confidence: float = 0.0
class OtherTextInfo(BaseModel):
    """Information about other extracted text."""
    full_name: str = None
    dob: str = None
    sex: str = None
    nationality: str = None
    origin: str = None
    residence: str = None
    expiry_date: str = None

class OCRResponse(BaseModel):
    """Enhanced OCR response model for CCCD extraction."""
    success: bool
    id_card_number: IDCardNumberInfo = None
    other_info: OtherTextInfo = None
    processing_time: float = 0.0
    image_info: Dict[str, Any] = {}
    error: str = None


async def validate_file(file: UploadFile) -> bytes:
    """Validate uploaded file and return content."""
    # Check file size
    content = await file.read()
    if len(content) > settings.MAX_FILE_SIZE:
        raise HTTPException(
            status_code=413,
            detail=f"File too large. Maximum size: {settings.MAX_FILE_SIZE / 1024 / 1024:.1f}MB"
        )
    
    # Check file format
    file_extension = file.filename.lower().split('.')[-1] if file.filename else ""
    if file_extension not in settings.SUPPORTED_FORMATS:
        raise HTTPException(
            status_code=400,
            detail=f"Unsupported file format. Supported formats: {', '.join(settings.SUPPORTED_FORMATS)}"
        )
    
    return content


@router.post(
    "/extract",
    response_model=OCRResponse,
    summary="Extract Text from CCCD",
    description="Upload a CCCD image and extract all text using OCR"
)
async def extract_text(
    file: UploadFile = File(..., description="CCCD image file (JPG, PNG, PDF)"),
    min_confidence: float = Form(0.5, description="Minimum confidence threshold")
):
    """Extract raw text from CCCD image."""
    start_time = time.time()
    
    try:
        # Validate and read file
        image_data = await validate_file(file)
        
        # Chuyển đổi sang định dạng ảnh
        pil_image = Image.open(io.BytesIO(image_data))
        if pil_image.mode != 'RGB':
            pil_image = pil_image.convert('RGB')
        processed_image = np.array(pil_image)
        
        image_info = {
            'original_size': processed_image.shape[:2],
            'final_size': processed_image.shape[:2],
            'operations_applied': ['format_conversion'],
            'rotation_angle': 0.0
        }
                    
        # Get OCR model and extract text
        ocr_model = model_manager.get_model({
            'lang': settings.OCR_LANG
        })
        
        ocr_result = ocr_model.extract_text(processed_image, min_confidence)
        
        # Return simple OCR response
        processing_time = time.time() - start_time
        # total_texts = len(ocr_result['other_info']['texts'])
        # if ocr_result['id_card_number']['number']:
        #     total_texts += 1
        
        return OCRResponse(
            success=True,
            id_card_number=IDCardNumberInfo(
                number=ocr_result['id_card_number']['number'],
                confidence=ocr_result['id_card_number']['confidence']
            ),
            other_info=OtherTextInfo(
                full_name=ocr_result['other_info']['full_name'],
                dob=ocr_result['other_info']['dob'],
                sex=ocr_result['other_info']['sex'],
                nationality=ocr_result['other_info']['nationality'],
                origin=ocr_result['other_info']['origin'],
                residence=ocr_result['other_info']['residence'],
                expiry_date=ocr_result['other_info']['expiry_date']
            ),
            processing_time=processing_time,
            image_info=image_info
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"OCR extraction failed: {str(e)}", exc_info=True)
        return OCRResponse(
            success=False,
            error=f"Processing failed: {str(e)}"
        )


@router.get(
    "/health",
    response_model=dict,
    summary="Health Check",
    description="Check the health status of the ID Card Service"
)
async def health_check():
    """Health check endpoint."""
    try:
        # Check OCR model availability
        model_info = model_manager.get_cache_info()
        model_loaded = model_info['total_cached'] > 0
        
        try:
            if not model_loaded and settings.PRELOAD_MODEL:
                # Try to load model
                model_manager.get_model()
                model_loaded = True
        except Exception as e:
            logger.warning(f"Model loading test failed: {str(e)}")
            model_loaded = False
        
        return {
            "status": "healthy" if model_loaded else "degraded",
            "ocr_engine": "available" if model_loaded else "unavailable",
            "model_loaded": model_loaded,
            "version": settings.VERSION
        }
        
    except Exception as e:
        logger.error(f"Health check failed: {str(e)}")
        return {
            "status": "unhealthy",
            "ocr_engine": "error",
            "model_loaded": False,
            "version": settings.VERSION
        }