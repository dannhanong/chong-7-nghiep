import base64
import os
import statistics
import time
import logging
import traceback
import concurrent
import numpy as np
from typing import List, Dict, Any, Optional
from fastapi import APIRouter, Body, Depends, Path, Request, UploadFile, File, HTTPException, Form
from pydantic import BaseModel
import io
from PIL import Image
import numpy as np
import cv2
import asyncio

from ..utils.user_utils import get_user_id_from_username, update_user_identity_verify
from ..core.model_manager import model_manager
from ..config.settings import settings
from ..core.face_recognizer import FaceRecognizer
from ..core.face_detector import FaceDetector
from ..core.auth.jwt_service import jwt_service
from ..core.ocr_engine import get_ocr_engine

logger = logging.getLogger(__name__)
router = APIRouter()
ocr_engine = get_ocr_engine()

class IDCardNumberInfo(BaseModel):
    """Information about extracted ID card number."""
    number: str = None
    confidence: float = 0.0

class OtherTextInfo(BaseModel):
    """Information about other extracted text."""
    number: str = None
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

def get_current_username(request: Request) -> Optional[str]:
    return jwt_service.get_username_from_request(request)

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
    summary="Extract Text from CCCD",
    description="Upload a CCCD image and extract all text using OCR"
)
async def extract_text(
    request: Request,
    file: UploadFile = File(..., description="CCCD image file (JPG, PNG, PDF)"),
    min_confidence: float = Form(0.5, description="Minimum confidence threshold")
):
    """Extract raw text from CCCD image."""    
    try:
        username = get_current_username(request)
        if not username:
            raise HTTPException(
                status_code=401,
                detail="Vui lòng đăng nhập để sử dụng dịch vụ này."
            )

        # Validate and read file
        image_data = await validate_file(file)
        
        # Chuyển đổi sang định dạng ảnh
        pil_image = Image.open(io.BytesIO(image_data))
        if pil_image.mode != 'RGB':
            pil_image = pil_image.convert('RGB')
        processed_image = np.array(pil_image)
        
        ocr_result = ocr_engine.extract_text(processed_image, min_confidence)

        print(f"OCR result: {ocr_result}")

        if (
            not ocr_result.get("success")
            or not ocr_result.get("id_card_number")
            or not ocr_result["id_card_number"].get("number")
            or not ocr_result["id_card_number"].get("confidence")
            or not ocr_result.get("other_info")
            or not ocr_result["other_info"].get("full_name")
            or not ocr_result["other_info"].get("dob")
            or not ocr_result["other_info"].get("sex")
            or not ocr_result["other_info"].get("nationality")
            or not ocr_result["other_info"].get("origin")
            or not ocr_result["other_info"].get("residence")
            or not ocr_result["other_info"].get("expiry_date")
        ):
            raise HTTPException(
                status_code=400,
                detail="Lấy thông tin từ CCCD không thành công. Vui lòng kiểm tra lại."
            )
                
        return OtherTextInfo(
            success=True,
            # id_card_number=IDCardNumberInfo(
            #     number=ocr_result['id_card_number']['number'],
            #     confidence=ocr_result['id_card_number']['confidence']
            # ),
            # other_info=OtherTextInfo(
            #     number=ocr_result['id_card_number'].get('number', ''),
            #     full_name=ocr_result['other_info']['full_name'],
            #     dob=ocr_result['other_info']['dob'],
            #     sex=ocr_result['other_info']['sex'],
            #     nationality=ocr_result['other_info']['nationality'],
            #     origin=ocr_result['other_info']['origin'],
            #     residence=ocr_result['other_info']['residence'],
            #     expiry_date=ocr_result['other_info']['expiry_date']
            # )
            number=ocr_result['id_card_number'].get('number', ''),
            full_name=ocr_result['other_info']['full_name'],
            dob=ocr_result['other_info']['dob'],
            sex=ocr_result['other_info']['sex'],
            nationality=ocr_result['other_info']['nationality'],
            origin=ocr_result['other_info']['origin'],
            residence=ocr_result['other_info']['residence'],
            expiry_date=ocr_result['other_info']['expiry_date']
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
    
@router.post(
    "/add-person/{user_id}",
    description="Add a new person to the ID card service"
)
async def add_person(
    user_id: str = Path(..., description="User ID of the person")
):
    """
    Add a new person to the ID card service.
    
    Args:
        user_id: User ID of the person to add.
        
    Returns:
        Success message if the person is added successfully.
    """
    try:
        face_recognizer = FaceRecognizer()

        result = face_recognizer.register_person_from_database(
            user_id=user_id
        )
        
        if result["success"]:
                logger.info(f"✅ SUCCESS: {result['message']}")
                logger.info(f"   Person ID: {result['person_id']}")
                
                if result.get("warnings"):
                    logger.warning("⚠️  Warnings:")
                    for warning in result["warnings"]:
                        logger.warning(f"     - {warning}")
                else:
                    logger.error(f"❌ FAILED: {result['message']}")
                    
                return result
    
    except Exception as e:
        logger.error(f"Failed to add person: {str(e)}")
        raise HTTPException(status_code=500, detail="Failed to add person")

recognizer = None

def get_recognizer():
    """Singleton pattern để khởi tạo recognizer chỉ một lần"""
    global recognizer
    if recognizer is None:
        recognizer = FaceRecognizer()
    return recognizer

@router.post(
    "/verify-face",
    response_model=dict,
    summary="Batch Face Recognition",
    description="Recognize faces in multiple images and return aggregated results"
)
async def batch_recognize(
    request: Request,
    data: Dict = Body(...),
    recognizer: FaceRecognizer = Depends(get_recognizer)
):
    """API for batch face recognition."""
    try:
        # Lấy dữ liệu từ request
        images = data.get("images", [])
        # user_id = data.get("user_id")
        username = get_current_username(request)
        user_id = get_user_id_from_username(username) if username else None
        specific_mode = data.get("specific_mode", True)
        
        if not images:
            return {"error": "No images provided"}
        
        if specific_mode and not user_id:
            return {"error": "User ID is required for specific mode"}
        
        logger.info(f"Batch recognition request: {len(images)} images, user_id={user_id}")
        
        # Khởi tạo face detector
        detector = FaceDetector(scale_factor=0.5)
        
        # Biến để lưu kết quả nhận diện
        successful_recognitions = 0
        confidence_scores = []
        distance_scores = []
        individual_results = []
        
        # Lập qua từng ảnh
        for i, base64_image in enumerate(images):
            try:
                # Decode ảnh
                image_data = base64.b64decode(base64_image)
                nparr = np.frombuffer(image_data, np.uint8)
                frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                
                if frame is None or frame.size == 0:
                    logger.warning(f"Invalid image data in image {i+1}")
                    continue
                
                # Phát hiện khuôn mặt
                face_locations = detector.detect_faces(frame)
                
                if not face_locations:
                    logger.info(f"No face detected in image {i+1}")
                    continue
                
                # Lấy khuôn mặt lớn nhất
                face_location = face_locations[0]
                top, right, bottom, left = face_location
                face_image = frame[top:bottom, left:right]
                
                # Xử lý nhận diện trong thread riêng
                loop = asyncio.get_running_loop()
                executor = concurrent.futures.ThreadPoolExecutor(max_workers=4)
                if specific_mode:
                    # Xác thực 1:1
                    result_future = loop.run_in_executor(
                        executor,
                        recognizer.recognize_face_with_saved_user_id_embedding,
                        user_id, face_image, 0.55
                    )
                
                # Chờ kết quả với timeout
                recognition_result = await asyncio.wait_for(result_future, timeout=5.0)
                
                # Lưu kết quả individual
                individual_results.append({
                    "recognized": recognition_result["recognized"],
                    "confidence": recognition_result.get("confidence", 0),
                    "distance": recognition_result.get("distance", 1.0)
                })
                
                # Nếu nhận diện thành công, cập nhật thống kê
                if recognition_result["recognized"]:
                    successful_recognitions += 1
                    confidence_scores.append(recognition_result.get("confidence", 0))
                    distance_scores.append(recognition_result.get("distance", 1.0))
                
            except Exception as e:
                logger.error(f"Error processing image {i+1}: {str(e)}")
                logger.error(traceback.format_exc())
                continue
        
        # Tính toán kết quả cuối cùng
        total_processed = len(individual_results)
        
        if total_processed == 0:
            return {
                "recognized": False,
                "message": "No valid face images could be processed",
                "processed_images": 0,
                "total_images": len(images)
            }
        
        # Nếu ít nhất 50% ảnh được nhận diện thành công, coi như nhận diện thành công
        recognition_threshold = 0.5  # 50%
        recognized = (successful_recognitions / total_processed) >= recognition_threshold
        
        # Tính điểm trung bình
        avg_confidence = statistics.mean(confidence_scores) if confidence_scores else 0
        avg_distance = statistics.mean(distance_scores) if distance_scores else 1.0
        
        if avg_confidence > 0.55:
            logger.info(f"Batch recognition successful: {successful_recognitions}/{total_processed} images recognized")
            update_user_identity_verify(username)
            return {
                "recognized": recognized,
                "user_id": user_id,
                "average_confidence": avg_confidence,
                "average_distance": avg_distance,
                "successful_recognitions": successful_recognitions,
                "processed_images": total_processed,
                "total_images": len(images),
                "recognition_rate": (successful_recognitions / total_processed) if total_processed > 0 else 0,
                "individual_results": individual_results
            }
        
    except Exception as e:
        logger.error(f"Batch recognition error: {str(e)}")
        logger.error(traceback.format_exc())
        raise HTTPException(status_code=500, detail=f"Processing error: {str(e)}")