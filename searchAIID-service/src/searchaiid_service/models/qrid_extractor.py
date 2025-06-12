import cv2
import numpy as np
import pyzbar.pyzbar as pyzbar
import logging
from typing import Dict, Any, Optional, List, Tuple
import os
import re

logger = logging.getLogger(__name__)

class QRIDExtractor:
    """ID card information extractor using QR code"""
    
    def __init__(self):
        """Initialize QR code extractor"""
        # Các pattern để parse dữ liệu từ QR code của CCCD
        self.field_patterns = {
            "id_number": r"\|(\d{12})\|",  # Số CCCD (12 chữ số)
            "name": r"^([^|]+)\|",         # Họ tên (phần đầu tiên trước dấu |)
            "dob": r"\|(\d{2}/\d{2}/\d{4})\|",  # Ngày sinh (DD/MM/YYYY)
            "gender": r"\|(Nam|Nữ)\|",     # Giới tính
            "address": r"\|([^|]+?)(?:\||$)",  # Địa chỉ (thường ở cuối)
            # Các pattern khác có thể được thêm vào dựa trên cấu trúc thực tế của QR code
        }
    
    def preprocess_image(self, image_path: str) -> np.ndarray:
        """Xử lý ảnh để tăng cường khả năng phát hiện QR code"""
        # Đọc ảnh
        image = cv2.imread(image_path)
        if image is None:
            raise ValueError(f"Could not read image from {image_path}")
        
        # Chuyển sang grayscale
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # Áp dụng GaussianBlur để giảm nhiễu
        blurred = cv2.GaussianBlur(gray, (5, 5), 0)
        
        # Sử dụng Adaptive Threshold để xử lý ảnh có điều kiện ánh sáng không đồng đều
        thresh = cv2.adaptiveThreshold(
            blurred, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
            cv2.THRESH_BINARY, 11, 2
        )
        
        # Thêm các bước tiền xử lý đặc biệt cho QR code
        # Làm rõ biên
        kernel = np.ones((3, 3), np.uint8)
        morph = cv2.morphologyEx(thresh, cv2.MORPH_CLOSE, kernel)
        
        return morph
    
    def detect_qr_codes(self, image: np.ndarray) -> List[Any]:
        """Phát hiện tất cả các QR codes trong ảnh"""
        # Sử dụng pyzbar để phát hiện QR codes
        return pyzbar.decode(image)
    
    def extract_largest_qr_code(self, image_path: str) -> Optional[str]:
        """Trích xuất QR code lớn nhất từ ảnh"""
        # Đọc ảnh gốc
        image = cv2.imread(image_path)
        if image is None:
            raise ValueError(f"Could not read image from {image_path}")
        
        # Thử phát hiện QR code trên ảnh gốc trước
        qr_codes = self.detect_qr_codes(image)
        
        # Nếu không tìm thấy, thử với ảnh đã tiền xử lý
        if not qr_codes:
            processed_image = self.preprocess_image(image_path)
            qr_codes = self.detect_qr_codes(processed_image)
        
        # Nếu vẫn không tìm thấy, thử với các biến thể xử lý khác
        if not qr_codes:
            # Thử nghiệm các kích thước ảnh khác nhau
            resized_image = cv2.resize(image, (0, 0), fx=1.5, fy=1.5)
            qr_codes = self.detect_qr_codes(resized_image)
            
            if not qr_codes:
                resized_image = cv2.resize(image, (0, 0), fx=0.7, fy=0.7)
                qr_codes = self.detect_qr_codes(resized_image)
        
        # Nếu vẫn không tìm thấy, thử cách khác: nghiêng ảnh
        if not qr_codes:
            rows, cols = image.shape[:2]
            M = cv2.getRotationMatrix2D((cols/2, rows/2), 90, 1)
            rotated = cv2.warpAffine(image, M, (cols, rows))
            qr_codes = self.detect_qr_codes(rotated)
        
        # Nếu tìm thấy QR codes, lấy cái lớn nhất (có khả năng cao nhất là của CCCD)
        if qr_codes:
            # Sắp xếp theo kích thước (diện tích) của QR code
            largest_qr = max(qr_codes, key=lambda qr: qr.rect.width * qr.rect.height)
            # Giải mã dữ liệu từ QR
            qr_data = largest_qr.data.decode('utf-8', errors='ignore')
            return qr_data
        
        return None
    
    def extract_info_from_qr_data(self, qr_data: str) -> Dict[str, Any]:
        """Trích xuất thông tin từ dữ liệu QR code"""
        logger.info(f"Extracted QR data: {qr_data}")
        
        # Khởi tạo dict kết quả
        result = {
            "name": "",
            "dob": "",
            "gender": "",
            "nationality": "Việt Nam",  # Mặc định là Việt Nam
            "country": "",
            "address": "",
            "id_number": "",
            "expiry_date": "",
            "card_type": "CCCD"  # Mặc định là CCCD
        }
        
        # Phân tích dữ liệu QR code dựa trên cấu trúc đã biết
        # Cấu trúc thông thường: Họ tên|Ngày sinh|Giới tính|CCCD|Địa chỉ
        try:
            # Tách các phần dữ liệu
            if "|" in qr_data:
                parts = qr_data.split("|")
                
                # Ánh xạ các phần vào kết quả
                if len(parts) >= 1 and parts[0]:
                    result["name"] = parts[0].strip().upper()
                
                # Lấy các phần khác tùy thuộc vào số lượng phần
                if len(parts) >= 2 and parts[1]:
                    # Kiểm tra xem phần này có phải định dạng ngày không
                    if re.match(r"\d{2}/\d{2}/\d{4}", parts[1]):
                        result["dob"] = parts[1].strip()
                
                if len(parts) >= 3 and parts[2]:
                    result["gender"] = parts[2].strip()
                
                if len(parts) >= 4 and parts[3]:
                    # Nếu là chuỗi số, có thể là CCCD
                    if parts[3].strip().isdigit() and len(parts[3].strip()) == 12:
                        result["id_number"] = parts[3].strip()
                
                if len(parts) >= 5 and parts[4]:
                    result["address"] = parts[4].strip()
            else:
                # Nếu không có dấu phân cách "|", thử dùng regex để tìm thông tin
                for field, pattern in self.field_patterns.items():
                    match = re.search(pattern, qr_data)
                    if match:
                        result[field] = match.group(1).strip()
        
        except Exception as e:
            logger.error(f"Error parsing QR data: {str(e)}")
        
        return result
    
    def extract(self, image_path: str) -> Dict[str, Any]:
        """Trích xuất thông tin từ ảnh CCCD sử dụng QR code"""
        try:
            # Trích xuất dữ liệu QR code
            qr_data = self.extract_largest_qr_code(image_path)
            
            if qr_data:
                # Phân tích dữ liệu QR để lấy thông tin
                extracted_info = self.extract_info_from_qr_data(qr_data)
                logger.info(f"Successfully extracted info from QR code: {extracted_info.keys()}")
                return extracted_info
            else:
                logger.warning("No QR code detected in the image")
                # Trả về kết quả trống
                return {
                    "name": "",
                    "dob": "",
                    "gender": "",
                    "nationality": "Việt Nam",
                    "country": "",
                    "address": "",
                    "id_number": "",
                    "expiry_date": "",
                    "card_type": "Unknown",
                    "error": "No QR code detected"
                }
                
        except Exception as e:
            logger.error(f"Error extracting QR code: {str(e)}")
            return {
                "name": "",
                "dob": "",
                "gender": "",
                "nationality": "",
                "country": "",
                "address": "",
                "id_number": "",
                "expiry_date": "",
                "error": str(e)
            }
    
    def enhance_qr_detection(self, image_path: str) -> Optional[str]:
        """Nâng cao khả năng phát hiện QR code với nhiều phương pháp xử lý ảnh"""
        # Đọc ảnh gốc
        original_image = cv2.imread(image_path)
        
        # Danh sách các phương pháp xử lý ảnh sẽ thử
        processing_methods = [
            # Phương pháp 1: Ảnh gốc
            lambda img: img,
            
            # Phương pháp 2: Tăng độ sáng
            lambda img: cv2.convertScaleAbs(img, alpha=1.5, beta=30),
            
            # Phương pháp 3: Tăng độ tương phản
            lambda img: cv2.convertScaleAbs(img, alpha=2.0, beta=0),
            
            # Phương pháp 4: Adaptive threshold
            lambda img: cv2.adaptiveThreshold(
                cv2.cvtColor(img, cv2.COLOR_BGR2GRAY), 255, 
                cv2.ADAPTIVE_THRESH_GAUSSIAN_C, cv2.THRESH_BINARY, 11, 2
            ),
            
            # Phương pháp 5: Canny edge detection
            lambda img: cv2.Canny(cv2.cvtColor(img, cv2.COLOR_BGR2GRAY), 100, 200),
            
            # Phương pháp 6: Morphological closing
            lambda img: cv2.morphologyEx(
                cv2.cvtColor(img, cv2.COLOR_BGR2GRAY), 
                cv2.MORPH_CLOSE, 
                np.ones((5, 5), np.uint8)
            )
        ]
        
        # Kích thước khác nhau để thử
        scales = [1.0, 1.5, 0.7, 2.0]
        
        # Góc xoay để thử
        angles = [0, 90, 180, 270]
        
        # Thử từng phương pháp xử lý ảnh
        for method in processing_methods:
            try:
                processed = method(original_image)
                
                # Đảm bảo ảnh là grayscale hoặc color
                if len(processed.shape) == 2:
                    # Nếu là grayscale, chuyển thành BGR
                    processed_color = cv2.cvtColor(processed, cv2.COLOR_GRAY2BGR)
                else:
                    processed_color = processed
                
                # Thử phát hiện QR trên ảnh đã xử lý
                qr_codes = self.detect_qr_codes(processed_color)
                if qr_codes:
                    largest_qr = max(qr_codes, key=lambda qr: qr.rect.width * qr.rect.height)
                    return largest_qr.data.decode('utf-8', errors='ignore')
                
                # Thử với các kích thước khác nhau
                for scale in scales:
                    if scale != 1.0:
                        resized = cv2.resize(processed_color, (0, 0), fx=scale, fy=scale)
                        qr_codes = self.detect_qr_codes(resized)
                        if qr_codes:
                            largest_qr = max(qr_codes, key=lambda qr: qr.rect.width * qr.rect.height)
                            return largest_qr.data.decode('utf-8', errors='ignore')
                
                # Thử với các góc xoay khác nhau
                for angle in angles:
                    if angle != 0:
                        rows, cols = processed_color.shape[:2]
                        M = cv2.getRotationMatrix2D((cols/2, rows/2), angle, 1)
                        rotated = cv2.warpAffine(processed_color, M, (cols, rows))
                        qr_codes = self.detect_qr_codes(rotated)
                        if qr_codes:
                            largest_qr = max(qr_codes, key=lambda qr: qr.rect.width * qr.rect.height)
                            return largest_qr.data.decode('utf-8', errors='ignore')
                            
            except Exception as e:
                logger.warning(f"Error with processing method: {str(e)}")
                continue
        
        return None