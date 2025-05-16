import json
import re
import logging

logger = logging.getLogger(__name__)

def extract_json_from_text(text):
    try:
        json_pattern = r'```(?:json)?\s*([\s\S]*?)```'
        match = re.search(json_pattern, text)

        if match:
            json_str = match.group(1).strip()
            logger.debug(f"Extracted JSON string from markdown: {json_str[:100]}...")
            return json.loads(json_str)
        
        json_pattern = r'(\{[\s\S]*\})'
        match = re.search(json_pattern, text)
        if match:
            json_str = match.group(1)
            logger.debug(f"Extracted raw JSON string: {json_str[:100]}...")
            return json.loads(json_str)
        
        # Nếu không tìm thấy, thử parse toàn bộ text
        logger.debug("Trying to parse entire text as JSON")
        return json.loads(text)
    
    except json.JSONDecodeError as e:
        logger.warning(f"Failed to parse JSON: {e}")
        # Nếu không phải JSON, parse text thành dict
        result = {}
        
        # Tìm các dòng theo dạng "key: value"
        pattern = r'"?([^":]+)"?\s*:\s*"?([^",]+)"?'
        matches = re.findall(pattern, text)
        
        for key, value in matches:
            result[key.strip()] = value.strip()
        
        if not result:
            # Fallback: tạo một đối tượng JSON đơn giản với toàn bộ text
            result = {"raw_text": text}
        
        logger.debug(f"Created fallback dict with {len(result)} entries")
        return result