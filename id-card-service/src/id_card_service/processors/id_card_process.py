import logging
from typing import Optional, Tuple
import numpy as np
import time
import requests
import cv2
from id_card_service.config.settings import settings
from id_card_service.core.face_detector import FaceDetector

logger = logging.getLogger(__name__)

class IDCardProcessor:
    def __init__(self):
        self.base_url = settings.BASE_API_URL + "/files/preview"
        self.timeout = 10
        self.cache = {}
        self.cache_expiry = 3600  # 1 hour cache
        self.cache_hits = 0
        self.cache_misses = 0

    def fetch_id_card_image(self, image_code: str) -> Optional[np.ndarray]:
        """
        Tải ảnh căn cước từ API
        
        Args:
            image_code: Mã của ảnh từ service CCCD

        Returns:
            np.ndarray: Ảnh dưới dạng OpenCV array hoặc None nếu thất bại
        """
        try:
            # Kiểm tra cache trước
            if image_code in self.cache:
                cached_time, cached_image = self.cache[image_code]
                if time.time() - cached_time < self.cache_expiry:
                    self.cache_hits += 1
                    logger.info(f"Using cached image for code: {image_code}")
                    return cached_image
                
            self.cache_misses += 1
            
            # Tạo URL đầy đủ hoặc sử dụng trực tiếp nếu đã là URL
            if image_code.startswith(('http://', 'https://')):
                url = image_code
            else:
                url = f"{self.base_url}/{image_code}"

            logger.info(f"Fetching ID card image from: {url}")
            
            # Gửi request
            response = requests.get(url, timeout=self.timeout)
            
            if response.status_code == 200:
                # Convert bytes to numpy array
                image_array = np.asarray(bytearray(response.content), dtype=np.uint8)
                image = cv2.imdecode(image_array, cv2.IMREAD_COLOR)
                
                if image is not None:
                    # Lưu vào cache
                    self.cache[image_code] = (time.time(), image.copy())
                    logger.info(f"Successfully fetched and cached image: {image_code}")
                    return image
                else:
                    logger.error(f"Failed to decode image from response: {image_code}")
                    return None
            else:
                logger.error(f"HTTP Error {response.status_code} when fetching image: {image_code}")
                return None
                
        except requests.exceptions.Timeout:
            logger.error(f"Timeout when fetching image: {image_code}")
            return None
        except requests.exceptions.ConnectionError:
            logger.error(f"Connection error when fetching image: {image_code}")
            return None
        except Exception as e:
            logger.error(f"Unexpected error fetching image {image_code}: {str(e)}")
            return None
        
    def enhance_id_card_face(self, image: np.ndarray) -> np.ndarray:
        """
        Cải thiện chất lượng ảnh gương mặt từ CCCD
        
        Args:
            image: Ảnh gương mặt cần cải thiện
            
        Returns:
            np.ndarray: Ảnh đã được cải thiện
        """
        try:
            # Normalize lighting using CLAHE
            lab = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)
            l, a, b = cv2.split(lab)
            
            # Apply CLAHE to L channel
            clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
            l = clahe.apply(l)
            
            # Merge channels back
            enhanced = cv2.merge([l, a, b])
            enhanced = cv2.cvtColor(enhanced, cv2.COLOR_LAB2BGR)
            
            # Apply sharpening filter
            kernel = np.array([[-1, -1, -1], 
                             [-1,  9, -1], 
                             [-1, -1, -1]])
            sharpened = cv2.filter2D(enhanced, -1, kernel)
            
            # Denoise
            denoised = cv2.bilateralFilter(sharpened, 9, 75, 75)
            
            logger.debug("Enhanced ID card face image")
            return denoised
            
        except Exception as e:
            logger.error(f"Error enhancing ID card face: {str(e)}")
            return image  # Return original if enhancement fails
        
    def extract_face_from_id_card(
            self, image: np.ndarray
        ) -> Optional[Tuple[np.ndarray, Tuple[int, int, int, int]]]:
        """
        Trích xuất gương mặt từ ảnh căn cước
        
        Args:
            image: Ảnh căn cước đầy đủ
            
        Returns:
            Tuple[np.ndarray, Tuple]: (Ảnh gương mặt, tọa độ gương mặt) hoặc None
        """
        try:
            # Import detector (tránh circular import)
            import sys
            import os
            sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
                        
            # Sử dụng detector với cấu hình tối ưu cho ảnh CCCD
            detector = FaceDetector(scale_factor=1.0, detection_method="haar")
            face_locations = detector.detect_faces(image)
            
            if not face_locations:
                logger.warning("No face detected in ID card image")
                return None
            
            # Lấy gương mặt lớn nhất (thường là gương mặt chính trong CCCD)
            largest_face = max(face_locations, key=lambda loc: (loc[2] - loc[0]) * (loc[1] - loc[3]))
            top, right, bottom, left = largest_face
            
            # Trích xuất vùng gương mặt
            face_image = image[top:bottom, left:right]
            
            if face_image.size == 0:
                logger.warning("Extracted face region is empty")
                return None
            
            # Resize về kích thước chuẩn
            face_image = cv2.resize(face_image, (160, 160))
            
            # Cải thiện chất lượng
            enhanced_face = self.enhance_id_card_face(face_image)
            
            logger.info(f"Successfully extracted face from ID card, size: {enhanced_face.shape}")
            return enhanced_face, largest_face
            
        except ImportError as e:
            logger.error(f"Import error: {str(e)}")
            return None
        except Exception as e:
            logger.error(f"Error extracting face from ID card: {str(e)}")
            return None