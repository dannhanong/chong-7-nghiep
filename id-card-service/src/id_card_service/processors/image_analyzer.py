"""Image Quality Analysis module for CCCD preprocessing."""

import cv2
import numpy as np
import logging
from typing import Dict, List, Tuple
from scipy import ndimage

logger = logging.getLogger(__name__)

class ImageQualityAnalyzer:
    """Analyzer for assessing image quality and determining preprocessing needs."""
    
    def __init__(self):
        """Initialize the image quality analyzer."""
        self.quality_thresholds = {
            'blur_threshold': 100.0,        # Laplacian variance threshold
            'brightness_low': 50,           # Too dark threshold
            'brightness_high': 200,         # Too bright threshold  
            'contrast_low': 30,             # Low contrast threshold
            'noise_threshold': 0.3,         # Noise level threshold
            'rotation_threshold': 2.0       # Rotation angle threshold (degrees)
        }
    
    def assess_quality(self, image: np.ndarray) -> Dict:
        """
        Assess image quality and recommend preprocessing steps.
        
        Args:
            image: Input image as numpy array
            
        Returns:
            Dict containing quality metrics and preprocessing recommendations
        """
        try:
            # Convert to grayscale if needed for analysis
            if len(image.shape) == 3:
                gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
            else:
                gray = image.copy()
            
            # Calculate quality metrics
            blur_score = self._calculate_blur_score(gray)
            brightness = self._calculate_brightness(gray)
            contrast = self._calculate_contrast(gray)
            noise_level = self._calculate_noise_level(gray)
            rotation_angle = self._detect_rotation_angle(gray)
            resolution_quality = self._assess_resolution(image)
            
            # Determine preprocessing needs
            preprocessing_needed = self._determine_preprocessing_steps(
                blur_score, brightness, contrast, noise_level, rotation_angle, resolution_quality
            )
            
            quality_assessment = {
                'blur_score': float(blur_score),
                'brightness': float(brightness),
                'contrast': float(contrast),
                'noise_level': float(noise_level),
                'rotation_angle': float(rotation_angle),
                'resolution_quality': resolution_quality,
                'preprocessing_needed': preprocessing_needed,
                'overall_quality': self._calculate_overall_quality(
                    blur_score, brightness, contrast, noise_level
                )
            }
            
            logger.info(f"Quality assessment completed: {quality_assessment}")
            return quality_assessment
            
        except Exception as e:
            logger.error(f"Image quality assessment failed: {str(e)}")
            return {
                'blur_score': 0.0,
                'brightness': 128.0,
                'contrast': 50.0,
                'noise_level': 0.5,
                'rotation_angle': 0.0,
                'resolution_quality': 'unknown',
                'preprocessing_needed': ['normalize'],
                'overall_quality': 0.5,
                'error': str(e)
            }
    
    def _calculate_blur_score(self, gray: np.ndarray) -> float:
        """Calculate blur score using Laplacian variance."""
        laplacian = cv2.Laplacian(gray, cv2.CV_64F)
        variance = laplacian.var()
        
        # Normalize to 0-1 scale (higher = sharper)
        blur_score = min(1.0, variance / self.quality_thresholds['blur_threshold'])
        return blur_score
    
    def _calculate_brightness(self, gray: np.ndarray) -> float:
        """Calculate average brightness."""
        return float(np.mean(gray))
    
    def _calculate_contrast(self, gray: np.ndarray) -> float:
        """Calculate contrast using standard deviation."""
        return float(np.std(gray))
    
    def _calculate_noise_level(self, gray: np.ndarray) -> float:
        """Estimate noise level using high-frequency components."""
        # Apply Gaussian blur and calculate difference
        blurred = cv2.GaussianBlur(gray, (5, 5), 0)
        noise = cv2.absdiff(gray, blurred)
        
        # Calculate noise level as normalized mean of high-frequency components
        noise_level = np.mean(noise) / 255.0
        return float(noise_level)
    
    def _detect_rotation_angle(self, gray: np.ndarray) -> float:
        """Detect rotation angle using Hough line transform."""
        try:
            # Edge detection
            edges = cv2.Canny(gray, 50, 150, apertureSize=3)
            
            # Hough line detection
            lines = cv2.HoughLines(edges, 1, np.pi/180, threshold=100)
            
            if lines is not None:
                angles = []
                for line in lines:
                    rho, theta = line[0]
                    angle = np.degrees(theta) - 90
                    
                    # Keep angles between -45 and 45 degrees
                    if angle > 45:
                        angle -= 90
                    elif angle < -45:
                        angle += 90
                    
                    angles.append(angle)
                
                if angles:
                    # Return median angle to avoid outliers
                    return float(np.median(angles))
            
            return 0.0
            
        except Exception as e:
            logger.warning(f"Rotation detection failed: {str(e)}")
            return 0.0
    
    def _assess_resolution(self, image: np.ndarray) -> str:
        """Assess image resolution quality."""
        height, width = image.shape[:2]
        total_pixels = height * width
        
        if total_pixels >= 1920 * 1080:  # Full HD or higher
            return 'high'
        elif total_pixels >= 1280 * 720:  # HD
            return 'medium'
        elif total_pixels >= 640 * 480:   # VGA
            return 'low'
        else:
            return 'very_low'
    
    def _determine_preprocessing_steps(
        self, 
        blur_score: float, 
        brightness: float, 
        contrast: float, 
        noise_level: float,
        rotation_angle: float,
        resolution_quality: str
    ) -> List[str]:
        """Determine which preprocessing steps are needed."""
        steps = []
        
        # Check for noise reduction need
        if noise_level > self.quality_thresholds['noise_threshold']:
            steps.append('denoise')
        
        # Check for contrast enhancement need
        if contrast < self.quality_thresholds['contrast_low']:
            steps.append('enhance_contrast')
        
        # Check for brightness adjustment need
        if brightness < self.quality_thresholds['brightness_low']:
            steps.append('brighten')
        elif brightness > self.quality_thresholds['brightness_high']:
            steps.append('darken')
        
        # Check for rotation correction need
        if abs(rotation_angle) > self.quality_thresholds['rotation_threshold']:
            steps.append('correct_rotation')
        
        # Check for sharpening need (if not too noisy)
        if blur_score < 0.5 and noise_level < 0.4:
            steps.append('sharpen')
        
        # Check for upscaling need
        if resolution_quality in ['low', 'very_low']:
            steps.append('upscale')
        
        # Always normalize as final step
        if steps:
            steps.append('normalize')
        
        return steps
    
    def _calculate_overall_quality(
        self, 
        blur_score: float, 
        brightness: float, 
        contrast: float, 
        noise_level: float
    ) -> float:
        """Calculate overall quality score (0-1)."""
        # Normalize brightness score (optimal around 128)
        brightness_score = 1.0 - abs(brightness - 128) / 128
        
        # Normalize contrast score
        contrast_score = min(1.0, contrast / 100.0)
        
        # Invert noise level (lower noise = higher score)
        noise_score = 1.0 - noise_level
        
        # Weighted average
        overall_quality = (
            blur_score * 0.3 +
            brightness_score * 0.2 +
            contrast_score * 0.3 +
            noise_score * 0.2
        )
        
        return float(max(0.0, min(1.0, overall_quality)))


class ImageQualityMetrics:
    """Helper class for image quality metrics calculation."""
    
    @staticmethod
    def calculate_ssim(img1: np.ndarray, img2: np.ndarray) -> float:
        """Calculate Structural Similarity Index (SSIM) between two images."""
        try:
            from skimage.metrics import structural_similarity as ssim
            
            # Convert to grayscale if needed
            if len(img1.shape) == 3:
                img1 = cv2.cvtColor(img1, cv2.COLOR_RGB2GRAY)
            if len(img2.shape) == 3:
                img2 = cv2.cvtColor(img2, cv2.COLOR_RGB2GRAY)
            
            # Ensure same size
            if img1.shape != img2.shape:
                img2 = cv2.resize(img2, (img1.shape[1], img1.shape[0]))
            
            return float(ssim(img1, img2))
            
        except ImportError:
            logger.warning("scikit-image not available, using basic correlation")
            return float(np.corrcoef(img1.flatten(), img2.flatten())[0, 1])
        except Exception as e:
            logger.error(f"SSIM calculation failed: {str(e)}")
            return 0.0
    
    @staticmethod
    def calculate_psnr(img1: np.ndarray, img2: np.ndarray) -> float:
        """Calculate Peak Signal-to-Noise Ratio (PSNR) between two images."""
        try:
            mse = np.mean((img1.astype(float) - img2.astype(float)) ** 2)
            if mse == 0:
                return float('inf')
            
            max_pixel = 255.0
            psnr = 20 * np.log10(max_pixel / np.sqrt(mse))
            return float(psnr)
            
        except Exception as e:
            logger.error(f"PSNR calculation failed: {str(e)}")
            return 0.0