import os
import base64
import binascii
import requests
import logging
from datetime import datetime, timezone
from flask import Flask, request, jsonify
from flask_cors import CORS
from dotenv import load_dotenv

from services.yolo_service import analyze_video, analyze_frame, get_yolo_model
from services.risk_service import evaluate_ngo_risk, _xgboost_model

# Load environment variables
load_dotenv()

logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s] %(message)s')
logger = logging.getLogger("ML_App")

app = Flask(__name__)
CORS(app)

PORT = int(os.getenv("PORT", 5001))
YOLO_MODEL = os.getenv("YOLO_MODEL", "yolov8n.pt")
YOLO_CONFIDENCE = float(os.getenv("YOLO_CONFIDENCE", 0.25))
YOLO_INFERENCE_FPS = int(os.getenv("YOLO_INFERENCE_FPS", 3))
BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:3000")

# Media path mappings for local development only.
# In production (Render), video files are not present; /predict will return 404
# for video-based analysis. Real-time /predict-frame is the production path.
ML_SERVICE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(ML_SERVICE_DIR, ".."))
DEFAULT_MEDIA_PATHS = {
    "camera-001": os.path.join(ML_SERVICE_DIR, "..", "backend", "media", "cctv_demo.mp4"),
    "camera-002": os.path.join(ML_SERVICE_DIR, "..", "backend", "media", "cctv_demo_2.mp4")
}

# Pre-warm models on service startup
try:
    get_yolo_model(YOLO_MODEL)
except Exception as e:
    logger.warning(f"[Startup Warning] Pre-warming YOLO model failed: {e}")

@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "healthy",
        "service": "Drishti360 ML Service (YOLO + XGBoost)",
        "yoloModel": {
            "name": YOLO_MODEL,
            "confidenceThreshold": YOLO_CONFIDENCE,
            "inferenceFps": YOLO_INFERENCE_FPS
        },
        "xgboostModel": {
            "isLoaded": _xgboost_model.is_loaded,
            "version": _xgboost_model.version,
            "modelPath": _xgboost_model.model_path,
            "status": "loaded" if _xgboost_model.is_loaded else "unavailable (requires labeled dataset for training)"
        }
    }), 200

@app.route("/predict", methods=["POST"])
def predict():
    data = request.get_json() or {}
    camera_id = data.get("cameraId", "camera-001")
    ngo_id = data.get("ngoId", "ngo-001")
    video_path = data.get("videoPath")
    post_to_backend = data.get("postToBackend", True)

    # Check offline status for camera-003
    if camera_id.lower() in ["camera-003", "cam-003"]:
        return jsonify({
            "error": "Camera camera-003 is OFFLINE. Cannot run YOLO inference on offline camera."
        }), 400

    # Resolve default video path if not explicitly provided
    if not video_path:
        video_path = DEFAULT_MEDIA_PATHS.get(camera_id.lower())
        if not video_path or not os.path.exists(video_path):
            filename = "cctv_demo.mp4" if "001" in camera_id else "cctv_demo_2.mp4"
            video_path = os.path.join(PROJECT_ROOT, "app", "src", "main", "res", "raw", filename)

    if not video_path or not os.path.exists(video_path):
        return jsonify({
            "error": f"Video file not accessible for camera {camera_id} at: {video_path}"
        }), 404

    confidence = float(data.get("confidence", YOLO_CONFIDENCE))
    fps = int(data.get("fps", YOLO_INFERENCE_FPS))

    try:
        result = analyze_video(
            video_path=video_path,
            camera_id=camera_id,
            ngo_id=ngo_id,
            model_name=YOLO_MODEL,
            confidence_threshold=confidence,
            target_fps=fps
        )

        backend_status = None
        if post_to_backend:
            try:
                backend_endpoint = f"{BACKEND_URL}/api/ml/activity"
                logger.info(f"[ML App] Forwarding aggregated activity to backend: {backend_endpoint}")
                resp = requests.post(backend_endpoint, json=result, timeout=5)
                if resp.status_code in [200, 201]:
                    backend_status = "persisted_to_postgresql"
                    logger.info("[ML App] Successfully persisted activity to backend PostgreSQL.")
                else:
                    backend_status = f"backend_returned_{resp.status_code}"
            except Exception as b_err:
                logger.warning(f"[ML App Warning] Backend communication failed: {b_err}")
                backend_status = "backend_unreachable"

        result["backendPersistence"] = backend_status
        return jsonify(result), 200

    except Exception as e:
        logger.error(f"[ML App Error] Prediction error: {str(e)}")
        return jsonify({
            "error": "YOLO inference execution failed",
            "details": str(e)
        }), 500

@app.route("/predict-frame", methods=["POST"])
def predict_frame():
    """
    Real-time single-frame person detection.
    Body: {"imageBase64": "<jpeg base64, optionally data-URI prefixed>", "confidence": 0.4 (optional)}
    Uses ONLY the supplied frame. No DB, no history, no seeded values.
    """
    import time
    start_time = time.time()
    
    data = request.get_json(silent=True) or {}
    image_b64 = data.get("imageBase64")
    if not image_b64 or not isinstance(image_b64, str):
        return jsonify({"error": "imageBase64 is required"}), 400

    if image_b64.startswith("data:") and "," in image_b64:
        image_b64 = image_b64.split(",", 1)[1]

    try:
        image_bytes = base64.b64decode(image_b64, validate=False)
    except (binascii.Error, ValueError):
        return jsonify({"error": "imageBase64 is not valid base64"}), 400

    try:
        confidence = float(data.get("confidence", YOLO_CONFIDENCE))
    except (TypeError, ValueError):
        return jsonify({"error": "confidence must be a number"}), 400

    logger.info(f"[ML App] /predict-frame received {len(image_bytes)} bytes, conf={confidence}")

    try:
        result = analyze_frame(image_bytes, model_name=YOLO_MODEL, confidence_threshold=confidence)
    except ValueError as e:
        return jsonify({"error": str(e)}), 400
    except Exception as e:
        logger.error(f"[ML App Error] predict-frame failed: {e}")
        return jsonify({"error": "YOLO frame inference failed", "details": str(e)}), 500

    result["timestamp"] = datetime.now(timezone.utc).isoformat()
    result["source"] = "live-frame"
    
    total_ms = int((time.time() - start_time) * 1000)
    logger.info(f"[ML App] Total ML request time: {total_ms}ms")
    
    return jsonify(result), 200

@app.route("/predict/risk", methods=["POST"])
@app.route("/risk", methods=["POST"])
def predict_risk():
    """
    Evaluates NGO Risk Score using Feature Engineering and XGBoost / Fallback Risk Model.
    """
    data = request.get_json() or {}
    ngo_data = data.get("ngoData") or data.get("features") or data
    yolo_data = data.get("yoloData")

    if not ngo_data or not isinstance(ngo_data, dict):
        return jsonify({"error": "Valid NGO feature payload required"}), 400

    try:
        risk_result = evaluate_ngo_risk(ngo_data, yolo_data)
        return jsonify(risk_result), 200
    except Exception as e:
        logger.error(f"[ML App Error] Risk evaluation error: {str(e)}")
        return jsonify({
            "error": "Risk evaluation failed",
            "details": str(e)
        }), 500

if __name__ == "__main__":
    logger.info(f"[ML Service] Starting server on port {PORT}...")
    app.run(host="0.0.0.0", port=PORT, debug=False)
