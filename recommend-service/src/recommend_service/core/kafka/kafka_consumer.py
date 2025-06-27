from bson import ObjectId
from kafka import KafkaConsumer
import json
from threading import Thread
from recommend_service.config.settings import settings
import logging
from datetime import datetime
from recommend_service.db.mongodb import MongoDB
from recommend_service.core.recommendation.models.hybrid import HybridRecommender
from recommend_service.core.recommendation.models.semantic_content_based import SemanticContentBasedRecommender
from openai import OpenAI

logger = logging.getLogger(__name__)

class KafkaEventConsumer:
    def __init__(self, semantic_recommender):
        self.semantic_recommender = semantic_recommender
        self.consumer = KafkaConsumer(
            'job_created', 'job_updated', 'job_updated_without_description_change', 'job_deleted',
            'profile_created', 'profile_updated', 'profile_deleted',
            bootstrap_servers=[settings.KAFKA_BOOTSTRAP_SERVERS],
            auto_offset_reset='latest',
            value_deserializer=lambda m: json.loads(m.decode('utf-8'))
        )
        self.db = MongoDB()
        self.openai_client = OpenAI(api_key=settings.OPENAI_API_KEY)
        self.model = 'gpt-4.1-nano'

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

                elif topic == 'job_updated_without_description_change':
                    job_data = event.get('data')
                    job_id = job_data.get('id')
                    self._update_job_embedding_without_description_change(job_id, job_data)
                    
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

    def _update_job_short_description(self, job_id, job_data):
        """Update job short description embedding"""
        try:
            description = job_data.get('description', '')
            clean_description = self._clean_html_content(description)
            
            response = self.openai_client.chat.completions.create(
                model=self.model,
                messages=[
                    {
                        "role": "system",
                        "content": """
                        Bạn là một trợ lý chuyên tóm tắt nội dung các bài đăng tuyển dụng việc làm.
                        """
                    },
                    {
                        "role": "user",
                        "content": f"""
                        Tóm tắt nội dung công việc sau,ít hơn 20 từ:
                        Mô tả: {clean_description}
                        """
                    }
                ],
                response_format={"type": "text"}
            )

            short_description_summary = response.choices[0].message.content.strip()
            if not short_description_summary:
                logger.warning(f"Empty short description summary for job_id: {job_id}")
                return
            try:
                jobs_collection = self.db.get_collection(
                    settings.MONGODB_JOB_DATABASE,
                    settings.MONGODB_JOBS_COLLECTION
                )
                result = jobs_collection.update_one(
                    {"_id": ObjectId(job_id)},
                    {"$set": {"shortDescription": short_description_summary}}
                )
            except Exception as e:
                logger.error(f"Error updating job short description embedding: {e}")

            if result.matched_count > 0:
                logger.info(f"Updated job short description for job_id: {job_id}")
            else:
                logger.warning(f"Job ID {job_id} not found for short description update")
        except Exception as e:
            logger.error(f"Error updating job short description embedding: {e}")

    def _update_job_embedding(self, job_id, job_data):
        """Update embedding for a single job"""
        try:
            # Cập nhật tiêu đề công việc
            self._update_job_short_description(job_id, job_data)

            description = job_data.get('description', '')
            clean_description = self._clean_html_content(description)

            categories_collection = self.db.get_collection(
                settings.MONGODB_JOB_DATABASE,
                settings.MONGODB_CATEGORIES_COLLECTION
            )

            # Lấy danh mục chính từ MongoDB
            main_category_id = job_data.get('categoryId', '')
            main_category = categories_collection.find_one({
                "_id": ObjectId(main_category_id)
            }) if main_category_id else None
            main_category_name = main_category['name'] if main_category else 'Không xác định'
            print(f"Main category name: {main_category_name}")

            # Tạo content
            content = ' '.join([
                f"Tiêu đề: {job_data.get('title', '')}",
                f"Mô tả: {clean_description}",
                # f"Yêu cầu: {job_data.get('requirements', '')}",
                f"Danh mục chính: {main_category_name}",
                # f"Kỹ năng: {job_data.get('skills', '')}",
                f"Kinh nghiệm: {job_data.get('experienceLevel', '')}",
                # f"Tags: {', '.join(job_data.get('tags', []))}",
            ])
            
            # Tạo embedding
            embedding = self.semantic_recommender.model.encode([content])[0]
            
            # Lưu vào MongoDB
            embeddings_collection = self.db.get_collection(
                settings.MONGODB_JOB_DATABASE,
                settings.MONGODB_JOB_EMBEDDINGS_COLLECTION
            )
            
            result = embeddings_collection.update_one(
                {"jobId": job_id},
                {"$set": {
                    "embedding": embedding.tolist(),
                    "updated_at": datetime.now()
                }},
                upsert=True
            )

            if result.matched_count > 0:
                logger.info(f"Updated existing embedding for jobId: {job_id}")
            else:
                logger.info(f"Created new embedding for jobId: {job_id}, inserted_id: {result.upserted_id}")

            logger.info(f"Updated embedding for jobId: {job_id}")

        except Exception as e:
            logger.error(f"Error updating job embedding: {e}")

    def _update_job_embedding_without_description_change(self, job_id, job_data):
        """Update embedding for a single job"""
        try:
            description = job_data.get('description', '')
            clean_description = self._clean_html_content(description)

            categories_collection = self.db.get_collection(
                settings.MONGODB_JOB_DATABASE,
                settings.MONGODB_CATEGORIES_COLLECTION
            )

            # Lấy danh mục chính từ MongoDB
            main_category_id = job_data.get('categoryId', '')
            main_category = categories_collection.find_one({
                "_id": ObjectId(main_category_id)
            }) if main_category_id else None
            main_category_name = main_category['name'] if main_category else 'Không xác định'
            print(f"Main category name: {main_category_name}")

            # Tạo content
            content = ' '.join([
                f"Tiêu đề: {job_data.get('title', '')}",
                f"Mô tả: {clean_description}",
                f"Yêu cầu: {job_data.get('requirements', '')}",
                f"Danh mục chính: {main_category_name}",
                f"Kỹ năng: {job_data.get('skills', '')}",
                f"Kinh nghiệm: {job_data.get('experienceLevel', '')}",
                f"Tags: {', '.join(job_data.get('tags', []))}",
            ])
            
            # Tạo embedding
            embedding = self.semantic_recommender.model.encode([content])[0]
            
            # Lưu vào MongoDB
            embeddings_collection = self.db.get_collection(
                settings.MONGODB_JOB_DATABASE,
                settings.MONGODB_JOB_EMBEDDINGS_COLLECTION
            )
            
            result = embeddings_collection.update_one(
                {"jobId": job_id},
                {"$set": {
                    "embedding": embedding.tolist(),
                    "updated_at": datetime.now()
                }},
                upsert=True
            )

            if result.matched_count > 0:
                logger.info(f"Updated existing embedding for jobId: {job_id}")
            else:
                logger.info(f"Created new embedding for jobId: {job_id}, inserted_id: {result.upserted_id}")

            logger.info(f"Updated embedding for jobId: {job_id}")

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
            embeddings_collection.delete_one({"jobId": job_id})
            logger.info(f"Deleted embedding for jobId: {job_id}")
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
            self.semantic_recommender.store_profile_embedding(user_id, user_profile, embedding)
            logger.info(f"Updated profile embedding for userId: {user_id}")

        except Exception as e:
            logger.error(f"Error updating profile embedding: {e}")
