import os
import tempfile
from fastapi import APIRouter, UploadFile, File, HTTPException, Request
from searchaiid_service.api.models import OCRResponse, OCRRequest
from searchaiid_service.core.auth.jwt_service import jwt_service
from searchaiid_service.models.vintern import VinternModel
from searchaiid_service.core.kafka.kafka_service import kafka_service
import logging
import torch
import uuid

logger = logging.getLogger(__name__)
router = APIRouter()

@router.post("/ocr", response_model=OCRResponse)
async def process_id_card(
    auth: Request,
    file: UploadFile = File(...),
    request: OCRRequest = None
) -> OCRResponse:
    try:
        username = jwt_service.get_username_from_request(auth)
        print(f"Username: {username}")

        # with tempfile.NamedTemporaryFile(delete=False) as temp_file:
        #     temp_file.write(await file.read())
        #     temp_path = temp_file.name

        # try:
        #     model = VinternModel()
        #     prompt = request.prompt if request and request.prompt else None

        #     try:
        #         data = model.extract_info(temp_path, prompt)
        #     except RuntimeError as e:
        #         if "Input type (float) and bias type (struct c10::BFloat16)" in str(e):
        #             logger.warning("Trying to convert model to float32 and retry...")
        #             model.model = model.model.to(torch.float32)
        #             data = model.extract_info(temp_path, prompt)
        #         else:
        #             raise
        #     return OCRResponse(
        #         success=True,
        #         data=data
        #     )
        # finally:
        #     os.unlink(temp_path)

        message = {
            "username": username,
        }

        kafka_service.produce_message(
            topic="ocr_test",
            key=str(uuid.uuid4()),
            value=message,
        )

        return OCRResponse(
            success=True,
            data={}
        )

    except Exception as e:
        logger.error(f"Error processing file: {e}")
        raise HTTPException(status_code=500, detail=str(e)
)