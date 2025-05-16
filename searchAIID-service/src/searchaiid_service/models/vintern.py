import torch
from transformers import AutoModel, AutoTokenizer
from searchaiid_service.config.settings import settings
from searchaiid_service.utils.image_utils import load_image
from searchaiid_service.core.ocr.extractor import extract_json_from_text
import logging

class VinternModel:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(VinternModel, cls).__new__(cls)
            cls._instance._init_model()
        return cls._instance
    
    def _init_model(self):
        self.model = AutoModel.from_pretrained(
            settings.MODEL_PATH,
            torch_dtype=torch.bfloat16,
            low_cpu_mem_usage=True,
            trust_remote_code=True,
            use_flash_attn=False
        ).eval()

        if torch.cuda.is_available():
            self.model = self.model.cuda()

        self.tokenizer = AutoTokenizer.from_pretrained(
            settings.MODEL_PATH,
            trust_remote_code=True,
            use_fast=False
        )

    def extract_info(self, image_path, prompt=None):
        if prompt is None:
            prompt = '''<image>\nTrích xuất thông tin từ căn cước công dân hoặc chứng minh thư và trả về dạng JSON:
            {
                "name": "Tên",
                "dob": "Ngày sinh",
                "gender": "Giới tính",
                "nationality": "Quốc tịch",
                "country": "Quê quán",
                "address": "Nơi thường trú",
                "id_number": "Số căn cước công dân hoặc chứng minh thư",
                "expiry_date": "Có giá trị đến",
            }
            '''

        pixel_values = load_image(
            image_path, 
            input_size=settings.MAX_IMAGE_SIZE, 
            max_num=settings.MAX_IMAGE_BLOCKS
        )

        if torch.cuda.is_available():
            pixel_values = pixel_values.to(dtype=torch.bfloat16, device='cuda')
        else:
            self.model.to(torch.float32)
            pixel_values = pixel_values.float()
        
        # generation_config = {
        #     "max_new_tokens": 1024,
        #     "do_sample": False,
        #     "num_beams": 3,
        #     "repetition_penalty": 2.5
        # }
        generation_config = dict(max_new_tokens= 1024, do_sample=False, num_beams = 3, repetition_penalty=2.5)
        
        try:
            response, _ = self.model.chat(
                self.tokenizer, 
                pixel_values, 
                prompt, 
                generation_config, 
                history=None, 
                return_history=True
            )
        
            return extract_json_from_text(response)
        except Exception as e:
            logging.error(f"Lỗi khi gọi model.chat: {str(e)}")
            raise