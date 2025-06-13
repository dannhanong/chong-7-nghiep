from enum import Enum
import os
import tempfile
from typing import List, Optional
from fastapi import APIRouter, Query, UploadFile, File, HTTPException, Request
from pydantic import BaseModel
from searchaiid_service.api.models import OCRResponse, OCRRequest
from searchaiid_service.core.auth.jwt_service import jwt_service
from searchaiid_service.models.vintern import VinternModel
from searchaiid_service.core.kafka.kafka_service import kafka_service
from searchaiid_service.models.lightweight_id_extractor import LightweightIDExtractor
from searchaiid_service.models.qrid_extractor import QRIDExtractor
import logging
import torch
import uuid
import time

logger = logging.getLogger(__name__)
router = APIRouter()

class QuantizationMethod(str, Enum):
    NONE = "none"
    DYNAMIC = "dynamic"
    STATIC = "static" 
    ONNX = "onnx"

class BenchmarkResponse(BaseModel):
    method: str
    processing_time: float
    model_size_mb: float
    success: bool
    error: Optional[str] = None

@router.post("/ocr", response_model=OCRResponse)
async def process_id_card(
    auth: Request,
    file: UploadFile = File(...),
    request: OCRRequest = None
) -> OCRResponse:
    try:
        username = jwt_service.get_username_from_request(auth)
        print(f"Username: {username}")

        with tempfile.NamedTemporaryFile(delete=False) as temp_file:
            temp_file.write(await file.read())
            temp_path = temp_file.name

        try:
            model = VinternModel()
            prompt = request.prompt if request and request.prompt else None

            try:
                data = model.extract_info(temp_path, prompt)
            except RuntimeError as e:
                if "Input type (float) and bias type (struct c10::BFloat16)" in str(e):
                    logger.warning("Trying to convert model to float32 and retry...")
                    model.model = model.model.to(torch.float32)
                    data = model.extract_info(temp_path, prompt)
                else:
                    raise
            return OCRResponse(
                success=True,
                data=data
            )
        finally:
            os.unlink(temp_path)

        # message = {
        #     "username": username,
        # }

        # kafka_service.produce_message(
        #     topic="ocr_test",
        #     key=str(uuid.uuid4()),
        #     value=message,
        # )

        return OCRResponse(
            success=True,
            data={}
        )

    except Exception as e:
        logger.error(f"Error processing file: {e}")
        raise HTTPException(status_code=500, detail=str(e)
)
    
@router.post("/ocr/lightweight", response_model=OCRResponse)
async def process_id_card_lightweight(
    auth: Request,
    file: UploadFile = File(...),
) -> OCRResponse:
    """Lightweight ID card processing without heavy models"""
    try:
        username = jwt_service.get_username_from_request(auth)
        logger.info(f"Processing lightweight OCR request for user: {username}")

        with tempfile.NamedTemporaryFile(delete=False) as temp_file:
            temp_file.write(await file.read())
            temp_path = temp_file.name

        try:
            # Sử dụng trích xuất nhẹ
            start_time = time.time()
            lightweight_extractor = LightweightIDExtractor()
            data = lightweight_extractor.extract(temp_path)
            processing_time = time.time() - start_time
            
            return OCRResponse(
                success=True,
                data=data,
                processing_time=processing_time
            )
        finally:
            os.unlink(temp_path)
            
    except Exception as e:
        logger.error(f"Error processing file with lightweight method: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    
@router.post("/ocr/smart", response_model=OCRResponse)
async def process_id_card_smart(
    auth: Request,
    file: UploadFile = File(...),
) -> OCRResponse:
    """Smart ID card processing using multiple techniques"""
    try:
        username = jwt_service.get_username_from_request(auth)
        logger.info(f"Processing smart OCR request for user: {username}")

        with tempfile.NamedTemporaryFile(delete=False) as temp_file:
            temp_file.write(await file.read())
            temp_path = temp_file.name

        try:
            # Sử dụng trích xuất thông minh
            start_time = time.time()
            
            # Thử Vintern model nếu có GPU
            try:
                if torch.cuda.is_available():
                    logger.info("GPU available, trying Vintern model")
                    model = VinternModel()
                    data = model.extract_info(temp_path)
                    processing_time = time.time() - start_time
                    
                    # Kiểm tra kết quả có đủ tốt không
                    if data.get("id_number") and data.get("name"):
                        return OCRResponse(
                            success=True,
                            data=data,
                            processing_time=processing_time
                        )
            except Exception as e:
                logger.warning(f"Vintern model failed: {e}, falling back to lightweight")
            
            # Fallback to lightweight extractor
            logger.info("Using lightweight extractor")
            lightweight_extractor = LightweightIDExtractor()
            
            # Thử các phương pháp từ đơn giản đến phức tạp
            # 1. Phương pháp cơ bản
            data = lightweight_extractor.extract(temp_path)
            
            # 2. Nếu thiếu thông tin quan trọng, thử phương pháp nâng cao
            if not data.get("id_number") or not data.get("name"):
                logger.info("Basic extraction incomplete, trying advanced processing")
                data = lightweight_extractor.enhance_with_advanced_processing(temp_path)
            
            # 3. Nếu vẫn thiếu, thử phương pháp vùng
            if not data.get("id_number") or not data.get("name"):
                logger.info("Advanced processing incomplete, trying region-based extraction")
                data = lightweight_extractor.extract_with_regions(temp_path)
            
            processing_time = time.time() - start_time
            
            return OCRResponse(
                success=True,
                data=data,
                processing_time=processing_time
            )
        finally:
            os.unlink(temp_path)
            
    except Exception as e:
        logger.error(f"Error processing file with smart method: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    
@router.post("/ocr/qr", response_model=OCRResponse)
async def process_id_card_qr(
    auth: Request,
    file: UploadFile = File(...),
) -> OCRResponse:
    """Extract information from ID card using QR code"""
    try:
        username = jwt_service.get_username_from_request(auth)
        logger.info(f"Processing QR code OCR request for user: {username}")

        with tempfile.NamedTemporaryFile(delete=False) as temp_file:
            temp_file.write(await file.read())
            temp_path = temp_file.name

        try:
            # Sử dụng QR code extractor
            start_time = time.time()
            qr_extractor = QRIDExtractor()
            
            # Thử phương pháp nâng cao trước
            data = qr_extractor.extract(temp_path)
            
            # Nếu không tìm thấy QR code, thử phương pháp nâng cao
            if not data.get("id_number") and not data.get("name"):
                logger.info("Basic QR extraction failed, trying enhanced method")
                qr_data = qr_extractor.enhance_qr_detection(temp_path)
                if qr_data:
                    data = qr_extractor.extract_info_from_qr_data(qr_data)
            
            processing_time = time.time() - start_time
            
            return OCRResponse(
                success=True,
                data=data,
                processing_time=processing_time,
                error=data.get("error")
            )
        finally:
            os.unlink(temp_path)
            
    except Exception as e:
        logger.error(f"Error processing file with QR method: {e}")
        raise HTTPException(status_code=500, detail=str(e))