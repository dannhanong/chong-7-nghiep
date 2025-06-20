import cv2
import numpy as np
import os
import json
import uuid
import time
import logging
from datetime import datetime
from typing import Dict, Any, Optional, List, Tuple
from deepface import DeepFace
import pickle
from id_card_service.db.mongodb import MongoDB
from id_card_service.utils.id_card_utils import get_id_card_by_user_id
from id_card_service.config.settings import settings
from id_card_service.processors.id_card_process import IDCardProcessor
from id_card_service.utils.user_utils import get_user_info_by_user_id
import os

BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
# Hoặc cách đơn giản hơn:
# from pathlib import Path
# BASE_DIR = Path(__file__).resolve().parents[3]  # Lấy thư mục cha cấp 3

known_faces_dir = os.path.join(BASE_DIR, "data", "known_faces")
placeholder_path = os.path.join(BASE_DIR, "data", "placeholder.jpg")

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class FaceRecognizer():
    def __init__(self, known_faces_dir: str = known_faces_dir):
        self.db = MongoDB()
        self.id_card_processor = IDCardProcessor()
        self.known_faces_dir = known_faces_dir

        # Tạo thư mục lưu face embeddings
        self.embeddings_dir = os.path.join(self.known_faces_dir, ".embeddings")
        os.makedirs(self.embeddings_dir, exist_ok=True)
        
        # Thêm biến model_name
        self.model_name = "Facenet512"  
        
        # Thêm biến threshold
        self.threshold = 0.5

        logger.info("Face Recognizer initialized with new MongoDB structure")

    def register_person_from_database(self, user_id: str) -> Dict[str, Any]:
        """
        Đăng ký người dùng từ ảnh CCCD trong MongoDB
        
        Args:
            user_id: ID của user trong database
            
        Returns:
            Dict: Kết quả đăng ký
        """
        result = {
            "success": False,
            "person_id": None,
            "message": "",
            "warnings": []
        }
        
        try:
            logger.info(f"Starting registration from MongoDB: {user_id}")
            
            # 1. Lấy thông tin identity card từ collection identity_cards
            identity_card = get_id_card_by_user_id(user_id)
            if not identity_card:
                result["message"] = f"Không tìm thấy thông tin CCCD cho user_id: {user_id}"
                return result
            
            # 2. Lấy frontImageCode
            front_image_code = identity_card.get("frontImageCode")
            if not front_image_code:
                result["message"] = "CCCD không có ảnh mặt trước (frontImageCode)"
                return result
            
            # 3. Tạo URL và tải ảnh CCCD
            id_card_image = self.id_card_processor.fetch_id_card_image(front_image_code)
            
            if id_card_image is None:
                result["message"] = "Không thể tải ảnh CCCD từ URL"
                return result
            
            # 4. Trích xuất gương mặt từ ảnh CCCD
            face_result = self.id_card_processor.extract_face_from_id_card(id_card_image)
            if face_result is None:
                result["message"] = "Không thể trích xuất gương mặt từ ảnh CCCD"
                return result
            
            face_image, face_location = face_result
            
            # 5. Sử dụng user_id làm person_id
            person_id = user_id
            
            # Kiểm tra xem person đã tồn tại chưa
            if self._person_exists(person_id):
                result["message"] = f"Người dùng với ID {person_id} đã được đăng ký face rồi"
                result["person_id"] = person_id
                return result
            
            # 6. Tạo thư mục cho person và lưu ảnh gương mặt
            person_dir = os.path.join(self.known_faces_dir, person_id)
            os.makedirs(person_dir, exist_ok=True)
            
            face_path = os.path.join(person_dir, "face_from_cccd.jpg")
            cv2.imwrite(face_path, face_image)

            embedding_saved = self.save_face_embedding(
                person_id=person_id,
                face_image=face_image,
                filename="face_from_cccd_embedding"
            )

            if not embedding_saved:
                result["warnings"].append("Không thể lưu embedding cho gương mặt")

            # 7. Thêm metadata cho person (từ identity_card)
            metadata = {
                "full_name": identity_card.get("fullName", ""),
                "id_number": identity_card.get("idCardNumber", ""),
                "date_of_birth": identity_card.get("dateOfBirth", ""),
                "sex": identity_card.get("sex", ""),
                "nationality": identity_card.get("nationality", ""),
                "registered_at": datetime.now().isoformat(),
                "source": "id_card"
            }
            
            metadata_path = os.path.join(person_dir, "metadata.json")
            with open(metadata_path, 'w', encoding='utf-8') as f:
                json.dump(metadata, f, ensure_ascii=False, indent=2)
            
            # 8. Rebuild face database
            self.build_database()
            
            result.update({
                "success": True,
                "person_id": person_id,
                "message": f"Đăng ký thành công gương mặt cho {metadata['full_name']}",
            })
            
            return result
            
        except Exception as e:
            error_msg = f"Lỗi khi đăng ký từ database: {str(e)}"
            logger.error(error_msg)
            result["message"] = error_msg
            return result
    
    def _person_exists(self, person_id: str) -> bool:
        """
        Kiểm tra xem person đã tồn tại chưa
        
        Args:
            person_id: ID của person
            
        Returns:
            bool: True nếu đã tồn tại
        """
        person_dir = os.path.join(self.known_faces_dir, person_id)
        return os.path.exists(person_dir)

    def extract_face_embedding(self, face_image: np.ndarray) -> Optional[np.ndarray]:
        """
        Trích xuất vector embedding từ ảnh khuôn mặt
        
        Args:
            face_image: Ảnh khuôn mặt đã cắt
            
        Returns:
            np.ndarray: Vector embedding 
        """
        try:
            # Lưu ảnh tạm thời để DeepFace xử lý
            temp_path = os.path.join(self.known_faces_dir, f"temp_{time.time()}.jpg")
            cv2.imwrite(temp_path, face_image)
            
            # Trích xuất embedding
            embedding_objs = DeepFace.represent(
                img_path=temp_path,
                model_name=self.model_name,
                enforce_detection=False,
                detector_backend="skip"
            )
            
            # Xóa file tạm
            if os.path.exists(temp_path):
                os.remove(temp_path)
            
            if embedding_objs and len(embedding_objs) > 0:
                # Lấy vector embedding từ kết quả
                embedding_vector = embedding_objs[0]["embedding"]
                return np.array(embedding_vector)
            
            return None
        
        except Exception as e:
            logger.error(f"Error extracting face embedding: {str(e)}")
            return None

    def save_face_embedding(self, person_id: str, face_image: np.ndarray, filename: str = None) -> bool:
        """
        Tạo và lưu embedding cho khuôn mặt
        
        Args:
            person_id: ID người dùng
            face_image: Ảnh khuôn mặt
            filename: Tên file (mặc định là timestamp)
            
        Returns:
            bool: True nếu thành công
        """
        try:
            # Tạo thư mục cho người dùng trong .embeddings nếu chưa có
            person_embedding_dir = os.path.join(self.embeddings_dir, person_id)
            os.makedirs(person_embedding_dir, exist_ok=True)
            
            # Tạo embedding
            embedding = self.extract_face_embedding(face_image)
            if embedding is None:
                logger.error(f"Could not extract embedding for person: {person_id}")
                return False
            
            # Tạo tên file
            if filename is None:
                filename = f"embedding_{int(time.time())}.pkl"
            elif not filename.endswith('.pkl'):
                filename = f"{filename}.pkl"
            
            # Lưu embedding
            embedding_path = os.path.join(person_embedding_dir, filename)
            with open(embedding_path, 'wb') as f:
                pickle.dump(embedding, f)
            
            logger.info(f"Saved embedding for {person_id} to {embedding_path}")
            return True
        
        except Exception as e:
            logger.error(f"Error saving face embedding: {str(e)}")
            return False
        
    def recognize_face_with_saved_user_id_embedding(
            self, 
            user_id: str, 
            face_image: np.ndarray, 
            threshold: float = None
        ) -> Dict[str, Any]:
        """
        Nhận diện khuôn mặt bằng cách so sánh với các embedding đã lưu
        
        Args:
            user_id: ID của người dùng
            face_image: Ảnh khuôn mặt cần nhận diện
            threshold: Ngưỡng so sánh (nếu không cung cấp, sử dụng self.threshold)
            
        Returns:
            Dict: Kết quả nhận diện với person_id, distance, confidence
        """
        if threshold is None:
            threshold = self.threshold
            
        result = {
            "recognized": False,
            "person_id": None,
            "distance": 1.0,
            "confidence": 0.0
        }
        
        try:
            # Trích xuất embedding cho khuôn mặt đầu vào
            query_embedding = self.extract_face_embedding(face_image)
            if query_embedding is None:
                return result
                
            best_match_distance = float('inf')
            best_match_person = None

            # Lấy thư mục embedding của user_id
            person_embedding_dir = os.path.join(self.embeddings_dir, user_id)
            if not os.path.isdir(person_embedding_dir):
                logger.warning(f"Embedding directory for user_id {user_id} does not exist")
                return result
                
            # Duyệt qua các file embedding
            for embedding_file in os.listdir(person_embedding_dir):
                if not embedding_file.endswith('.pkl'):
                    continue
                    
                embedding_path = os.path.join(person_embedding_dir, embedding_file)
                
                # Đọc embedding
                with open(embedding_path, 'rb') as f:
                    stored_embedding = pickle.load(f)
                
                # Tính khoảng cách cosine
                from scipy.spatial.distance import cosine
                distance = cosine(query_embedding, stored_embedding)
                
                # Cập nhật best match
                if distance < best_match_distance:
                    best_match_distance = distance
                    best_match_person = user_id

                # Thêm early return khi tìm thấy match rất tốt
                if distance < threshold * 0.6:  # Match rất tốt
                    confidence = 100 * (1 - distance / threshold)
                    return {
                        "recognized": True,
                        "person_id": user_id,
                        "distance": distance,
                        "confidence": min(100, max(0, confidence))
                    }
            
            # Kiểm tra kết quả
            if best_match_person and best_match_distance < threshold:
                confidence = 100 * (1 - best_match_distance / threshold)
                result.update({
                    "recognized": True,
                    "person_id": best_match_person,
                    "distance": best_match_distance,
                    "confidence": min(100, max(0, confidence))
                })
            
            return result
            
        except Exception as e:
            logger.error(f"Error recognizing face with saved embeddings: {str(e)}")
            return result
        
    def build_database(self):
        """Build database of face representations"""
        try:
            print("Building face database...")
            if not os.path.exists(self.known_faces_dir) or len(os.listdir(self.known_faces_dir)) == 0:
                print(f"Known faces directory '{self.known_faces_dir}' is empty or does not exist.")
                return
            
            if not os.path.exists(placeholder_path):
                blank_image = np.ones((100, 100, 3), dtype=np.uint8) * 255
                cv2.imwrite(placeholder_path, blank_image)
            
            # Khởi tạo DeepFace database
            try:
                self.representations = DeepFace.find(
                    img_path=placeholder_path,
                    db_path=self.known_faces_dir,
                    model_name=self.model_name,
                    enforce_detection=False,
                    detector_backend="skip",
                    silent=True
                )
                print(f"Face database built with {len(os.listdir(self.known_faces_dir))} identities")
            except Exception as e:
                if "No faces were detected" in str(e):
                    print("Using alternate method to build database...")
                    # Tạo database một cách thủ công
                    person_folders = [f for f in os.listdir(self.known_faces_dir) 
                                     if os.path.isdir(os.path.join(self.known_faces_dir, f))]
                    print(f"Found {len(person_folders)} person folders: {person_folders}")
                else:
                    print(f"Error building face database: {str(e)}")
                
        except Exception as e:
            print(f"Error in build_database: {str(e)}")