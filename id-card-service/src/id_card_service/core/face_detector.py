import cv2
import numpy as np
from typing import List, Tuple

class FaceDetector:
    def __init__(self, scale_factor: float = 1.0, detection_method: str = "haar"):
        self.scale_factor = scale_factor
        self.detection_method = detection_method
        
        # Sử dụng Haar Cascade Classifier (nhanh hơn nhưng ít chính xác hơn)
        if detection_method == "haar":
            self.detector = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
        elif detection_method == "deepface":
            # Chỉ import khi cần thiết để giảm thời gian khởi động
            from deepface import DeepFace
            self.DeepFace = DeepFace
        else:
            raise ValueError(f"Unknown detection method: {detection_method}")
            
    def detect_faces(self, frame: np.ndarray) -> List[Tuple[int, int, int, int]]:
        """Detect faces in the given frame"""
        try:
            # Resize for faster processing
            if self.scale_factor < 1.0:
                small_frame = cv2.resize(frame, (0, 0), fx=self.scale_factor, fy=self.scale_factor)
            else:
                small_frame = frame.copy()
                
            face_locations = []
            
            if self.detection_method == "haar":
                # Convert to grayscale
                gray = cv2.cvtColor(small_frame, cv2.COLOR_BGR2GRAY)
                
                # Use a faster, less accurate approach first
                faces = self.detector.detectMultiScale(
                    gray,
                    scaleFactor=1.05,  # Larger value = faster but less accurate
                    minNeighbors=4,   # Lower value = faster but more false positives
                    minSize=(20, 20)
                )
                
                # Convert to (top, right, bottom, left) format
                for (x, y, w, h) in faces:
                    # Convert to (top, right, bottom, left) format
                    top = y
                    right = x + w
                    bottom = y + h
                    left = x
                    
                    # Scale back to original size if needed
                    if self.scale_factor < 1.0:
                        top = int(top / self.scale_factor)
                        right = int(right / self.scale_factor)
                        bottom = int(bottom / self.scale_factor)
                        left = int(left / self.scale_factor)
                        
                    face_locations.append((top, right, bottom, left))
            
            elif self.detection_method == "deepface":
                # Use DeepFace's RetinaFace detector
                try:
                    faces = self.DeepFace.extract_faces(
                        img_path=small_frame,
                        detector_backend="retinaface",
                        enforce_detection=False,
                        align=False
                    )
                    
                    for face_data in faces:
                        facial_area = face_data["facial_area"]
                        x, y, w, h = facial_area["x"], facial_area["y"], facial_area["w"], facial_area["h"]
                        
                        # Convert to (top, right, bottom, left) format
                        top = y
                        right = x + w
                        bottom = y + h
                        left = x
                        
                        # Scale back to original size if needed
                        if self.scale_factor < 1.0:
                            top = int(top / self.scale_factor)
                            right = int(right / self.scale_factor)
                            bottom = int(bottom / self.scale_factor)
                            left = int(left / self.scale_factor)
                            
                        face_locations.append((top, right, bottom, left))
                except Exception as e:
                    print(f"DeepFace detection error: {str(e)}")
            
            return face_locations
            
        except Exception as e:
            print(f"Error detecting faces: {str(e)}")
            return []
        
    def _enhance_image(self, image):
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
        gray = clahe.apply(gray)
        return cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR)
            
    def draw_face_rectangles(self, frame: np.ndarray, face_locations: List[Tuple[int, int, int, int]]) -> np.ndarray:
        """Draw rectangles around detected faces"""
        frame_copy = frame.copy()

        if not face_locations:
            return frame_copy

        for (top, right, bottom, left) in face_locations:
            cv2.rectangle(frame_copy, (left, top), (right, bottom), (0, 255, 0), 2)
        return frame_copy