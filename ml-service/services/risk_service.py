import logging
import math
from typing import Dict, Any, Optional
from services.feature_service import build_ngo_features
from services.xgboost_model import XGBoostRiskModel

logger = logging.getLogger("Risk_Service")

# Instantiate singleton XGBoost risk model evaluator
_xgboost_model = XGBoostRiskModel()

def calculate_fallback_risk(feature_dict: Dict[str, Any]) -> Dict[str, Any]:
    """
    Operational Fallback Risk Model (Deterministic scoring engine).
    Used when trained XGBoost model artifact is not available.
    """
    cctv_uptime = float(feature_dict.get("cctvUptime", 80.0))
    att_rate = float(feature_dict.get("attendanceRate", 70.0))
    alerts = int(feature_dict.get("alertCount", 0))
    days_insp = int(feature_dict.get("daysSinceLastInspection", 30))
    yolo_avg = float(feature_dict.get("yoloAvgPersonCount", 0.0))

    # 1. CCTV Uptime Risk (Max 30 pts)
    cctv_weight = min(30.0, (100.0 - cctv_uptime) * 0.75)

    # 2. Attendance Anomaly Risk (Max 30 pts)
    att_weight = min(30.0, (100.0 - att_rate) * 0.75)

    # 3. Operational Alerts & Inspection Lag Risk (Max 25 pts)
    alert_weight = min(20.0, alerts * 7.0)
    insp_weight = min(10.0, (days_insp / 60.0) * 10.0)

    # 4. YOLO Object Detection Occupancy Weight (Max 15 pts)
    yolo_weight = 0.0
    if yolo_avg > 0:
        if yolo_avg < 1.0:
            yolo_weight = 10.0
        elif yolo_avg < 2.0:
            yolo_weight = 5.0

    base_risk = 10.0 if (cctv_weight > 5.0 or att_weight > 5.0 or alert_weight > 0) else 0.0

    raw_score = cctv_weight + att_weight + alert_weight + insp_weight + yolo_weight + base_risk
    final_score = int(round(min(99.0, max(10.0, raw_score))))

    if final_score >= 70.0:
        level = "HIGH"
    elif final_score >= 40.0:
        level = "MEDIUM"
    else:
        level = "LOW"

    factors = []
    if cctv_uptime < 85:
        factors.append({
            "factor": "CCTV Uptime",
            "title": "CCTV stream instability",
            "value": f"{round(cctv_uptime)}%",
            "impact": "HIGH" if cctv_uptime < 75 else "MEDIUM",
            "description": f"CCTV uptime recorded at {round(cctv_uptime)}%"
        })

    if att_rate < 80:
        factors.append({
            "factor": "Attendance Rate",
            "title": "Attendance discrepancy",
            "value": f"{round(att_rate)}%",
            "impact": "HIGH" if att_rate < 70 else "MEDIUM",
            "description": f"Beneficiary presence recorded at {round(att_rate)}%"
        })

    if alerts > 0:
        factors.append({
            "factor": "Active Alerts",
            "title": "Unresolved operational alerts",
            "value": f"{alerts} active",
            "impact": "HIGH" if alerts >= 3 else "MEDIUM",
            "description": f"{alerts} unresolved operational alerts open"
        })

    if days_insp > 30:
        factors.append({
            "factor": "Inspection Schedule",
            "title": "Audit overdue",
            "value": f"{days_insp} days",
            "impact": "HIGH" if days_insp > 60 else "MEDIUM",
            "description": f"Last physical inspection was {days_insp} days ago"
        })

    if yolo_avg > 0 and yolo_avg < 2.0:
        factors.append({
            "factor": "YOLO Person Analysis",
            "title": "Low visual occupancy detected",
            "value": f"{yolo_avg} persons avg",
            "impact": "MEDIUM",
            "description": f"YOLO object detection detected average of {yolo_avg} persons during monitoring"
        })

    if not factors:
        factors.append({
            "factor": "General Compliance",
            "title": "Nominal Operations",
            "value": "Optimal",
            "impact": "LOW",
            "description": "All monitoring parameters within standard operational thresholds"
        })

    return {
        "score": final_score,
        "riskLevel": level,
        "factors": factors,
        "model": {
            "name": "FallbackRiskModel",
            "type": "fallback",
            "version": "phase-3b",
            "isFallback": True,
            "message": "XGBoost model artifact unavailable; operational fallback model used."
        }
    }

def evaluate_ngo_risk(ngo_data: Dict[str, Any], yolo_data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    """
    Main risk evaluation handler.
    1. Builds feature vector.
    2. Tries XGBoostRiskModel inference.
    3. Falls back to FallbackRiskModel if XGBoost model artifact is not available.
    """
    feature_payload = build_ngo_features(ngo_data, yolo_data)
    ngo_id = feature_payload["ngoId"]
    ngo_name = feature_payload["ngoName"]
    features = feature_payload["features"]

    prediction = None
    if _xgboost_model.is_loaded:
        try:
            logger.info(f"[RiskService] Running XGBoost prediction for {ngo_id}...")
            prediction = _xgboost_model.predict(features)
        except Exception as e:
            logger.warning(f"[XGBoost Warning] Inference failed for {ngo_id}: {e}")

    if prediction is None:
        logger.info(f"[XGBoost] Model unavailable for {ngo_id}. Using FallbackRiskModel.")
        prediction = calculate_fallback_risk(features)

    return {
        "ngoId": ngo_id,
        "ngoName": ngo_name,
        "riskScore": prediction["score"],
        "riskLevel": prediction["riskLevel"],
        "factors": prediction["factors"],
        "model": prediction["model"],
        "features": features
    }
