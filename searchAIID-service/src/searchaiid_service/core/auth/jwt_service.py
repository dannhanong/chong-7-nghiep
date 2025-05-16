import jwt
from datetime import datetime, timedelta
from fastapi import Depends, HTTPException, status, Request, Header
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from typing import Optional, Dict, Union
from searchaiid_service.config.settings import settings
import logging
import base64

logger = logging.getLogger(__name__)
security = HTTPBearer()

class JWTService:
    def __init__(self):
        self.secret = settings.JWT_SECRET
        self.algorithm = settings.JWT_ALGORITHM
        self.expiration = settings.JWT_EXPIRATION_MINUTES

    def create_token(self, username: str, additional_data: Dict = None) -> str:
        """Tạo JWT mới"""
        payload = {
            "sub": username,
            "iat": datetime.utcnow(),
            "exp": datetime.utcnow() + timedelta(minutes=self.expiration)
        }

        if additional_data:
            payload.update(additional_data)

        return jwt.encode(payload, self.secret, algorithm=self.algorithm)
    
    def extract_username(self, token: str) -> Optional[str]:
        """Giải mã JWT và lấy tên người dùng"""
        try:
            secret_bytes = base64.b64decode(self.secret)
            payload = jwt.decode(
                token, 
                secret_bytes, 
                algorithms=[self.algorithm]
            )
            return payload.get("sub")
        except jwt.ExpiredSignatureError:
            logger.warning("Token đã hết hạn")
            return None
        except jwt.InvalidTokenError as e:
            logger.warning(f"Token không hợp lệ: {e}")
            return None
        except Exception as e:
            logger.error(f"Lỗi không xác định khi decode token: {e}")
            return None
        
    def get_claims_from_token(self, token: str) -> Dict:
        """Giải mã JWT và lấy tất cả các claims"""
        try:
            payload = jwt.decode(
                token, 
                self.secret, 
                algorithms=[self.algorithm]
            )
            return payload
        except jwt.ExpiredSignatureError:
            logger.warning("Token đã hết hạn")
            return {}
        except jwt.InvalidTokenError as e:
            logger.warning(f"Token không hợp lệ: {e}")
            return {}
        except Exception as e:
            logger.error(f"Lỗi không xác định khi decode token: {e}")
            return {}
        
    def get_username_from_request(self, request: Request) -> Optional[str]:
        """Lấy username từ yêu cầu"""
        try:
            auth_header = request.headers.get("Authorization")
            if not auth_header:
                logger.debug("Không có header Authorization")
                return None
                
            if not auth_header.startswith("Bearer "):
                logger.debug("Header Authorization không đúng định dạng Bearer")
                return None
                
            token = auth_header[7:]
            return self.extract_username(token)
        except Exception as e:
            logger.error(f"Lỗi khi lấy username từ request: {e}")
            return None
    
    def get_token_from_authorization(self, authorization: str = Header(None)) -> Optional[str]:
        """Lấy token từ header Authorization"""
        if authorization and authorization.startswith("Bearer "):
            return authorization[7:]
        return None
    
    def validate_token(self, token: str) -> bool:
        """Kiểm tra token có hợp lệ không"""
        try:
            jwt.decode(
                token, 
                self.secret, 
                algorithms=[self.algorithm]
            )
            return True
        except:
            return False
    
jwt_service = JWTService()