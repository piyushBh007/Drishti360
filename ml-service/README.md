# Drishti360 ML Service - YOLO Person Detection

The ML Service is a standalone microservice responsible for real object detection and activity aggregation on CCTV video feeds.

## Architecture

- **Engine**: Ultralytics YOLOv8n (pretrained COCO model)
- **Framework**: Python 3.14 + Flask + OpenCV
- **Port**: `5001`
- **Output**: Aggregated person detection metrics (`averagePersonCount`, `maximumPersonCount`, `minimumPersonCount`, `detectionRate`, `framesProcessed`).

## Configuration

Environment variables can be specified in `.env`:

```env
PORT=5001
YOLO_MODEL=yolov8n.pt
YOLO_CONFIDENCE=0.40
YOLO_INFERENCE_FPS=3
BACKEND_URL=http://localhost:5000
```

## Usage

### Health Check

```http
GET /health
```

### Run Inference

```http
POST /predict
Content-Type: application/json

{
  "cameraId": "camera-001",
  "ngoId": "ngo-001",
  "videoPath": "c:\\Drishti360\\backend\\media\\cctv_demo.mp4",
  "postToBackend": true
}
```

The service runs frame-level YOLO inference, calculates aggregated spatial statistics, and posts the results to the Node.js Express backend (`POST /api/ml/activity`), which persists the observation in PostgreSQL (`camera_activity` table).
