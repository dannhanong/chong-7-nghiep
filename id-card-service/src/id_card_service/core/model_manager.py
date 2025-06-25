"""Model management and caching for OCR operations."""

import logging
import time
import threading
from typing import Dict, Optional, Any
from dataclasses import dataclass
from .ocr_engine import OCREngine
import psutil
import os

logger = logging.getLogger(__name__)


@dataclass
class ModelInfo:
    """Information about a loaded model."""
    engine: OCREngine
    load_time: float
    last_used: float
    usage_count: int

class ModelManager:
    """Singleton manager for OCR model caching and lifecycle."""
    
    _instance = None
    _lock = threading.Lock()
    
    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialized = False
        return cls._instance
    
    def __init__(self):
        if self._initialized:
            return
        
        self._models: Dict[str, ModelInfo] = {}
        self._max_cache_size = 3  # Maximum number of cached models
        self._default_config = {
            'lang': 'en'
        }
        self._initialized = True
        
        logger.info("ModelManager initialized")
    
    def get_model(self, config: Optional[Dict[str, Any]] = None) -> OCREngine:
        """
        Get OCR model with specified configuration.
        
        Args:
            config: Model configuration parameters
            
        Returns:
            OCREngine instance
        """
        if config is None:
            config = self._default_config.copy()
        else:
            # Merge with defaults
            merged_config = self._default_config.copy()
            merged_config.update(config)
            config = merged_config
        
        # Create cache key from configuration
        cache_key = self._create_cache_key(config)
        
        with self._lock:
            # Check if model is already cached
            if cache_key in self._models:
                model_info = self._models[cache_key]
                model_info.last_used = time.time()
                model_info.usage_count += 1
                logger.debug(f"Returning cached model: {cache_key}")
                return model_info.engine
            
            # Clean up cache if needed
            self._cleanup_cache()
            
            # Create new model
            start_time = time.time()
            engine = OCREngine(**config)
            load_time = time.time() - start_time
            
            # Cache the model
            self._models[cache_key] = ModelInfo(
                engine=engine,
                load_time=load_time,
                last_used=time.time(),
                usage_count=1
            )
            
            logger.info(f"Created and cached new model: {cache_key} (load time: {load_time:.2f}s)")
            return engine
    
    def _create_cache_key(self, config: Dict[str, Any]) -> str:
        """Create a unique cache key from model configuration."""
        key_parts = []
        for key in sorted(config.keys()):
            key_parts.append(f"{key}={config[key]}")
        return "|".join(key_parts)
    
    def _cleanup_cache(self) -> None:
        """Remove least recently used models if cache is full."""
        if len(self._models) >= self._max_cache_size:
            # Sort by last used time
            sorted_models = sorted(
                self._models.items(),
                key=lambda x: x[1].last_used
            )
            
            # Remove oldest model
            oldest_key = sorted_models[0][0]
            removed_model = self._models.pop(oldest_key)
            logger.info(f"Removed cached model: {oldest_key} "
                       f"(usage: {removed_model.usage_count})")
    
    def get_cache_info(self) -> Dict[str, Any]:
        """Get information about cached models."""
        with self._lock:
            cache_info = {
                'total_cached': len(self._models),
                'max_cache_size': self._max_cache_size,
                'models': {}
            }
            
            for key, model_info in self._models.items():
                cache_info['models'][key] = {
                    'load_time': model_info.load_time,
                    'last_used': model_info.last_used,
                    'usage_count': model_info.usage_count,
                    'model_loaded': model_info.engine.is_model_loaded()
                }
            
            return cache_info
    
    def clear_cache(self) -> None:
        """Clear all cached models."""
        with self._lock:
            cleared_count = len(self._models)
            self._models.clear()
            logger.info(f"Cleared {cleared_count} cached models")
    
    def preload_default_model(self) -> None:
        """Preload the default model for faster first request."""
        try:
            logger.info("Preloading default OCR model...")
            model = self.get_model()
            # Force model loading by calling a lightweight operation
            import numpy as np
            dummy_image = np.ones((100, 100), dtype=np.uint8) * 255
            model.extract_text(dummy_image, min_confidence=0.9)  # High threshold to avoid false positives
            logger.info("Default model preloaded successfully")
        except Exception as e:
            logger.warning(f"Failed to preload default model: {str(e)}")
    
    def get_memory_usage(self) -> Dict[str, Any]:
        """Get estimated memory usage of cached models."""
        try:
            process = psutil.Process(os.getpid())
            memory_info = process.memory_info()
            
            return {
                'rss_mb': memory_info.rss / 1024 / 1024,
                'vms_mb': memory_info.vms / 1024 / 1024,
                'cached_models': len(self._models)
            }
        except ImportError:
            return {
                'message': 'psutil not available for memory monitoring',
                'cached_models': len(self._models)
            }
    
    def update_config(self, new_config: Dict[str, Any]) -> None:
        """Update default configuration."""
        with self._lock:
            self._default_config.update(new_config)
            logger.info(f"Updated default configuration: {new_config}")
    
    def set_max_cache_size(self, max_size: int) -> None:
        """Set maximum cache size."""
        if max_size < 1:
            raise ValueError("Max cache size must be at least 1")
        
        with self._lock:
            self._max_cache_size = max_size
            # Clean up if current cache exceeds new limit
            while len(self._models) > max_size:
                self._cleanup_cache()
            
            logger.info(f"Set max cache size to {max_size}")

# Global model manager instance
model_manager = ModelManager()