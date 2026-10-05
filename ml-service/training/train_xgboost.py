import os
import sys
import argparse
import json
import logging
from datetime import datetime

logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s] %(message)s')
logger = logging.getLogger("Train_XGBoost")

REQUIRED_FEATURES = [
    "attendanceRate",
    "cctvUptime",
    "activeCameraCount",
    "totalCameraCount",
    "alertCount",
    "daysSinceLastInspection",
    "yoloAvgPersonCount",
    "yoloMaxPersonCount",
    "yoloMinPersonCount",
    "yoloDetectionRate",
    "yoloObservationCount"
]

def train(data_path: str, output_dir: str, target_col: str = "risk_score"):
    logger.info("==================================================")
    logger.info("Drishti360 Supervised XGBoost Model Training Pipeline")
    logger.info("==================================================")

    if not os.path.exists(data_path):
        logger.error(f"[XGBoost Trainer Error] Labeled training dataset not found at: {data_path}")
        logger.error("[XGBoost Trainer Error] Production model training requires labeled historical inspection/risk data.")
        logger.error("Please supply a valid labeled CSV or JSON dataset containing historical risk evaluations.")
        sys.exit(1)

    try:
        import pandas as pd
        import numpy as np
        from sklearn.model_selection import train_test_split
        from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
        import xgboost as xgb

        # Load dataset
        if data_path.endswith('.json'):
            df = pd.read_json(data_path)
        else:
            df = pd.read_csv(data_path)

        logger.info(f"[XGBoost Trainer] Loaded dataset with {len(df)} rows from {data_path}")

        # Check required columns
        missing_cols = [col for col in REQUIRED_FEATURES if col not in df.columns]
        if missing_cols:
            logger.error(f"[XGBoost Trainer Error] Missing required feature columns in dataset: {missing_cols}")
            sys.exit(1)

        if target_col not in df.columns:
            logger.error(f"[XGBoost Trainer Error] Target column '{target_col}' not found in dataset.")
            sys.exit(1)

        X = df[REQUIRED_FEATURES]
        y = df[target_col]

        logger.info(f"[XGBoost Trainer] Features: {REQUIRED_FEATURES}")
        logger.info(f"[XGBoost Trainer] Target: {target_col}")

        # Split 80/20 train/validation
        X_train, X_val, y_train, y_val = train_test_split(X, y, test_size=0.20, random_state=42)

        logger.info(f"[XGBoost Trainer] Training set: {len(X_train)} samples, Validation set: {len(X_val)} samples")

        # Train XGBoost Regressor
        model = xgb.XGBRegressor(
            n_estimators=100,
            max_depth=4,
            learning_rate=0.05,
            subsample=0.8,
            colsample_bytree=0.8,
            random_state=42
        )

        model.fit(X_train, y_train)

        # Validation evaluation
        val_preds = model.predict(X_val)
        rmse = float(np.sqrt(mean_squared_error(y_val, val_preds)))
        mae = float(mean_absolute_error(y_val, val_preds))
        r2 = float(r2_score(y_val, val_preds))

        logger.info("--------------------------------------------------")
        logger.info("Validation Results:")
        logger.info(f"  RMSE: {rmse:.4f}")
        logger.info(f"  MAE:  {mae:.4f}")
        logger.info(f"  R2:   {r2:.4f}")
        logger.info("--------------------------------------------------")

        # Save artifacts
        os.makedirs(output_dir, exist_ok=True)
        model_path = os.path.join(output_dir, "model.json")
        schema_path = os.path.join(output_dir, "feature_schema.json")
        metadata_path = os.path.join(output_dir, "metadata.json")

        model.save_model(model_path)

        schema_content = {
            "featureNames": REQUIRED_FEATURES,
            "targetColumn": target_col
        }
        with open(schema_path, "w") as f:
            json.dump(schema_content, f, indent=2)

        metadata_content = {
            "name": "Drishti360 XGBoost Risk Model",
            "version": "1.0.0",
            "trainedAt": datetime.utcnow().isoformat() + "Z",
            "datasetSize": len(df),
            "trainingSamples": len(X_train),
            "validationSamples": len(X_val),
            "metrics": {
                "rmse": round(rmse, 4),
                "mae": round(mae, 4),
                "r2": round(r2, 4)
            },
            "featureNames": REQUIRED_FEATURES
        }
        with open(metadata_path, "w") as f:
            json.dump(metadata_content, f, indent=2)

        logger.info(f"[XGBoost Trainer] Model artifact saved to: {model_path}")
        logger.info(f"[XGBoost Trainer] Feature schema saved to: {schema_path}")
        logger.info(f"[XGBoost Trainer] Metadata saved to: {metadata_path}")
        logger.info("==================================================")
        logger.info("XGBoost Model Training Completed Successfully.")

    except Exception as e:
        logger.error(f"[XGBoost Trainer Error] Training failed: {e}")
        sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Train XGBoost Risk Model for Drishti360")
    parser.add_argument("--data", type=str, required=True, help="Path to labeled historical CSV/JSON dataset")
    parser.add_argument("--output", type=str, default="../models/xgboost", help="Directory to save model artifacts")
    parser.add_argument("--target", type=str, default="risk_score", help="Target column name")
    args = parser.parse_args()

    train(args.data, args.output, args.target)
