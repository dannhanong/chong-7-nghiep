from kafka import KafkaConsumer
import json
from threading import Thread
from recommend_service.config.settings import settings
import logging
from datetime import datetime
from recommend_service.db.mongodb import MongoDB
from recommend_service.core.recommendation.models.hybrid import HybridRecommender
from recommend_service.core.recommendation.models.semantic_content_based import SemanticContentBasedRecommender

logger = logging.getLogger(__name__)

class KafkaEventConsumer:
    def __init__(self, semantic_recommender):
        self.semantic_recommender = semantic_recommender
        self.consumer = KafkaConsumer(
            'job_created', 'job_updated', 'job_deleted',
            'profile_created', 'profile_updated', 'profile_deleted',
            bootstrap_servers=[settings.KAFKA_BOOTSTRAP_SERVERS],
            auto_offset_reset='latest',
            value_deserializer=lambda m: json.loads(m.decode('utf-8'))
        )
        self.db = MongoDB()

    def start(self):
        """Start consuming messages in a separate thread"""
        Thread(target=self._consume_events, daemon=True).start()

    def _consume_events(self):
        """Consume events and update vectors accordingly"""
        for message in self.consumer:
            try:
                topic = message.topic
                event = message.value
                
                if topic == 'job_created' or topic == 'job_updated':
                    job_data = event.get('data')
                    job_id = job_data.get('id')
                    self._update_job_embedding(job_id, job_data)
                    
                elif topic == 'job_deleted':
                    job_id = event.get('data', {}).get('id')
                    self._delete_job_embedding(job_id)

                elif topic == 'profile_created' or topic == 'profile_updated':
                    user_id = event.get('userId')
                    self._update_profile_embedding(user_id)

                elif topic == 'profile_deleted':
                    user_id = event.get('userId')
                    self._delete_profile_embedding(user_id)
                    
            except Exception as e:
                logger.error(f"Error processing Kafka event: {e}")

    def _update_job_embedding(self, job_id, job_data):
        """Update embedding for a single job"""
        try:
            description = job_data.get('description', '')
            clean_description = self._clean_html_content(description)
            
            # Tạo content
            content = ' '.join([
                f"Tiêu đề: {job_data.get('title', '')}",
                f"Mô tả: {clean_description}",
                f"Yêu cầu: {job_data.get('requirements', '')}",
                f"Kỹ năng: {job_data.get('skills', '')}",
                f"Kinh nghiệm: {job_data.get('experienceLevel', '')}"
            ])
            
            # Tạo embedding
            embedding = self.semantic_recommender.model.encode([content])[0]
            
            # Lưu vào MongoDB
            embeddings_collection = self.db.get_collection(
                settings.MONGODB_JOB_DATABASE,
                settings.MONGODB_JOB_EMBEDDINGS_COLLECTION
            )
            
            result = embeddings_collection.update_one(
                {"job_id": job_id},
                {"$set": {
                    "embedding": embedding.tolist(),
                    "updated_at": datetime.now()
                }},
                upsert=True
            )

            if result.matched_count > 0:
                logger.info(f"Updated existing embedding for job_id: {job_id}")
            else:
                logger.info(f"Created new embedding for job_id: {job_id}, inserted_id: {result.upserted_id}")
            
            logger.info(f"Updated embedding for job_id: {job_id}")
            
        except Exception as e:
            logger.error(f"Error updating job embedding: {e}")

    def _clean_html_content(self, html_content):
        """Clean HTML content to plain text"""
        if not html_content:
            return ""

        try:
            from bs4 import BeautifulSoup
            soup = BeautifulSoup(html_content, 'html.parser')

            for script_or_style in soup(['script', 'style']):
                script_or_style.decompose()

            text = soup.get_text()

            lines = (line.strip() for line in text.splitlines())
            chunks = (phrase.strip() for line in lines for phrase in line.split("  "))
            text = ' '.join(chunk for chunk in chunks if chunk)

            return text
        except ImportError:
            import re
            text = re.sub(r'<[^>]+>', ' ', html_content)

            text = re.sub(r'&nbsp;', ' ', text)
            text = re.sub(r'&amp;', '&', text)
            text = re.sub(r'&lt;', '<', text)
            text = re.sub(r'&gt;', '>', text)
            text = re.sub(r'&quot;', '"', text)
            text = re.sub(r'&#39;', "'", text)
            
            # Chuẩn hóa khoảng trắng
            text = re.sub(r'\s+', ' ', text).strip()
            
            return text
        
        except Exception as e:
            logger.warning(f"Error cleaning HTML content: {e}")
            # Trả về văn bản gốc nếu xảy ra lỗi
            return html_content

    def _delete_job_embedding(self, job_id):
        """Delete embedding for a job"""
        try:
            embeddings_collection = self.db.get_collection(
                settings.MONGODB_JOB_DATABASE,
                settings.MONGODB_JOB_EMBEDDINGS_COLLECTION
            )
            embeddings_collection.delete_one({"job_id": job_id})
            logger.info(f"Deleted embedding for job_id: {job_id}")
        except Exception as e:
            logger.error(f"Error deleting job embedding: {e}")

    def _update_profile_embedding(self, user_id):
        """Update embedding for a user profile"""
        try:
            recommender = HybridRecommender()
            user_profile = recommender.build_user_profile(user_id)
            
            if not user_profile:
                logger.warning(f"Empty profile data for user_id: {user_id}")
                return
                
            # Sử dụng phương thức của semantic_recommender
            profile_content = self.semantic_recommender._build_semantic_profile(user_profile)
            embedding = self.semantic_recommender.model.encode([profile_content])[0]
            
            # Lưu vào MongoDB
            self.semantic_recommender._store_profile_embedding(user_id, user_profile, embedding)
            logger.info(f"Updated profile embedding for user_id: {user_id}")
                    
        except Exception as e:
            logger.error(f"Error updating profile embedding: {e}")
