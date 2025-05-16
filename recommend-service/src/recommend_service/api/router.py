import os
import tempfile
from fastapi import APIRouter, UploadFile, File, HTTPException, Request
from recommend_service.api.models import OCRResponse, OCRRequest
from recommend_service.core.auth.jwt_service import jwt_service
import logging
import uuid

logger = logging.getLogger(__name__)
router = APIRouter()