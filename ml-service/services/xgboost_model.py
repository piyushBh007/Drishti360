import os
import json
import logging
from typing import Dict, Any, Tuple
import numpy as np

logger = logging.getLogger("XGBoost_Model")

MODEL_DIR = os.path.join(os.path.dirname(__file__), "..", "models", "xgboost")
MODEL_FILE_PATH = os.path.join(MODEL_DIR, "model.json")
SCHEMA_FILE_PATH = os.path.join(MODEL_DIR, "feature_schema.json")
METADATA_FILE_PATH = os.path.join(MODEL_DIR, "metadata.json")

class XGBoostRiskModel:
    """
    Supervised XGBoost Risk Model inference wrapper.
    Evaluates NGO feature vectors to predict risk scores (0-100).
    Requires a trained model artifact generated from legitimate labeled historical data.
    """
    def __init__(self, model_dir: str = MODEL_DIR):
        self.model_dir = model_dir
        self.model_path = os.path.join(model_dir, "model.json")
        self.schema_path = os.path.join(model_dir, "feature_schema.json")
        self.metadata_path = os.path.join(model_dir, "metadata.json")
        self.booster = None
        self.feature_names = []
        self.version = "1.0.0"
        self.is_loaded = False
        self._load_model_if_available()

    def _load_model_if_available(self):
        if not os.path.exists(self.model_path):
            logger.info(f"[XGBoost] Model artifact not found at: {self.model_path}")
            logger.info("[XGBoost] Production model training requires labeled historical inspection/risk data.")
            self.is_loaded = False
            return

        try:
            import xgboost as xgb
            booster = xgb.Booster()
            booster.load_model(self.model_path)
            self.booster = booster

            if os.path.exists(self.schema_path):
                with open(self.schema_path, "r") as f:
                    schema_data = json.load(f)
                    self.feature_names = schema_data.get("featureNames", [])

            if os.path.exists(self.metadata_path):
                with open(self.metadata_path, "r") as f:
                    meta_data = json.load(f)
                    self.version = meta_data.get("version", "1.0.0")

            self.is_loaded = True
            logger.info(f"[XGBoost] Successfully loaded model v{self.version} from {self.model_path}")

        except Exception as e:
            logger.error(f"[XGBoost Error] Failed to load model artifact: {e}")
            self.is_loaded = False

    def predict(self, feature_dict: Dict[str, Any]) -> Dict[str, Any]:
        """
        Executes XGBoost inference on feature vector.
        Raises FileNotFoundError if trained model artifact is not available.
        """
        if not self.is_loaded or self.booster is None:
            err_msg = "XGBoost model artifact unavailable. Production training requires labeled historical dataset."
            logger.warning(f"[XGBoost] {err_msg}")
            raise FileNotFoundError(err_msg)

        import xgboost as xgb

        # Order features according to feature schema
        if not self.feature_names:
            self.feature_names = list(feature_dict.keys())

        vector = [float(feature_dict.get(fn, 0.0)) for fn in self.feature_names]
        dmatrix = xgb.DMatrix(np.array([vector]), feature_names=self.feature_names)

        # Run inference
        raw_pred = self.booster.predict(dmatrix)[0]
        score = float(np.clip(raw_pred, 0.0, 100.0))
        score_rounded = round(score, 1)

        # Classify risk level based on standard thresholds
        if score_rounded >= 70.0:
            level = "HIGH"
        elif score_rounded >= 40.0:
            level = "MEDIUM"
        else:
            level = "LOW"

        # Generate risk factors breakdown from feature values
        factors = self._generate_factors(feature_dict)

        return {
            "score": score_rounded,
            "riskLevel": level,
            "factors": factors,
            "model": {
                "name": "XGBoostRiskModel",
                "type": "xgboost",
                "version": self.version,
                "isFallback": False
            }
        }

    def _generate_factors(self, f: Dict[str, Any]) -> list:
        factors = []
        cctv_uptime = float(f.get("cctvUptime", 100))
        att_rate = float(f.get("attendanceRate", 100))
        alerts = int(f.get("alertCount", 0))
        days_insp = int(f.get("daysSinceLastInspection", 0))
        yolo_avg = float(f.get("yoloAvgPersonCount", 0))

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
                "description": f"{alerts} unresolved alerts currently open"
            })

        if days_insp > 30:
            factors.append({
                "factor": "Inspection Schedule",
                "title": "Physical audit overdue",
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

        return factors
