import json
import logging
import threading
from typing import Dict, Any, Callable
from confluent_kafka import Producer, Consumer, KafkaError
from searchaiid_service.config.settings import settings

logger = logging.getLogger(__name__)

class KafkaService:
    def __init__(self):
        self.bootstrap_servers = settings.KAFKA_BOOTSTRAP_SERVERS
        self.consumer_group = settings.KAFKA_CONSUMER_GROUP

        self.producer_config = {
            'bootstrap.servers': self.bootstrap_servers,
        }

        self.consumer_config = {
            'bootstrap.servers': self.bootstrap_servers,
            'group.id': self.consumer_group,
            'auto.offset.reset': settings.KAFKA_AUTO_OFFSET_RESET,
        }

        self.producer = Producer(self.producer_config)
        self.consumers = {}
        self.consumer_threads = {}

    def produce_message(self, topic: str, key: str, value: Dict[str, Any]) -> None:
        """Gửi thông điệp đến Kafka."""
        try:
            headers = [
                ('__TypeId__', b'com.dan.events.dtos.OcrTestMessage')
            ]

            self.producer.produce(
                topic=topic,
                key=key.encode('utf-8') if key else None,
                value=json.dumps(value).encode('utf-8'),
                headers=headers,
                callback=self._delivery_callback
            )
            self.producer.flush()
            print(f"Message produced to topic {topic}")
        except Exception as e:
            logger.error(f"Error producing message to topic {topic}: {e}")
            raise

    def _delivery_callback(self, err, msg):
        """Callback được gọi sau khi message được gửi đi"""
        if err:
            logger.error(f"Message delivery failed: {err}")
        else:
            logger.debug(f"Message delivered to {msg.topic()} [{msg.partition()}]")

    def start_consumer(self, topic: str, message_handler: Callable[[Dict], None]) -> None:
        """Khởi động consumer để lắng nghe thông điệp từ topic"""
        if topic in self.consumer_threads and self.consumer_threads[topic].is_alive():
            logger.warning(f"Consumer for topic {topic} is already running")
            return
        
        # Tạo consumer mới
        consumer = Consumer(self.consumer_config)
        consumer.subscribe([topic])
        self.consumers[topic] = consumer
        
        # Khởi động thread riêng cho consumer
        thread = threading.Thread(
            target=self._consume_messages,
            args=(topic, message_handler),
            daemon=True
        )
        self.consumer_threads[topic] = thread
        thread.start()
        logger.info(f"Started consumer for topic {topic}")

    def _consume_messages(self, topic: str, message_handler: Callable[[Dict], None]) -> None:
        """Hàm xử lý thông điệp liên tục trong thread riêng"""
        consumer = self.consumers.get(topic)
        if not consumer:
            logger.error(f"No consumer found for topic {topic}")
            return
        
        try:
            while True:
                msg = consumer.poll(1.0)  # timeout 1 second
                
                if msg is None:
                    continue
                
                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        logger.debug(f"Reached end of partition for {msg.topic()} [{msg.partition()}]")
                    else:
                        logger.error(f"Error during consuming: {msg.error()}")
                else:
                    try:
                        # Parse JSON message
                        value = json.loads(msg.value().decode('utf-8'))
                        # Process message in handler
                        message_handler(value)
                    except json.JSONDecodeError:
                        logger.error(f"Failed to parse message as JSON: {msg.value()}")
                    except Exception as e:
                        logger.error(f"Error processing message: {e}")
        except Exception as e:
            logger.error(f"Consumer thread error: {e}")
        finally:
            # Đóng consumer khi thread kết thúc
            consumer.close()
            logger.info(f"Consumer for topic {topic} closed")
    
    def stop_consumer(self, topic: str) -> None:
        """Dừng consumer cho topic cụ thể"""
        if topic in self.consumers:
            # Signal to close consumer
            if topic in self.consumers:
                self.consumers[topic].close()
                del self.consumers[topic]
            logger.info(f"Stopped consumer for topic {topic}")

    def stop_all_consumers(self) -> None:
        """Dừng tất cả consumers"""
        for topic in list(self.consumers.keys()):
            self.stop_consumer(topic)
        logger.info("All consumers stopped")

kafka_service = KafkaService()