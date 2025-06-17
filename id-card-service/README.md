# ID Card Service - CCCD Information Extraction

REST API service để trích xuất thông tin từ thẻ căn cước công dân (CCCD) Việt Nam sử dụng PaddleOCR.

## 🎯 Tính Năng

- ✅ Trích xuất thông tin từ ảnh CCCD (mặt trước và mặt sau)
- ✅ Hỗ trợ định dạng JPG, PNG, PDF
- ✅ Tự động detect và xoay ảnh
- ✅ Làm sạch và chuẩn hóa text tiếng Việt
- ✅ Validation thông tin trích xuất
- ✅ REST API với FastAPI
- ✅ Tối ưu cho CPU (không cần GPU)

## 🚀 Cài Đặt

### Yêu Cầu Hệ Thống
- Python 3.12+
- PDM (Python Dependency Manager)
- RAM: >= 4GB (khuyến nghị 8GB)
- CPU: Multi-core khuyến nghị

### 1. Clone Repository
```bash
git clone <repository-url>
cd id-card-service
```

### 2. Cài Đặt Dependencies
```bash
# Cài đặt PDM nếu chưa có
pip install pdm

# Cài đặt dependencies
pdm install
```

### 3. Cấu Hình Environment
```bash
# Copy file .env mẫu
cp .env.example .env

# Chỉnh sửa cấu hình theo nhu cầu
nano .env
```

### 4. Khởi Động Service
```bash
# Development mode
pdm run python -m src.id_card_service.main

# Hoặc sử dụng uvicorn trực tiếp
pdm run uvicorn src.id_card_service.main:app --host 0.0.0.0 --port 5000 --reload
```

## 📖 API Documentation

### Base URL
```
http://localhost:5000
```

### Endpoints

#### 1. Extract CCCD Information
**POST** `/id/extract`

Upload ảnh CCCD và trích xuất thông tin.

**Parameters:**
- `file`: File ảnh (multipart/form-data)
- `side`: "front" | "back" | "auto" (optional, default: "auto")
- `enhance_image`: boolean (optional, default: true)
- `min_confidence`: float 0.0-1.0 (optional, default: 0.6)

**Response:**
```json
{
  "success": true,
  "data": {
    "full_name": "NGUYỄN VĂN A",
    "id_number": "123456789012",
    "date_of_birth": "01/01/1990",
    "gender": "Nam",
    "hometown": "Hà Nội",
    "permanent_address": "123 Đường ABC, Quận XYZ, Hà Nội",
    "confidence_score": 0.85,
    "processing_time": 3.2,
    "side_detected": "front"
  },
  "errors": [],
  "warnings": ["Low image quality detected"],
  "processing_info": {
    "image_info": {
      "original_size": [1200, 800],
      "operations_applied": ["resize", "contrast_enhancement"]
    },
    "ocr_info": {
      "total_regions": 12,
      "average_confidence": 0.85
    }
  }
}
```

#### 2. Health Check
**GET** `/id/health`

Kiểm tra trạng thái service.

**Response:**
```json
{
  "status": "healthy",
  "ocr_engine": "available",
  "model_loaded": true,
  "memory_usage": "245MB",
  "version": "0.1.0"
}
```

#### 3. Model Information
**GET** `/id/model-info`

Thông tin về OCR models đã load.

## 🧪 Testing với cURL

### Test Extract Endpoint
```bash
curl -X POST "http://localhost:5000/id/extract" \
  -H "accept: application/json" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/cccd-image.jpg" \
  -F "side=auto" \
  -F "enhance_image=true" \
  -F "min_confidence=0.6"
```

### Test Health Check
```bash
curl -X GET "http://localhost:5000/id/health"
```

## ⚙️ Cấu Hình

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DEBUG` | False | Debug mode |
| `HOST` | 0.0.0.0 | Server host |
| `PORT` | 5000 | Server port |
| `OCR_USE_GPU` | False | Use GPU for OCR |
| `OCR_LANG` | vi | OCR language |
| `MIN_CONFIDENCE` | 0.6 | Minimum OCR confidence |
| `MAX_FILE_SIZE` | 10MB | Maximum upload file size |
| `PRELOAD_MODEL` | True | Preload OCR model at startup |

### Tối Ưu Performance

1. **Memory**: Tăng `MODEL_CACHE_SIZE` nếu có đủ RAM
2. **CPU**: Điều chỉnh `MAX_WORKERS` theo số CPU cores
3. **Quality vs Speed**: Giảm `TARGET_WIDTH/HEIGHT` để xử lý nhanh hơn

## 🏗️ Architecture

```
src/id_card_service/
├── api/                 # API routes và schemas
├── core/               # Core OCR engine và image processing
├── processors/         # Text processing và validation
├── models/             # Data models
├── config/             # Configuration
└── main.py            # Application entry point
```

### Component Overview

- **OCREngine**: PaddleOCR wrapper tối ưu cho tiếng Việt
- **ImageProcessor**: Tiền xử lý ảnh (resize, enhance, denoise)
- **TextCleaner**: Làm sạch OCR output
- **FieldExtractor**: Trích xuất từng trường thông tin
- **CCCDValidator**: Validation và chuẩn hóa dữ liệu
- **ModelManager**: Quản lý và cache OCR models

## 📊 Độ Chính Xác Mong Đợi

| Trường Thông Tin | Độ Chính Xác | Ghi Chú |
|------------------|--------------|---------|
| Họ và tên | 85-95% | Phụ thuộc chất lượng ảnh |
| Số CCCD | 90-95% | Định dạng cố định, dễ nhận dạng |
| Ngày sinh | 80-90% | Format DD/MM/YYYY |
| Giới tính | 90-95% | Nam/Nữ |
| Quê quán | 70-85% | Text dài, phức tạp |
| Nơi thường trú | 70-85% | Text dài, phức tạp |

## 🐛 Troubleshooting

### Lỗi Thường Gặp

1. **Model loading failed**
   ```bash
   # Kiểm tra memory available
   free -h
   
   # Restart service
   pdm run python -m src.id_card_service.main
   ```

2. **Low accuracy**
   - Kiểm tra chất lượng ảnh input
   - Tăng độ phân giải ảnh
   - Điều chỉnh `min_confidence`

3. **Slow processing**
   - Giảm `TARGET_WIDTH/HEIGHT`
   - Tắt `enhance_image`
   - Tăng `MAX_WORKERS`

### Debug Mode
```bash
# Bật debug để xem logs chi tiết
export DEBUG=True
export LOG_LEVEL=DEBUG
pdm run python -m src.id_card_service.main
```

## 🔮 Roadmap

### Phase 1 ✅ (Hoàn thành)
- Core OCR pipeline
- Basic field extraction
- REST API endpoints

### Phase 2 🚧 (Đang phát triển)
- Fine-tuning với dataset CCCD
- Batch processing
- Web UI đơn giản

### Phase 3 📋 (Kế hoạch)
- Model optimization
- Microservice deployment
- Performance monitoring

## 📝 License

MIT License - xem file [LICENSE](LICENSE) để biết thêm chi tiết.

## 🤝 Contributing

1. Fork repository
2. Tạo feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Tạo Pull Request

## 📞 Support

- **Documentation**: [docs/PROJECT_ARCHITECTURE.md](docs/PROJECT_ARCHITECTURE.md)
- **Issues**: GitHub Issues
- **Email**: thannong0512@gmail.com

---

**Phiên bản hiện tại**: 0.1.0
**Cập nhật lần cuối**: December 6, 2025
