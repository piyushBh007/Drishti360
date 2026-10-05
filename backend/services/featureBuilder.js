const db = require('../db/connection');
const fs = require('fs');
const path = require('path');

const DATA_DIR = path.join(__dirname, '..', 'data');

function readJsonFile(filename) {
  try {
    const filePath = path.join(DATA_DIR, filename);
    if (!fs.existsSync(filePath)) return [];
    return JSON.parse(fs.readFileSync(filePath, 'utf8'));
  } catch (err) {
    return [];
  }
}

async function buildFeatures(ngoId) {
  let ngo = null;
  let cameras = [];
  let alerts = [];
  let inspections = [];

  try {
    const ngoRes = await db.query('SELECT * FROM ngos WHERE LOWER(id) = LOWER($1)', [ngoId]);
    if (ngoRes.rows.length > 0) {
      ngo = ngoRes.rows[0];
    }
    const camRes = await db.query('SELECT * FROM cameras WHERE LOWER(ngo_id) = LOWER($1)', [ngoId]);
    cameras = camRes.rows;

    const alertRes = await db.query('SELECT * FROM alerts WHERE LOWER(ngo_id) = LOWER($1) AND UPPER(status) = $2', [ngoId, 'OPEN']);
    alerts = alertRes.rows;

    const inspRes = await db.query('SELECT * FROM inspections WHERE LOWER(ngo_id) = LOWER($1) ORDER BY created_at DESC', [ngoId]);
    inspections = inspRes.rows;
  } catch (err) {
    console.warn(`[FeatureBuilder] DB Query fallback for ${ngoId}:`, err.message);
  }

  // Fallback to JSON files if DB record not found
  if (!ngo) {
    const ngosJson = readJsonFile('ngos.json');
    ngo = ngosJson.find(n => n.id && n.id.toLowerCase() === ngoId.toLowerCase());
  }

  if (!ngo) {
    throw new Error(`NGO not found for ID: ${ngoId}`);
  }

  if (cameras.length === 0) {
    const camsJson = readJsonFile('cameras.json');
    cameras = camsJson.filter(c => (c.ngoId || c.ngo_id || '').toLowerCase() === ngoId.toLowerCase());
  }

  if (alerts.length === 0 && !ngo.activeAlertsCount) {
    const alertsJson = readJsonFile('alerts.json');
    alerts = alertsJson.filter(a => (a.ngoId || a.ngo_id || '').toLowerCase() === ngoId.toLowerCase() && (a.status || 'OPEN').toUpperCase() === 'OPEN');
  }

  if (inspections.length === 0) {
    const inspJson = readJsonFile('inspections.json');
    inspections = inspJson.filter(i => (i.ngoId || i.ngo_id || '').toLowerCase() === ngoId.toLowerCase());
  }

  // Extract raw metrics
  const attendanceRate = parseFloat(ngo.attendance_rate || ngo.attendanceRate || 70.0);
  const cctvTotalCount = cameras.length > 0 ? cameras.length : (ngo.cctv_total || ngo.cctvTotalCount || 3);
  const activeCameraCount = cameras.length > 0
    ? cameras.filter(c => (c.status || '').toUpperCase() === 'ONLINE').length
    : (ngo.cctv_online || ngo.cctvOnlineCount || 0);

  const cctvUptime = ngo.uptime_percent !== undefined && ngo.uptime_percent !== null
    ? parseFloat(ngo.uptime_percent)
    : (ngo.networkUptime !== undefined ? parseFloat(ngo.networkUptime) : (cctvTotalCount > 0 ? (activeCameraCount / cctvTotalCount) * 100 : 0));

  const alertCount = alerts.length > 0 ? alerts.length : (ngo.alerts_count || ngo.activeAlertsCount || 0);
  const inspectionCount = inspections.length;

  let lastInspectionDate = ngo.last_inspection || ngo.lastInspectionDate || null;
  if (inspections.length > 0 && inspections[0].created_at) {
    lastInspectionDate = inspections[0].created_at;
  }

  let daysSinceLastInspection = 45;
  if (lastInspectionDate) {
    const lastDate = new Date(lastInspectionDate);
    if (!isNaN(lastDate.getTime())) {
      const diffMs = Date.now() - lastDate.getTime();
      daysSinceLastInspection = Math.max(0, Math.floor(diffMs / (1000 * 60 * 60 * 24)));
    }
  }

  // Fetch real YOLO activity metadata from PostgreSQL camera_activity table
  let yoloAvgPersonCount = 0.0;
  let yoloMaxPersonCount = 0;
  let yoloMinPersonCount = 0;
  let yoloDetectionRate = 0.0;
  let yoloObservationCount = 0;

  try {
    const yoloRes = await db.query(
      `SELECT 
        COALESCE(AVG(average_person_count), 0) as avg_persons,
        COALESCE(MAX(maximum_person_count), 0) as max_persons,
        COALESCE(MIN(minimum_person_count), 0) as min_persons,
        COALESCE(AVG(detection_rate), 0) as avg_detection_rate,
        COUNT(*) as record_count
       FROM camera_activity 
       WHERE LOWER(ngo_id) = LOWER($1)`,
      [ngoId]
    );
    if (yoloRes.rows.length > 0 && parseInt(yoloRes.rows[0].record_count, 10) > 0) {
      const row = yoloRes.rows[0];
      yoloAvgPersonCount = parseFloat(parseFloat(row.avg_persons).toFixed(2));
      yoloMaxPersonCount = parseInt(row.max_persons, 10);
      yoloMinPersonCount = parseInt(row.min_persons, 10);
      yoloDetectionRate = parseFloat(parseFloat(row.avg_detection_rate).toFixed(4));
      yoloObservationCount = parseInt(row.record_count, 10);
    }
  } catch (yoloErr) {
    console.warn(`[FeatureBuilder] Failed to fetch YOLO activity for ${ngoId}:`, yoloErr.message);
  }

  // Derived normalized features (0.0 to 1.0)
  const networkIssueScore = Math.min(1.0, Math.max(0.0, (100.0 - cctvUptime) / 100.0));
  const attendanceAnomalyScore = Math.min(1.0, Math.max(0.0, (100.0 - attendanceRate) / 100.0));
  const documentationIssueScore = Math.min(1.0, Math.max(0.0, daysSinceLastInspection / 90.0));

  return {
    ngoId: ngo.id,
    ngoName: ngo.name || ngo.ngoName || 'NGO Entity',
    attendanceRate: attendanceRate,
    cctvUptime: cctvUptime,
    activeCameraCount: activeCameraCount,
    totalCameraCount: cctvTotalCount,
    alertCount: alertCount,
    inspectionCount: inspectionCount,
    daysSinceLastInspection: daysSinceLastInspection,
    networkIssueScore: Math.round(networkIssueScore * 100) / 100,
    attendanceAnomalyScore: Math.round(attendanceAnomalyScore * 100) / 100,
    documentationIssueScore: Math.round(documentationIssueScore * 100) / 100,
    // Real YOLO Object Detection Metadata from camera_activity table
    yoloAvgPersonCount: yoloAvgPersonCount,
    yoloMaxPersonCount: yoloMaxPersonCount,
    yoloMinPersonCount: yoloMinPersonCount,
    yoloDetectionRate: yoloDetectionRate,
    yoloObservationCount: yoloObservationCount,
    yolo: {
      averagePersonCount: yoloAvgPersonCount,
      peakPersonCount: yoloMaxPersonCount,
      minPersonCount: yoloMinPersonCount,
      detectionRate: yoloDetectionRate,
      observationCount: yoloObservationCount
    }
  };
}

module.exports = {
  buildFeatures
};
