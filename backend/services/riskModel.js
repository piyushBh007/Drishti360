const http = require('http');
const https = require('https');

/**
 * Abstract RiskModel Interface / Base Class
 */
class RiskModel {
  predict(features) {
    throw new Error('RiskModel.predict must be implemented by concrete subclass');
  }
}

/**
 * FallbackRiskModel for Phase 3A/3B
 * Deterministic heuristic scoring engine used when XGBoost model is unavailable or un-trained
 */
class FallbackRiskModel extends RiskModel {
  constructor() {
    super();
    this.version = 'phase-3b';
    this.modelType = 'fallback';
    this.name = 'FallbackRiskModel';
  }

  predict(features) {
    const {
      attendanceRate = 70,
      cctvUptime = 80,
      alertCount = 0,
      daysSinceLastInspection = 30,
      yoloAvgPersonCount = 0
    } = features;

    // Calculate component risk weights (Scale: 0 to 100)
    // 1. CCTV Uptime Risk Weight (Max 30 pts)
    const cctvRiskWeight = Math.min(30, (100 - cctvUptime) * 0.75);

    // 2. Attendance Anomaly Risk Weight (Max 30 pts)
    const attendanceRiskWeight = Math.min(30, (100 - attendanceRate) * 0.75);

    // 3. Operational & Inspection Alert Risk Weight (Max 25 pts)
    const alertRiskWeight = Math.min(20, alertCount * 7);
    const inspectionRiskWeight = Math.min(10, (daysSinceLastInspection / 60) * 10);

    // 4. YOLO Object Detection Occupancy Weight (Max 15 pts)
    let yoloRiskWeight = 0;
    if (yoloAvgPersonCount > 0) {
      if (yoloAvgPersonCount < 1.0) {
        yoloRiskWeight = 10;
      } else if (yoloAvgPersonCount < 2.0) {
        yoloRiskWeight = 5;
      }
    }

    const baseRisk = (cctvRiskWeight > 5 || attendanceRiskWeight > 5 || alertRiskWeight > 0) ? 10 : 0;

    // Total Score calculation
    const rawScore = cctvRiskWeight + attendanceRiskWeight + alertRiskWeight + inspectionRiskWeight + yoloRiskWeight + baseRisk;
    const finalScore = Math.min(99, Math.max(10, Math.round(rawScore)));

    // Risk Level
    let level = 'LOW';
    if (finalScore >= 70) {
      level = 'HIGH';
    } else if (finalScore >= 40) {
      level = 'MEDIUM';
    }

    // Risk Factors Breakdown
    const factors = [];

    if (cctvUptime < 85) {
      const cctvImpact = cctvUptime < 75 ? 'HIGH' : 'MEDIUM';
      factors.push({
        factor: 'CCTV Uptime',
        title: 'CCTV instability',
        value: `${Math.round(cctvUptime)}%`,
        impact: cctvImpact,
        scoreContribution: Math.round(cctvRiskWeight),
        description: `Stream uptime at ${Math.round(cctvUptime)}% with disconnect events`
      });
    }

    if (attendanceRate < 80) {
      const attImpact = attendanceRate < 70 ? 'HIGH' : 'MEDIUM';
      factors.push({
        factor: 'Attendance Rate',
        title: 'Attendance anomaly',
        value: `${Math.round(attendanceRate)}%`,
        impact: attImpact,
        scoreContribution: Math.round(attendanceRiskWeight),
        description: `Beneficiary presence recorded at ${Math.round(attendanceRate)}%`
      });
    }

    if (alertCount > 0) {
      const alertImpact = alertCount >= 3 ? 'HIGH' : 'MEDIUM';
      factors.push({
        factor: 'Active Alerts',
        title: 'Unresolved Alerts',
        value: `${alertCount} active`,
        impact: alertImpact,
        scoreContribution: Math.round(alertRiskWeight),
        description: `${alertCount} active operational alerts logged`
      });
    }

    if (daysSinceLastInspection > 30) {
      factors.push({
        factor: 'Inspection Schedule',
        title: 'Audit Overdue',
        value: `${daysSinceLastInspection} days`,
        impact: daysSinceLastInspection > 60 ? 'HIGH' : 'MEDIUM',
        scoreContribution: Math.round(inspectionRiskWeight),
        description: `Last physical inspection was ${daysSinceLastInspection} days ago`
      });
    }

    if (yoloAvgPersonCount > 0 && yoloAvgPersonCount < 2.0) {
      factors.push({
        factor: 'YOLO Person Analysis',
        title: 'Low Visual Occupancy',
        value: `${yoloAvgPersonCount} persons avg`,
        impact: 'MEDIUM',
        scoreContribution: Math.round(yoloRiskWeight),
        description: `YOLO object detection observed average of ${yoloAvgPersonCount} persons`
      });
    }

    if (factors.length === 0) {
      factors.push({
        factor: 'General Compliance',
        title: 'Nominal Operations',
        value: 'Optimal',
        impact: 'LOW',
        scoreContribution: 0,
        description: 'All monitoring parameters within standard operational thresholds'
      });
    }

    return {
      score: finalScore,
      riskLevel: level,
      factors: factors,
      model: {
        name: this.name,
        type: this.modelType,
        version: this.version,
        isFallback: true
      }
    };
  }
}

/**
 * XGBoostRiskModel
 * Connects Node.js backend to Python ML Service XGBoost Risk Evaluation Endpoint.
 * Automatically falls back to FallbackRiskModel if Python ML service or model file is unavailable.
 */
class XGBoostRiskModel extends RiskModel {
  constructor() {
    super();
    this.name = 'XGBoostRiskModel';
    this.fallbackModel = new FallbackRiskModel();
    this.mlServiceUrl = process.env.ML_SERVICE_URL || 'http://localhost:5001';
  }

  async predictAsync(features) {
    try {
      const payload = JSON.stringify({ ngoData: features });
      const url = new URL(`${this.mlServiceUrl}/predict/risk`);

      const isHttps = url.protocol === 'https:';
      const transport = isHttps ? https : http;

      const response = await new Promise((resolve, reject) => {
        const req = transport.request(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(payload)
          },
          timeout: 5000
        }, (res) => {
          let data = '';
          res.on('data', chunk => data += chunk);
          res.on('end', () => {
            if (res.statusCode >= 200 && res.statusCode < 300) {
              try {
                resolve(JSON.parse(data));
              } catch (e) {
                reject(e);
              }
            } else {
              reject(new Error(`ML service returned status ${res.statusCode}`));
            }
          });
        });
        req.on('error', (e) => reject(e));
        req.on('timeout', () => { req.destroy(); reject(new Error('ML service timeout')); });
        req.write(payload);
        req.end();
      });

      if (response && response.riskScore !== undefined) {
        return {
          score: response.riskScore,
          riskLevel: response.riskLevel || 'MEDIUM',
          factors: response.factors || [],
          model: response.model || { name: 'XGBoostRiskModel', type: 'xgboost', version: '1.0.0', isFallback: false }
        };
      }
    } catch (err) {
      console.log(`[XGBoost] ML service prediction unavailable (${err.message}). Using FallbackRiskModel.`);
    }

    return this.fallbackModel.predict(features);
  }

  predict(features) {
    return this.fallbackModel.predict(features);
  }
}

const activeRiskModel = new XGBoostRiskModel();

module.exports = {
  RiskModel,
  FallbackRiskModel,
  XGBoostRiskModel,
  activeRiskModel
};
