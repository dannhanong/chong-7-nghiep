import cv2
import numpy as np
import re
import pytesseract
from PIL import Image
import logging
from typing import Dict, Any, Optional, List, Tuple
import os

logger = logging.getLogger(__name__)

class LightweightIDExtractor:
    """Lightweight ID card information extractor using traditional CV and OCR"""
    
    def __init__(self):
        # Thiết lập Tesseract OCR cho tiếng Việt
        self.lang = "vie"  # Ngôn ngữ tiếng Việt
        # Kiểm tra xem có sẵn dữ liệu tiếng Việt chưa
        try:
            test_text = pytesseract.image_to_string(np.zeros((10, 10), dtype=np.uint8), lang=self.lang)
            logger.info("Vietnamese language data available for Tesseract")
        except Exception as e:
            logger.warning(f"Vietnamese language data not available: {e}. Falling back to English")
            self.lang = "eng"  # Fallback to English
            
        # Các mẫu regex cho thông tin CCCD
        self.patterns = {
            "id_number": r"(?<!\d)(\d{9}|\d{12})(?!\d)",  # 9 hoặc 12 chữ số
            "name": r"(?:Họ và tên|Họ tên)[:\s]+([A-ZĐÀÁẢÃẠÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴ\s]+)",
            "dob": r"(?:Ngày sinh|Sinh ngày)[:\s]+((?:0[1-9]|[12][0-9]|3[01])[\/\-\.]((?:0[1-9]|1[0-2])[\/\-\.](19|20)\d{2}))",
            "gender": r"(?:Giới tính|Nam/Nữ)[:\s]+([Nam|Nữ|KHÁC]+)",
            "nationality": r"(?:Quốc tịch)[:\s]+([A-ZĐÀÁẢÃẠÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴ\s]+)",
            "country": r"(?:Quê quán|Nguyên quán)[:\s]+([A-ZĐÀÁẢÃẠÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴa-zđàáảãạèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵ\s,]+)",
            "address": r"(?:Nơi thường trú|Thường trú|Địa chỉ)[:\s]+([A-ZĐÀÁẢÃẠÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴa-zđàáảãạèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵ\s,0-9\.]+)",
            "expiry_date": r"(?:Có giá trị đến|Giá trị đến|Ngày hết hạn)[:\s]+((?:0[1-9]|[12][0-9]|3[01])[\/\-\.]((?:0[1-9]|1[0-2])[\/\-\.](19|20)\d{2}))",
        }

    def preprocess_image(self, image_path: str) -> np.ndarray:
        """Xử lý ảnh để tăng cường khả năng OCR"""
        # Đọc ảnh
        image = cv2.imread(image_path)
        if image is None:
            raise ValueError(f"Could not read image from {image_path}")
        
        # Chuyển sang grayscale
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # Loại bỏ nhiễu với Gaussian blur
        blurred = cv2.GaussianBlur(gray, (5, 5), 0)
        
        # Áp dụng threshold để làm nổi bật văn bản
        _, thresh = cv2.threshold(blurred, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        
        # Kiểm tra màu chủ đạo, nếu màu sáng chiếm ưu thế, đảo ngược ảnh
        if np.mean(thresh) > 127:
            thresh = 255 - thresh
            
        # Morphological operations để loại bỏ nhiễu nhỏ
        kernel = np.ones((2, 2), np.uint8)
        opening = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, kernel)
        
        return opening
    
    def extract_text_from_image(self, processed_image: np.ndarray) -> str:
        """Trích xuất văn bản từ ảnh đã xử lý"""
        # Sử dụng Tesseract OCR
        config = f'--oem 3 --psm 6 -l {self.lang}'
        text = pytesseract.image_to_string(processed_image, config=config)
        
        # Xử lý text sau OCR
        text = text.replace('\n\n', '\n').strip()
        
        return text
    
    def find_field_by_pattern(self, text: str, pattern: str) -> Optional[str]:
        """Tìm kiếm thông tin theo mẫu regex"""
        match = re.search(pattern, text, re.IGNORECASE)
        if match and len(match.groups()) > 0:
            return match.group(1).strip()
        return None
    
    def extract_card_type(self, text: str) -> str:
        """Xác định loại thẻ: CCCD hoặc CMND"""
        if "căn cước" in text.lower() or "cccd" in text.lower():
            return "CCCD"
        elif "chứng minh" in text.lower() or "cmnd" in text.lower():
            return "CMND"
        # Dựa vào số digits trong ID number
        id_match = re.search(self.patterns["id_number"], text)
        if id_match:
            id_number = id_match.group(0)
            if len(id_number) == 12:
                return "CCCD"
            elif len(id_number) == 9:
                return "CMND"
        return "Unknown"
    
    def extract_segments(self, image_path: str) -> List[np.ndarray]:
        """Chia ảnh thành các phân đoạn để OCR hiệu quả hơn"""
        image = cv2.imread(image_path)
        height, width, _ = image.shape
        
        # Chia ảnh thành 4 phần
        segments = []
        # Phần trên bên trái (thường chứa logo, tiêu đề)
        segments.append(image[0:height//2, 0:width//2])
        # Phần trên bên phải (thường chứa ảnh, số ID)
        segments.append(image[0:height//2, width//2:width])
        # Phần dưới bên trái (thường chứa thông tin cá nhân)
        segments.append(image[height//2:height, 0:width//2])
        # Phần dưới bên phải (thường chứa địa chỉ, ngày hết hạn)
        segments.append(image[height//2:height, width//2:width])
        
        return segments
    
    def validate_and_correct(self, data: Dict[str, Any]) -> Dict[str, Any]:
        """Xác thực và sửa chữa dữ liệu đã trích xuất"""
        # ID number validation
        if "id_number" in data and data["id_number"]:
            # Loại bỏ ký tự không phải số
            data["id_number"] = re.sub(r'\D', '', data["id_number"])
            # Kiểm tra độ dài
            if len(data["id_number"]) not in [9, 12]:
                logger.warning(f"Invalid ID number length: {len(data['id_number'])}")
                # Có thể là lỗi OCR, thử sửa số dễ nhầm lẫn
                data["id_number"] = data["id_number"].replace('l', '1').replace('O', '0').replace('o', '0')
        
        # Name validation - convert to uppercase
        if "name" in data and data["name"]:
            data["name"] = data["name"].upper()
        
        # Date validation (DOB and expiry_date)
        for date_field in ["dob", "expiry_date"]:
            if date_field in data and data[date_field]:
                # Kiểm tra định dạng ngày
                date_pattern = r'(\d{1,2})[\/\-\.](\d{1,2})[\/\-\.](\d{4})'
                match = re.match(date_pattern, data[date_field])
                if match:
                    day, month, year = match.groups()
                    # Chuẩn hóa về định dạng DD/MM/YYYY
                    data[date_field] = f"{int(day):02d}/{int(month):02d}/{year}"
        
        return data
    
    def extract(self, image_path: str) -> Dict[str, Any]:
        """Extract information from ID card image"""
        try:
            # Trích xuất từ toàn bộ ảnh
            processed_image = self.preprocess_image(image_path)
            full_text = self.extract_text_from_image(processed_image)
            
            # Trích xuất từ các phân đoạn
            segments = self.extract_segments(image_path)
            segment_texts = []
            for segment in segments:
                processed_segment = self.preprocess_image(Image.fromarray(segment))
                segment_text = self.extract_text_from_image(processed_segment)
                segment_texts.append(segment_text)
            
            # Kết hợp tất cả văn bản
            combined_text = full_text + "\n" + "\n".join(segment_texts)
            
            # Xác định loại thẻ
            card_type = self.extract_card_type(combined_text)
            
            # Trích xuất thông tin từ văn bản
            extracted_data = {
                "card_type": card_type
            }
            
            # Trích xuất từng trường thông tin
            for field, pattern in self.patterns.items():
                value = self.find_field_by_pattern(combined_text, pattern)
                if value:
                    extracted_data[field] = value
            
            # Xác thực và sửa chữa dữ liệu
            validated_data = self.validate_and_correct(extracted_data)
            
            # Đảm bảo định dạng đầu ra nhất quán
            result = {
                "name": validated_data.get("name", ""),
                "dob": validated_data.get("dob", ""),
                "gender": validated_data.get("gender", ""),
                "nationality": validated_data.get("nationality", ""),
                "country": validated_data.get("country", ""),
                "address": validated_data.get("address", ""),
                "id_number": validated_data.get("id_number", ""),
                "expiry_date": validated_data.get("expiry_date", ""),
                "card_type": validated_data.get("card_type", "Unknown")
            }
            
            return result
            
        except Exception as e:
            logger.error(f"Error extracting information: {str(e)}")
            # Trả về dữ liệu trống khi có lỗi
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
        
    # Thêm vào class LightweightIDExtractor
    def enhance_with_advanced_processing(self, image_path: str) -> Dict[str, Any]:
        """Nâng cao khả năng trích xuất với xử lý hình ảnh sâu hơn"""
        # Xử lý thông thường
        basic_result = self.extract(image_path)
        
        # Nếu kết quả cơ bản không đủ tốt, thử các phương pháp nâng cao
        if not basic_result["id_number"] or not basic_result["name"]:
            try:
                # 1. Áp dụng cải thiện độ tương phản thích ứng
                image = cv2.imread(image_path)
                lab = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)
                l, a, b = cv2.split(lab)
                clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8))
                cl = clahe.apply(l)
                limg = cv2.merge((cl, a, b))
                enhanced = cv2.cvtColor(limg, cv2.COLOR_LAB2BGR)
                
                # Lưu tạm ảnh đã cải thiện
                enhanced_path = image_path + "_enhanced.jpg"
                cv2.imwrite(enhanced_path, enhanced)
                
                # Thử trích xuất lại
                enhanced_result = self.extract(enhanced_path)
                
                # Xóa file tạm
                os.remove(enhanced_path)
                
                # Kết hợp kết quả
                combined_result = {}
                for key in basic_result:
                    # Ưu tiên kết quả có giá trị
                    if enhanced_result.get(key) and not basic_result.get(key):
                        combined_result[key] = enhanced_result[key]
                    else:
                        combined_result[key] = basic_result[key]
                        
                return combined_result
                
            except Exception as e:
                logger.error(f"Advanced processing failed: {str(e)}")
                return basic_result
        
        return basic_result
    
    # Thêm vào class LightweightIDExtractor
    def detect_regions_of_interest(self, image_path: str) -> Dict[str, np.ndarray]:
        """Phát hiện các vùng quan tâm trên CCCD dựa trên mẫu"""
        image = cv2.imread(image_path)
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        height, width = gray.shape
        
        # Xác định các vùng quan tâm dựa trên tỷ lệ cố định
        regions = {
            "header": gray[0:height//6, 0:width],                  # Phần đầu (tiêu đề)
            "photo": gray[height//6:height//2, width*2//3:width],  # Vùng ảnh
            "id_number": gray[height//6:height//3, 0:width*2//3],  # Vùng số ID
            "name": gray[height//3:height//2, 0:width*2//3],       # Vùng tên
            "personal_info": gray[height//2:height*3//4, 0:width], # Thông tin cá nhân
            "address": gray[height*3//4:height, 0:width]           # Địa chỉ
        }
        
        return regions

    def extract_with_regions(self, image_path: str) -> Dict[str, Any]:
        """Trích xuất thông tin theo từng vùng cụ thể"""
        regions = self.detect_regions_of_interest(image_path)
        result = {}
        
        # Xử lý từng vùng
        for region_name, region_image in regions.items():
            # Lưu ảnh vùng tạm thời
            temp_path = f"{image_path}_{region_name}.jpg"
            cv2.imwrite(temp_path, region_image)
            
            # Trích xuất văn bản từ vùng
            processed = self.preprocess_image(temp_path)
            text = self.extract_text_from_image(processed)
            
            # Tìm thông tin phù hợp dựa trên loại vùng
            if region_name == "id_number":
                id_match = re.search(self.patterns["id_number"], text)
                if id_match:
                    result["id_number"] = id_match.group(0)
            elif region_name == "name":
                name_match = re.search(self.patterns["name"], text)
                if name_match:
                    result["name"] = name_match.group(1)
            # Tương tự cho các vùng khác
            
            # Xóa file tạm
            os.remove(temp_path)
        
        # Điền các thông tin còn thiếu từ phương pháp cơ bản
        basic_result = self.extract(image_path)
        for key in basic_result:
            if key not in result or not result[key]:
                result[key] = basic_result[key]
        
        return result