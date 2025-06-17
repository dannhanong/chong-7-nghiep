"""OCR Engine module using PaddleOCR for Vietnamese text recognition."""

import logging
import time
import numpy as np
from paddleocr import PaddleOCR
import cv2
from id_card_service.config.settings import settings
from openai import OpenAI
import json

logger = logging.getLogger(__name__)

class OCREngine:
    """PaddleOCR wrapper optimized for Vietnamese CCCD text extraction."""
    def __init__(
        self,
        lang: str = settings.OCR_LANG,
        use_angle_cls: bool = True
    ):
        """Initialize OCR Engine."""
        self.lang = lang
        self.use_angle_cls = use_angle_cls
        self._ocr_engine = None
        self._model_loaded = False
        self.openai_client = OpenAI(api_key=settings.OPENAI_API_KEY)
        self.model="gpt-4.1-nano"
        
        logger.info(f"Initializing OCR Engine with Lang: {self.lang}")
    
    def _load_model(self) -> None:
        """Lazy load OCR model to optimize startup time."""
        if not self._model_loaded:
            try:
                logger.info(f"Loading PaddleOCR model with lang={self.lang}...")
                
                # Khởi tạo PaddleOCR với các tham số tối thiểu
                self._ocr_engine = PaddleOCR(
                    lang=self.lang,
                    use_angle_cls=self.use_angle_cls
                )

                self._model_loaded = True
                logger.info("OCR model loaded successfully")
                
            except Exception as e:
                logger.error(f"Failed to load OCR model: {str(e)}")
                raise
    
    def _validate_id_number(self, number: str) -> bool:
        """Validate CCCD number format."""
        if not number.isdigit() or len(number) != 12:
            return False
            
        # Kiểm tra prefix hợp lệ (đầu số tỉnh/thành phố)
        valid_prefixes = [
            '001', '002', '004', '006', '008', 
            '010', '011', '012', '014', '015',
            '017', '019', '020', '022', '024',
            '025', '026', '027', '030', '031',
            '033', '034', '035', '036', '037',
            '038', '040', '042', '044', '045',
            '046', '048', '049', '051', '052',
            '054', '056', '058', '060', '062',
            '064', '066', '067', '068', '070',
            '072', '074', '075', '077', '079',
            '080', '082', '083', '084', '086',
            '087', '089', '091', '092', '093',
            '094', '095', '096'
        ]
        
        prefix = number[:3]
        return prefix in valid_prefixes

    def _extract_id_number(self, texts: list, scores: list) -> tuple:
        """Extract ID number from OCR results with enhanced pattern matching."""
        import re

        # Pattern để tìm số CCCD trong văn bản
        cccd_patterns = [
            # Pattern 1: Số xuất hiện sau các prefix phổ biến
            r'(?:s[oố]|[Nn]o|Số)[\.:/\s]*(\d{12})',
            
            # Pattern 2: Số có thể có khoảng trắng hoặc dấu cách
            r'\b(\d{3}[\s.]?\d{3}[\s.]?\d{3}[\s.]?\d{3})\b',
            
            # Pattern 3: 12 chữ số liên tiếp
            r'\b(\d{12})\b',
            
            # Pattern 4: Số xuất hiện trong chuỗi text phức tạp
            r'.*?(\d{12}).*?'
        ]

        best_match = None
        best_confidence = 0.0
        
        for i, text in enumerate(texts):
            # Chuẩn hóa text: bỏ khoảng trắng, chuyển về lowercase
            normalized_text = ''.join(text.lower().split())
            
            for pattern in cccd_patterns:
                matches = re.finditer(pattern, normalized_text)
                for match in matches:
                    # Lấy group cuối cùng nếu có capturing groups, không thì lấy toàn bộ match
                    digits = match.group(1) if match.groups() else match.group(0)
                    
                    # Loại bỏ các ký tự không phải số
                    digits = ''.join(filter(str.isdigit, digits))
                    
                    # Kiểm tra độ dài và tính hợp lệ của số CCCD
                    if len(digits) == 12 and self._validate_id_number(digits):
                        base_confidence = scores[i]
                        
                        # Tăng confidence dựa trên các yếu tố:
                        # 1. Có các từ khóa xung quanh
                        if any(kw in normalized_text for kw in ['so', 'no.', 'số', 'cccd', 'căncước']):
                            base_confidence *= 1.1
                            
                        # 2. Xuất hiện ở vị trí phù hợp (thường ở đầu document)
                        if i < len(texts) // 3:  # Trong 1/3 đầu tiên của văn bản
                            base_confidence *= 1.05
                            
                        # 3. Pattern match quality (ưu tiên các pattern chính xác hơn)
                        pattern_priority = cccd_patterns.index(pattern)
                        pattern_bonus = 1.0 - (pattern_priority * 0.05)  # Giảm dần theo priority
                        base_confidence *= pattern_bonus
                        
                        base_confidence = min(1.0, base_confidence)  # Cap at 1.0
                        
                        # Cập nhật best match nếu tìm thấy kết quả tốt hơn
                        if base_confidence > best_confidence:
                            best_match = digits
                            best_confidence = base_confidence
        
        return best_match, best_confidence
    
    def _extract_other_info(self, texts: list, scores: list) -> dict:
        """Extract other relevant information from OCR results."""
        # Sử dụng openai để tiến hành chỉnh lại chính tả văn bản từ other_info
        avg_confidence = sum(scores) / len(scores) if scores else 0.0

        response = self.openai_client.chat.completions.create(
            model=self.model,
            messages=[
                {
                    "role": "system",
                    "content": """
                    Bạn là một trợ lý AI chuyên chỉnh sửa văn bản tiếng Việt.
                    Nhiệm vụ của bạn là sửa lỗi chính tả, ngữ pháp và định dạng văn bản.
                    Hãy chỉnh lại chính tả và trả về kết quả dưới dạng JSON với các trường sau:
                    {
                        "full_name": "<Tên người>",
                        "dob": "<Ngày sinh>",
                        "sex": "<Giới tính>",
                        "nationality": "<Quốc tịch>",
                        "origin": "<Quê quán>",
                        "residence": "<Nơi thường trú>",
                        "expiry_date": "<Ngày hết hạn>",
                    }
                    """
                },
                {
                    "role": "user",
                    "content": f"""
                    Dưới đây là các thông tin nhận dạng cần chỉnh sửa chính tả:
                    {texts}

                    Hãy chỉnh sửa chính tả và định dạng các thông tin này. Nếu không tìm thấy thông tin nào, hãy trả về các trường rỗng.
                    """
                }
            ],
            temperature=0,
            response_format={"type": "json_object"}
        )

        response_data = response.choices[0].message.content
        try:
            other_info = json.loads(response_data)

            if other_info['full_name'] == "Cộng Hòa Xã Hội Chủ Nghĩa Việt Nam":
                other_info['full_name'] = ""
            return other_info
        except ValueError:
            logger.error("Failed to parse OpenAI response as JSON")
            other_info = {
                "full_name": "",
                "dob": "",
                "sex": "",
                "nationality": "",
                "origin": "",
                "residence": "",
                "expiry_date": "",
            }

    def extract_text(
        self,
        image: np.ndarray,
        min_confidence: float = 0.5
    ) -> dict:
        """Extract text from image using OCR with ID number separation."""
        if not self._model_loaded:
            self._load_model()
        
        start_time = time.time()
        
        try:
            # Đảm bảo hình ảnh đúng định dạng
            if len(image.shape) == 2:  # Grayscale
                image = cv2.cvtColor(image, cv2.COLOR_GRAY2RGB)
            elif len(image.shape) == 3 and image.shape[2] == 4:  # RGBA
                image = cv2.cvtColor(image, cv2.COLOR_RGBA2RGB)
            
            # Chạy OCR sử dụng predict() thay vì ocr()
            result = self._ocr_engine.predict(image)
                        
            texts = []
            scores = []
            
            # Xử lý kết quả OCR
            if isinstance(result, list):
                for page_result in result:
                    if 'rec_texts' in page_result and isinstance(page_result['rec_texts'], list):
                        texts.extend(page_result['rec_texts'])
                    
                    if 'rec_scores' in page_result and isinstance(page_result['rec_scores'], list):
                        scores.extend(page_result['rec_scores'])
            
            # Extract ID number first
            id_number, id_confidence = self._extract_id_number(texts, scores)
            
            # Filter remaining texts based on confidence
            other_texts = []
            other_scores = []
            
            for text, score in zip(texts, scores):
                # Skip the ID number if found
                if id_number and ''.join(filter(str.isdigit, text)) == id_number:
                    continue
                    
                if score >= min_confidence:
                    other_texts.append(text)
                    other_scores.append(score)
            
            processing_time = time.time() - start_time
                        
            if id_confidence < 0.9:
                return {
                    'success': False,
                    'id_card_number': None,
                    'other_info': {
                        'texts': other_texts,
                        'average_confidence': sum(other_scores) / len(other_scores) if other_scores else 0.0
                    },
                    'processing_time': processing_time,
                    'error': "ID card number confidence too low"
                }
            else:
                other_info = self._extract_other_info(other_texts, other_scores)

                return {
                    'success': True,
                    'id_card_number': {
                        'number': id_number,
                        'confidence': id_confidence
                    },
                    'other_info': other_info,
                    'processing_time': processing_time,
                    'total_detected': len(texts),
                    'total_filtered': len(other_texts) + (1 if id_number else 0)
                }
            
        except Exception as e:
            logger.error(f"OCR extraction failed: {str(e)}", exc_info=True)
            return {
                'success': False,
                'id_card_number': None,
                'other_info': {
                    'texts': [],
                    'average_confidence': 0.0
                },
                'processing_time': time.time() - start_time,
                'error': str(e)
            }
    
    def is_model_loaded(self) -> bool:
        """Check if OCR model is loaded."""
        return self._model_loaded
    
    def is_full_information(other_info: dict) -> bool:
        for key, value in other_info.items():
            if not value or value.strip() == "" or value is None:
                return False
        return True