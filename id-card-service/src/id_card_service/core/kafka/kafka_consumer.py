from bson import ObjectId
from kafka import KafkaConsumer
import json
from threading import Thread
from id_card_service.config.settings import settings
import logging
from datetime import datetime
from id_card_service.db.mongodb import MongoDB
from openai import OpenAI
from id_card_service.core.face_recognizer import FaceRecognizer

logger = logging.getLogger(__name__)

class KafkaEventConsumer:
    def __init__(self):
        self.consumer = KafkaConsumer(
            'job_create_identity_card',
            bootstrap_servers=[settings.KAFKA_BOOTSTRAP_SERVERS],
            auto_offset_reset='latest',
            value_deserializer=lambda m: json.loads(m.decode('utf-8')),
        )
        self.db = MongoDB()
        self.openai_client = OpenAI(api_key=settings.OPENAI_API_KEY)
        self.model = 'gpt-4.1-nano'

    def start(self):
        """Start consuming messages in a separate thread"""
        Thread(target=self._consume_events, daemon=True).start()

    def _consume_events(self):
        """Consume events and update vectors accordingly"""
        logger.info("Starting to consume Kafka events...")
        for message in self.consumer:
            try:
                topic = message.topic
                event = message.value
                
                if topic == 'job_create_identity_card':
                    data = event.get('data')
                    user_id = data.get('userId')
                    # self._add_id_card(user_id)
                    logger.info(f"Processing event for user_id: {user_id}")
                    self._add_id_card(user_id)
                    
            except Exception as e:
                logger.error(f"Error processing Kafka event: {e}")

    def _add_id_card(self, user_id):
        try:
            face_recognizer = FaceRecognizer()

            result = face_recognizer.register_person_from_database(
                user_id=user_id
            )
            
            if result["success"]:
                    logger.info(f"✅ SUCCESS: {result['message']}")
                    logger.info(f"   Person ID: {result['person_id']}")
                    
                    if result.get("warnings"):
                        logger.warning("⚠️  Warnings:")
                        for warning in result["warnings"]:
                            logger.warning(f"     - {warning}")
                    else:
                        logger.error(f"❌ FAILED: {result['message']}")
                        
                    return result
        except Exception as e:
            logger.error(f"Error updating job short description embedding: {e}")