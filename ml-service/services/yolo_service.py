import os
import time
import math
import cv2
import logging
from typing import Dict, Any, List
from ultralytics import YOLO

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s] %(message)s')
logger = logging.getLogger("YOLO_Service")

# Global model instance cache
_model_cache: Dict[str, YOLO] = {}

def get_yolo_model(model_name: str = "yolov8n.pt") -> YOLO:
    """
    Loads and caches the pretrained YOLO model.
    """
    global _model_cache
    if model_name not in _model_cache:
        logger.info(f"[YOLO] Loading pretrained model: {model_name}...")
        try:
            model = YOLO(model_name)
            _model_cache[model_name] = model
            logger.info(f"[YOLO] Model loaded successfully: {model_name}")
        except Exception as e:
            logger.error(f"[YOLO Error] Failed to load model '{model_name}': {str(e)}")
            raise RuntimeError(f"YOLO model loading failed: {str(e)}")
    return _model_cache[model_name]

def analyze_video(
    video_path: str,
    camera_id: str = "camera-001",
    ngo_id: str = "ngo-001",
    model_name: str = "yolov8n.pt",
    confidence_threshold: float = 0.40,
    target_fps: int = 3
) -> Dict[str, Any]:
    """
    Performs real YOLO person detection on sampled frames of a video file.
    Aggregates frame-level detection data into structured activity metadata.
    """
    start_time = time.time()
    logger.info(f"[YOLO] Starting inference for {camera_id} on video: {video_path}")

    if not os.path.exists(video_path):
        err_msg = f"Video file not found at path: {video_path}"
        logger.error(f"[YOLO Error] {err_msg}")
        raise FileNotFoundError(err_msg)

    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        err_msg = f"Failed to open video file: {video_path}"
        logger.error(f"[YOLO Error] {err_msg}")
        raise ValueError(err_msg)

    # Extract video properties
    video_fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
    duration_sec = total_frames / video_fps if video_fps > 0 else 0.0

    # Calculate frame step for target inference FPS
    frame_step = max(1, int(round(video_fps / target_fps))) if target_fps > 0 else 1

    model = get_yolo_model(model_name)

    frame_index = 0
    frames_processed = 0
    person_counts: List[int] = []
    frames_with_persons = 0
    sample_detections: List[Dict[str, Any]] = []

    logger.info(f"[YOLO] Video FPS: {video_fps:.2f}, Total Frames: {total_frames}, Sampling Step: {frame_step}")

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        # Process frame at sampled interval
        if frame_index % frame_step == 0:
            frames_processed += 1

            # Run YOLO inference with optimized parameters for CCTV
            results = model(frame, conf=confidence_threshold, imgsz=1280, iou=0.50, max_det=100, classes=[0], verbose=False)

            frame_persons = 0
            frame_bboxes = []

            for result in results:
                boxes = result.boxes
                if boxes is not None:
                    for box in boxes:
                        cls_id = int(box.cls[0].item())
                        conf = float(box.conf[0].item())

                        # Class ID 0 is 'person' in COCO dataset
                        if cls_id == 0 and conf >= confidence_threshold:
                            frame_persons += 1
                            xyxy = box.xyxy[0].tolist()
                            bbox = [round(v, 2) for v in xyxy]
                            frame_bboxes.append({
                                "class": "person",
                                "confidence": round(conf, 4),
                                "bbox": bbox
                            })

            person_counts.append(frame_persons)
            if frame_persons > 0:
                frames_with_persons += 1

            # Store sample detection for initial processed frames
            if len(sample_detections) < 3 and frame_bboxes:
                sample_detections.append({
                    "frameIndex": frame_index,
                    "personCount": frame_persons,
                    "detections": frame_bboxes
                })

        frame_index += 1

    cap.release()

    processing_time_sec = round(time.time() - start_time, 2)

    if not person_counts:
        person_counts = [0]

    avg_persons = round(sum(person_counts) / len(person_counts), 2)
    max_persons = max(person_counts)
    min_persons = min(person_counts)
    detection_rate = round(frames_with_persons / len(person_counts), 4)

    logger.info(f"[YOLO] Processing completed for {camera_id}")
    logger.info(f"[YOLO] Frames processed: {frames_processed}")
    logger.info(f"[YOLO] Average persons: {avg_persons}")
    logger.info(f"[YOLO] Maximum persons: {max_persons}")
    logger.info(f"[YOLO] Minimum persons: {min_persons}")
    logger.info(f"[YOLO] Detection rate: {detection_rate * 100:.1f}%")
    logger.info(f"[YOLO] Processing time: {processing_time_sec} seconds")

    return {
        "cameraId": camera_id,
        "ngoId": ngo_id,
        "modelName": "YOLOv8n",
        "modelVersion": "8.0",
        "observationDurationSeconds": round(duration_sec, 2),
        "framesProcessed": frames_processed,
        "averagePersonCount": avg_persons,
        "maximumPersonCount": max_persons,
        "minimumPersonCount": min_persons,
        "detectionRate": detection_rate,
        "processingTimeSeconds": processing_time_sec,
        "sampleDetections": sample_detections
    }


def analyze_frame(
    image_bytes: bytes,
    model_name: str = "yolov8n.pt",
    confidence_threshold: float = 0.40
) -> Dict[str, Any]:
    """
    Runs YOLOv8 on ONE decoded JPEG/PNG frame and returns person detections only.
    Nothing here reads the database, seeded data or historical activity.
    bbox values are pixel coordinates [x1, y1, x2, y2] in the decoded image of
    size imageWidth x imageHeight.
    """
    import numpy as np  # local import keeps module import light

    start = time.time()
    if not image_bytes:
        raise ValueError("Empty image payload")

    arr = np.frombuffer(image_bytes, dtype=np.uint8)
    frame = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if frame is None:
        raise ValueError("Image payload could not be decoded as JPEG/PNG")

    height, width = frame.shape[:2]
    model = get_yolo_model(model_name)

    # classes=[0] restricts YOLO output to COCO 'person' only.
    # imgsz=1280 improves detection of small/distant people in CCTV frames.
    # iou=0.50 prevents merging nearby people.
    results = model(frame, conf=confidence_threshold, imgsz=1280, iou=0.50, max_det=100, classes=[0], verbose=False)

    detections: List[Dict[str, Any]] = []
    for result in results:
        boxes = result.boxes
        if boxes is None:
            continue
        for box in boxes:
            cls_id = int(box.cls[0].item())
            conf = float(box.conf[0].item())
            if cls_id != 0 or conf < confidence_threshold:
                continue
            x1, y1, x2, y2 = [round(float(v), 2) for v in box.xyxy[0].tolist()]
            detections.append({
                "class": "person",
                "confidence": round(conf, 4),
                "bbox": [x1, y1, x2, y2]
            })

    detections.sort(key=lambda d: d["confidence"], reverse=True)
    inference_ms = int((time.time() - start) * 1000)
    highest = detections[0]["confidence"] if detections else 0.0

    logger.info(
        f"[YOLO-FRAME] image={width}x{height} conf>={confidence_threshold} "
        f"persons={len(detections)} highestConf={highest} time={inference_ms}ms"
    )

    return {
        "personCount": len(detections),
        "highestConfidence": highest,
        "detections": detections,
        "imageWidth": width,
        "imageHeight": height,
        "modelName": "YOLOv8n" if "v8n" in model_name or "yolov8n" in model_name else model_name,
        "confidenceThreshold": confidence_threshold,
        "inferenceMs": inference_ms
    }
