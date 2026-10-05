require('dotenv').config();
const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const http = require('http');
const https = require('https');
const { WebSocketServer } = require('ws');
const db = require('./db/connection');
const { initializeDatabase } = require('./db/seed');

const riskService = require('./services/riskService');

const app = express();
const PORT = process.env.PORT || 3000;
const server = http.createServer(app);

// CORS — Production: set CORS_ORIGIN to your Android app's origin or '*' only during dev.
// For Render deployment, CORS_ORIGIN is set via environment variable.
const corsOrigin = process.env.CORS_ORIGIN;
app.use(cors(corsOrigin ? {
  origin: corsOrigin.split(',').map(o => o.trim()),
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
} : undefined));
// Frame uploads are base64 JPEGs. Scoped limit; must be registered before the global parser.
app.use('/api/ml/predict-frame', express.json({ limit: '10mb' }));
app.use(express.json());
app.use((err, req, res, next) => {
  if (err.type === 'entity.too.large') {
    return res.status(413).json({ error: 'Image too large for ML inference' });
  }
  next(err);
});

const adminRoutes = require('./routes_admin');
app.use('/api', adminRoutes);

// Health checks — safe to call publicly; never expose secrets here.
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'Drishti360 Backend',
    version: '1.0.0',
    timestamp: new Date().toISOString()
  });
});
app.get('/api/health', (req, res) => res.redirect('/health'));

// Expose static public directory (e.g. GET /call.html)
const PUBLIC_DIR = path.join(__dirname, 'public');
if (!fs.existsSync(PUBLIC_DIR)) {
  fs.mkdirSync(PUBLIC_DIR, { recursive: true });
}
app.use(express.static(PUBLIC_DIR));

// Expose static media directories (e.g. GET /media/ngos/ngo-001.jpg)
const MEDIA_DIR = path.join(__dirname, 'media');
if (!fs.existsSync(MEDIA_DIR)) {
  fs.mkdirSync(MEDIA_DIR, { recursive: true });
}
const NGOS_MEDIA_DIR = path.join(MEDIA_DIR, 'ngos');
if (!fs.existsSync(NGOS_MEDIA_DIR)) {
  fs.mkdirSync(NGOS_MEDIA_DIR, { recursive: true });
}
app.use('/media', express.static(MEDIA_DIR));

const DATA_DIR = path.join(__dirname, 'data');


function readJsonFile(filename) {
  try {
    const filePath = path.join(DATA_DIR, filename);
    if (!fs.existsSync(filePath)) {
      return [];
    }
    const data = fs.readFileSync(filePath, 'utf8');
    return JSON.parse(data);
  } catch (err) {
    console.error(`Error reading ${filename}:`, err);
    return [];
  }
}

function writeJsonFile(filename, data) {
  try {
    const filePath = path.join(DATA_DIR, filename);
    fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf8');
  } catch (err) {
    console.error(`Error writing ${filename}:`, err.message);
  }
}

function mapNgoRowToContract(row, riskDetail = null) {
  const riskScoreVal = riskDetail ? riskDetail.score : (row.risk_score != null ? row.risk_score : null);
  const riskLevelVal = riskDetail ? riskDetail.riskLevel : (row.risk_level || null);
  const factorsVal = (riskDetail && riskDetail.factors && riskDetail.factors.length > 0) ? riskDetail.factors : [];

  return {
    id: row.id,
    name: row.name,
    ngoName: row.name,
    registrationNo: row.registration_no || '',
    category: row.category || 'General',
    establishedYear: row.established_year || null,
    beneficiaries: row.beneficiaries || null,
    description: row.city ? `Works for community development in ${row.city}.` : "Works for community development.",
    image: row.image || `${row.id}.png`,
    images: [row.image || `${row.id}.png`],
    status: row.status || 'ACTIVE',
    location: {
      latitude: parseFloat(row.latitude || 0),
      longitude: parseFloat(row.longitude || 0),
      city: row.city || '',
      state: row.state || '',
      address: row.address || ''
    },
    riskScore: (riskScoreVal != null && riskLevelVal != null) ? {
      score: riskScoreVal,
      level: riskLevelVal,
      factors: factorsVal,
      trend: row.risk_trend || 'STABLE'
    } : null,
    cctvOnlineCount: row.cctv_online != null ? row.cctv_online : null,
    cctvTotalCount: row.cctv_total != null ? row.cctv_total : null,
    attendanceRate: row.attendance_rate != null ? parseFloat(row.attendance_rate) : null,
    networkUptime: row.uptime_percent != null ? parseFloat(row.uptime_percent) : null,
    lastInspectionDate: row.last_inspection ? new Date(row.last_inspection).toISOString().split('T')[0] : null,
    activeAlertsCount: row.alerts_count != null ? row.alerts_count : 0
  };
}

// GET /api/ngos
app.get('/api/ngos', async (req, res) => {
  try {
    const result = await db.query(`SELECT * FROM ngos ORDER BY id ASC`);
    const mapped = result.rows.map(row => mapNgoRowToContract(row));
    return res.json(mapped);
  } catch (err) {
    console.error('[PostgreSQL Fallback] GET /api/ngos failed:', err.message);
    const ngos = readJsonFile('ngos.json');
    return res.json(ngos);
  }
});

// GET /api/ngos/:id
app.get('/api/ngos/:id', async (req, res) => {
  const ngoId = req.params.id;
  try {
    const result = await db.query(`SELECT * FROM ngos WHERE LOWER(id) = LOWER($1)`, [ngoId]);
    if (result.rows.length > 0) {
      // Do NOT invent or auto-calculate risk. Just return the NGO.
      return res.json(mapNgoRowToContract(result.rows[0], null));
    }
  } catch (err) {
    console.error(`[PostgreSQL Fallback] GET /api/ngos/${ngoId} failed:`, err.message);
  }

  const ngos = readJsonFile('ngos.json');
  const ngo = ngos.find(n => n.id.toLowerCase() === ngoId.toLowerCase());
  if (ngo) {
    res.json(ngo);
  } else {
    res.status(404).json({ error: 'NGO not found' });
  }
});

// GET /api/risk/:ngoId
app.get('/api/risk/:ngoId', async (req, res) => {
  const ngoId = req.params.ngoId;
  try {
    const riskResult = await riskService.calculateAndSaveRisk(ngoId);
    return res.json(riskResult);
  } catch (err) {
    console.error(`[API Error] GET /api/risk/${ngoId} failed:`, err.message);
    return res.status(404).json({ error: err.message });
  }
});

// GET /api/risks
app.get('/api/risks', async (req, res) => {
  try {
    const risks = await riskService.getAllRiskScores();
    return res.json(risks);
  } catch (err) {
    console.error('[API Error] GET /api/risks failed:', err.message);
    return res.status(500).json({ error: 'Failed to calculate risk scores' });
  }
});

function sanitizeStreamUrl(url) {
  if (!url || typeof url !== 'string') return null;
  return url.replace(/(rtsp|rtmps?|https?):\/\/([^:@]+):([^@]+)@/gi, '$1://***:***@');
}

// GET /api/cameras
app.get('/api/cameras', async (req, res) => {
  try {
    const result = await db.query(`SELECT * FROM cameras ORDER BY id ASC`);
    if (result.rows.length > 0) {
      const mapped = result.rows.map(row => {
        let streamUrl = row.stream_url;
        let status = row.status || 'OFFLINE';
        let source = row.source || (row.type === 'IP_CAMERA' ? 'RTSP' : 'LOCAL_VIDEO');
        let type = row.type || (source === 'RTSP' ? 'IP_CAMERA' : 'LOCAL_DEMO');
        let protocol = row.protocol || (source === 'RTSP' ? 'RTSP' : 'LOCAL_FILE');

        if ((source === 'RTSP' || type === 'IP_CAMERA') && process.env.RTSP_STREAM_URL) {
          streamUrl = process.env.RTSP_STREAM_URL;
          status = 'ONLINE';
        }

        return {
          id: row.id,
          ngoId: row.ngo_id,
          ngo_id: row.ngo_id,
          name: row.name,
          status: status,
          source: source,
          type: type,
          streamUrl: sanitizeStreamUrl(streamUrl),
          stream_url: sanitizeStreamUrl(streamUrl),
          protocol: protocol,
          mediaPath: row.media_path,
          media_path: row.media_path
        };
      });
      return res.json(mapped);
    }
  } catch (err) {
    console.error('[PostgreSQL Fallback] GET /api/cameras failed:', err.message);
  }

  const cameras = readJsonFile('cameras.json');
  const mappedJson = cameras.map(cam => {
    let streamUrl = cam.stream_url || cam.streamUrl || null;
    let status = cam.status || 'OFFLINE';
    let source = cam.source || (cam.type === 'IP_CAMERA' ? 'RTSP' : 'LOCAL_VIDEO');
    let type = cam.type || (source === 'RTSP' ? 'IP_CAMERA' : 'LOCAL_DEMO');
    let protocol = cam.protocol || (source === 'RTSP' ? 'RTSP' : 'LOCAL_FILE');

    if ((source === 'RTSP' || type === 'IP_CAMERA') && process.env.RTSP_STREAM_URL) {
      streamUrl = process.env.RTSP_STREAM_URL;
      status = 'ONLINE';
    }

    return {
      id: cam.id || cam.cameraId,
      ngoId: cam.ngo_id || cam.ngoId,
      ngo_id: cam.ngo_id || cam.ngoId,
      name: cam.name || cam.cameraName,
      status: status,
      source: source,
      type: type,
      streamUrl: sanitizeStreamUrl(streamUrl),
      stream_url: sanitizeStreamUrl(streamUrl),
      protocol: protocol,
      mediaPath: cam.media_path || cam.mediaPath || cam.media || null,
      media_path: cam.media_path || cam.mediaPath || cam.media || null
    };
  });
  res.json(mappedJson);
});

// --- ML YOLO Activity Routes ---

const inMemoryActivityLogs = [];

// POST /api/ml/predict-frame - Real-time YOLO person detection on ONE frame sent by Android.
// Stateless by design: does NOT read or write camera_activity / PostgreSQL / seeded data.
const ML_SERVICE_URL = (process.env.ML_SERVICE_URL || 'http://localhost:5001').replace(/\/+$/, '');
const ML_IS_HTTPS = ML_SERVICE_URL.startsWith('https://');

app.post('/api/ml/predict-frame', (req, res) => {
  console.log(`[FRAME] request received`);
  const reqId = Math.random().toString(36).slice(2, 8);
  const body = req.body || {};
  const imageBase64 = body.imageBase64;

  if (!imageBase64 || typeof imageBase64 !== 'string') {
    console.warn(`[predict-frame ${reqId}] rejected: imageBase64 missing`);
    return res.status(400).json({ error: 'imageBase64 is required' });
  }
  console.log(`[FRAME] image bytes received`);
  console.log(`[predict-frame ${reqId}] received from ${req.ip} cameraId=${body.cameraId || 'n/a'} base64Chars=${imageBase64.length}`);

  const payload = JSON.stringify({
    imageBase64,
    ...(body.confidence !== undefined ? { confidence: body.confidence } : {})
  });
  const target = new URL(`${ML_SERVICE_URL}/predict-frame`);
  const started = Date.now();

  console.log(`[FRAME] sending request to ML service`);
  const httpLib = ML_IS_HTTPS ? https : http;
  const upstream = httpLib.request({
    hostname: target.hostname,
    port: target.port || (ML_IS_HTTPS ? 443 : 80),
    path: target.pathname,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload) },
    timeout: 60000
  }, (upRes) => {
    const chunks = [];
    upRes.on('data', (c) => chunks.push(c));
    upRes.on('end', () => {
      console.log(`[FRAME] ML response received`);
      const raw = Buffer.concat(chunks).toString('utf8');
      let parsed;
      try { parsed = JSON.parse(raw); } catch (_) { parsed = null; }
      if (upRes.statusCode !== 200 || !parsed) {
        const msg = (parsed && parsed.error) || `ML service returned HTTP ${upRes.statusCode}`;
        console.error(`[predict-frame ${reqId}] ML error status=${upRes.statusCode} msg=${msg}`);
        return res.status(502).json({ error: `ML service error: ${msg}` });
      }
      console.log(`[FRAME] returning response to Android`);
      console.log(`[predict-frame ${reqId}] ML ok personCount=${parsed.personCount} highestConf=${parsed.highestConfidence} ${Date.now() - started}ms`);
      res.json(parsed);
    });
  });
  upstream.on('timeout', () => { 
    console.error(`[predict-frame ${reqId}] ML upstream timeout`);
    upstream.destroy(new Error('timeout')); 
  });
  upstream.on('error', (err) => {
    console.error(`[predict-frame ${reqId}] ML unreachable: ${err.message}`);
    if (!res.headersSent) {
      res.status(503).json({ error: `ML service unreachable at ${ML_SERVICE_URL} (${err.message})` });
    }
  });
  
  console.log(`[FRAME] ML request started`);
  upstream.write(payload);
  upstream.end();
});

// POST /api/ml/activity - Store aggregated YOLO activity detection metadata
app.post('/api/ml/activity', async (req, res) => {
  try {
    const {
      cameraId,
      ngoId: inputNgoId,
      observedAt,
      observationDurationSeconds = 0,
      averagePersonCount = 0,
      maximumPersonCount = 0,
      minimumPersonCount = 0,
      framesProcessed = 0,
      detectionRate = 0,
      modelName = 'YOLOv8n',
      modelVersion = '8.0'
    } = req.body;

    if (!cameraId) {
      return res.status(400).json({ error: 'cameraId is required' });
    }

    let ngoId = inputNgoId;
    if (!ngoId) {
      try {
        const camRes = await db.query('SELECT ngo_id FROM cameras WHERE LOWER(id) = LOWER($1)', [cameraId]);
        if (camRes.rows.length > 0) {
          ngoId = camRes.rows[0].ngo_id;
        }
      } catch (err) {
        console.warn('[ML API Warning] Failed to lookup ngo_id for camera:', err.message);
      }
    }

    const timestamp = observedAt ? new Date(observedAt) : new Date();

    try {
      const insertSql = `
        INSERT INTO camera_activity (
          camera_id, ngo_id, observed_at, observation_duration_seconds,
          average_person_count, maximum_person_count, minimum_person_count,
          frames_processed, detection_rate, model_name, model_version
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
        RETURNING *;
      `;

      const result = await db.query(insertSql, [
        cameraId,
        ngoId || 'ngo-001',
        timestamp,
        observationDurationSeconds,
        averagePersonCount,
        maximumPersonCount,
        minimumPersonCount,
        framesProcessed,
        detectionRate,
        modelName,
        modelVersion
      ]);

      const record = result.rows[0];
      console.log(`[ML Activity DB] Persisted YOLO activity for camera ${cameraId}: avg=${averagePersonCount}, max=${maximumPersonCount}`);
      return res.status(201).json({
        success: true,
        data: record
      });
    } catch (dbErr) {
      console.error('[ML Activity DB Error] PostgreSQL insert failed, placing in fallback store:', dbErr.message);
      const fallbackRecord = {
        id: inMemoryActivityLogs.length + 1,
        camera_id: cameraId,
        ngo_id: ngoId || 'ngo-001',
        observed_at: timestamp.toISOString(),
        observation_duration_seconds: parseFloat(observationDurationSeconds),
        average_person_count: parseFloat(averagePersonCount),
        maximum_person_count: parseInt(maximumPersonCount, 10),
        minimum_person_count: parseInt(minimumPersonCount, 10),
        frames_processed: parseInt(framesProcessed, 10),
        detection_rate: parseFloat(detectionRate),
        model_name: modelName,
        model_version: modelVersion
      };
      inMemoryActivityLogs.unshift(fallbackRecord);
      return res.status(201).json({
        success: true,
        fallback: true,
        data: fallbackRecord
      });
    }
  } catch (err) {
    console.error('[ML Activity Error] Failed to process ML activity payload:', err.message);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
});

// GET /api/ml/activity/ngo/:ngoId - Fetch YOLO activity for specific NGO
app.get('/api/ml/activity/ngo/:ngoId', async (req, res) => {
  const { ngoId } = req.params;
  try {
    const result = await db.query(
      `SELECT * FROM camera_activity WHERE LOWER(ngo_id) = LOWER($1) ORDER BY observed_at DESC LIMIT 100`,
      [ngoId]
    );
    if (result.rows.length > 0) {
      return res.json(result.rows);
    }
  } catch (err) {
    console.error('[ML Activity DB Fallback] GET /api/ml/activity/ngo failed:', err.message);
  }
  const filtered = inMemoryActivityLogs.filter(a => (a.ngo_id || '').toLowerCase() === ngoId.toLowerCase());
  return res.json(filtered);
});

// GET /api/ml/activity/:cameraId - Fetch YOLO activity for specific Camera
app.get('/api/ml/activity/:cameraId', async (req, res) => {
  const { cameraId } = req.params;
  try {
    const result = await db.query(
      `SELECT * FROM camera_activity WHERE LOWER(camera_id) = LOWER($1) ORDER BY observed_at DESC LIMIT 50`,
      [cameraId]
    );
    if (result.rows.length > 0) {
      return res.json(result.rows);
    }
  } catch (err) {
    console.error('[ML Activity DB Fallback] GET /api/ml/activity/:cameraId failed:', err.message);
  }
  const filtered = inMemoryActivityLogs.filter(a => (a.camera_id || '').toLowerCase() === cameraId.toLowerCase());
  return res.json(filtered);
});

// GET /api/ml/activity - Fetch recent YOLO activity records across all cameras
app.get('/api/ml/activity', async (req, res) => {
  try {
    const result = await db.query(`SELECT * FROM camera_activity ORDER BY observed_at DESC LIMIT 50`);
    if (result.rows.length > 0) {
      return res.json(result.rows);
    }
  } catch (err) {
    console.error('[ML Activity DB Fallback] GET /api/ml/activity failed:', err.message);
  }
  return res.json(inMemoryActivityLogs);
});

// GET /api/alerts
app.get('/api/alerts', async (req, res) => {
  try {
    const sql = `
      SELECT a.*, n.name AS ngo_name
      FROM alerts a
      LEFT JOIN ngos n ON a.ngo_id = n.id
      ORDER BY a.created_at DESC
    `;
    const result = await db.query(sql);
    if (result.rows.length > 0) {
      const mapped = result.rows.map(row => ({
        id: row.id,
        ngoId: row.ngo_id || 'ngo-001',
        ngoName: row.ngo_name || 'Sunrise Welfare Foundation',
        type: row.type || 'ANOMALY',
        title: `Anomaly Reported: ${row.ngo_name || 'NGO'}`,
        description: row.observation || 'Observation reported',
        severity: 'HIGH',
        timestamp: row.created_at ? new Date(row.created_at).toISOString() : new Date().toISOString(),
        status: row.status || 'OPEN'
      }));
      return res.json(mapped);
    }
  } catch (err) {
    console.error('[PostgreSQL Fallback] GET /api/alerts failed:', err.message);
  }
  const alerts = readJsonFile('alerts.json');
  res.json(alerts);
});

// GET /api/overview
app.get('/api/overview', async (req, res) => {
  try {
    let ngosList = [];
    let inspectionsCount = 0;
    let alertsList = [];

    try {
      const ngoRes = await db.query('SELECT * FROM ngos ORDER BY id ASC');
      if (ngoRes.rows.length > 0) {
        ngosList = ngoRes.rows.map(row => mapNgoRowToContract(row));
      } else {
        ngosList = readJsonFile('ngos.json');
      }
    } catch (e) {
      ngosList = readJsonFile('ngos.json');
    }

    try {
      const inspRes = await db.query('SELECT COUNT(*) AS total FROM inspections');
      if (inspRes.rows.length > 0) {
        inspectionsCount = parseInt(inspRes.rows[0].total, 10);
      } else {
        const inspections = readJsonFile('inspections.json');
        inspectionsCount = inspections.length;
      }
    } catch (e) {
      const inspections = readJsonFile('inspections.json');
      inspectionsCount = inspections.length;
    }

    try {
      const alertRes = await db.query('SELECT * FROM alerts ORDER BY created_at DESC LIMIT 10');
      if (alertRes.rows.length > 0) {
        alertsList = alertRes.rows.map(row => ({
          id: row.id,
          ngoId: row.ngo_id || 'ngo-001',
          title: `Anomaly Reported: ${row.ngo_name || 'NGO'}`,
          description: row.observation || row.details || 'Observation reported',
          severity: row.severity || 'HIGH',
          timestamp: row.created_at ? new Date(row.created_at).toISOString() : new Date().toISOString()
        }));
      } else {
        alertsList = readJsonFile('alerts.json');
      }
    } catch (e) {
      alertsList = readJsonFile('alerts.json');
    }

    const ngosMonitored = ngosList.length;
    const highRisk = ngosList.filter(n => n.riskScore && (n.riskScore.level === 'HIGH' || n.riskScore.score >= 75)).length;

    const priorityNgos = [...ngosList].sort((a, b) => {
      const scoreA = a.riskScore ? a.riskScore.score : 0;
      const scoreB = b.riskScore ? b.riskScore.score : 0;
      return scoreB - scoreA;
    }).slice(0, 3);

    return res.json({
      ngosMonitored,
      highRisk,
      inspections: inspectionsCount,
      priorityNgos,
      recentAlerts: alertsList.slice(0, 5)
    });
  } catch (err) {
    console.error('GET /api/overview error:', err.message);
    res.status(500).json({ error: 'Failed to retrieve overview metrics' });
  }
});

// GET /api/inspections
app.get('/api/inspections', async (req, res) => {
  try {
    const sql = `
      SELECT i.*, n.name AS ngo_name
      FROM inspections i
      LEFT JOIN ngos n ON i.ngo_id = n.id
      ORDER BY i.created_at DESC
    `;
    const result = await db.query(sql);
    if (result.rows.length > 0) {
      const mapped = result.rows.map(row => ({
        id: row.id,
        ngoId: row.ngo_id || 'ngo-001',
        ngoName: row.ngo_name || 'Sunrise Welfare Foundation',
        timestamp: row.submitted_at ? new Date(row.submitted_at).toISOString() : new Date().toISOString(),
        inspector: row.inspector_id || 'Inspector',
        status: row.status || 'COMPLETED',
        observations: row.observations || '',
        photoCount: row.photos_count || 0,
        checklist: { location: true, cctv: true, records: true, staff: true }
      }));
      return res.json(mapped);
    }
  } catch (err) {
    console.error('[PostgreSQL Fallback] GET /api/inspections failed:', err.message);
  }
  const inspections = readJsonFile('inspections.json');
  res.json(inspections);
});

// GET /api/inspections/:id
app.get('/api/inspections/:id', async (req, res) => {
  const id = req.params.id;
  try {
    const sql = `
      SELECT i.*, n.name AS ngo_name
      FROM inspections i
      LEFT JOIN ngos n ON i.ngo_id = n.id
      WHERE LOWER(i.id) = LOWER($1)
    `;
    const result = await db.query(sql, [id]);
    if (result.rows.length > 0) {
      const row = result.rows[0];
      return res.json({
        id: row.id,
        ngoId: row.ngo_id || 'ngo-001',
        ngoName: row.ngo_name || 'Sunrise Welfare Foundation',
        timestamp: row.submitted_at ? new Date(row.submitted_at).toISOString() : new Date().toISOString(),
        inspector: row.inspector_id || 'Inspector',
        status: row.status || 'COMPLETED',
        observations: row.observations || '',
        photoCount: row.photos_count || 0,
        checklist: { location: true, cctv: true, records: true, staff: true }
      });
    }
  } catch (err) {
    console.error(`[PostgreSQL Fallback] GET /api/inspections/${id} failed:`, err.message);
  }
  const inspections = readJsonFile('inspections.json');
  const insp = inspections.find(i => i.id.toLowerCase() === id.toLowerCase());
  if (insp) {
    res.json(insp);
  } else {
    res.status(404).json({ error: 'Inspection not found' });
  }
});

// POST /api/inspections
app.post('/api/inspections', async (req, res) => {
  const ngoId = req.body.ngoId || 'ngo-001';
  const inspector = req.body.inspector || 'Aarav Mehta';
  const status = req.body.status || 'SATISFACTORY';
  const observations = req.body.observations || 'Inspection completed cleanly.';
  const photoCount = typeof req.body.photoCount === 'number' ? req.body.photoCount : 0;
  const lat = req.body.latitude || 26.2389;
  const lng = req.body.longitude || 73.0243;

  let ngoName = req.body.ngoName || 'Sunrise Welfare Foundation';

  // Generate new Inspection ID: use PostgreSQL sequence as authoritative source
  // to guarantee atomic, collision-free ID generation even under concurrent requests.
  let newId;
  try {
    const seqRes = await db.query("SELECT nextval('inspection_id_seq') AS next_id");
    let candidateNum = parseInt(seqRes.rows[0].next_id, 10);

    // Cross-verify with JSON file to prevent collision if local files were modified
    const inspections = readJsonFile('inspections.json');
    let maxFound = candidateNum;
    inspections.forEach(r => {
      const num = parseInt((r.id || '').replace('INSP-', ''), 10);
      if (!isNaN(num) && num >= maxFound) maxFound = num + 1;
    });
    if (maxFound > candidateNum) {
      await db.query("SELECT setval('inspection_id_seq', $1)", [maxFound]);
      candidateNum = maxFound;
    }
    newId = `INSP-${candidateNum}`;
  } catch (seqErr) {
    console.warn('[ID Gen] Sequence unavailable, using MAX query fallback:', seqErr.message);
    let maxNum = 1000;
    try {
      const maxRes = await db.query("SELECT MAX(CAST(REPLACE(id, 'INSP-', '') AS INTEGER)) AS maxnum FROM inspections WHERE id ~ '^INSP-[0-9]+$'");
      const dbMax = maxRes.rows[0]?.maxnum;
      if (dbMax != null) {
        maxNum = Math.max(maxNum, parseInt(dbMax, 10));
      }
    } catch (dbMaxErr) {
      console.warn('[ID Gen] DB query unavailable, using JSON max:', dbMaxErr.message);
    }
    const inspections = readJsonFile('inspections.json');
    inspections.forEach(r => {
      const num = parseInt((r.id || '').replace('INSP-', ''), 10);
      if (!isNaN(num) && num > maxNum) maxNum = num;
    });
    newId = `INSP-${maxNum + 1}`;
  }

  const inspections = readJsonFile('inspections.json');

  const now = new Date();
  const newId = `INSP-${maxNum + 1}`;

  const newInspection = {
    id: newId,
    ngoId: ngoId,
    ngoName: ngoName,
    timestamp: now.toISOString(),
    inspector: inspector,
    status: status,
    observations: observations,
    photoCount: photoCount,
    checklist: req.body.checklist || { location: true, cctv: true, records: true, staff: true }
  };

  // 1. Dual-persist to JSON file
  inspections.unshift(newInspection);
  writeJsonFile('inspections.json', inspections);

  // 2. Dual-persist to PostgreSQL if connected
  try {
    const ngoRes = await db.query('SELECT name FROM ngos WHERE LOWER(id) = LOWER($1)', [ngoId]);
    if (ngoRes.rows.length > 0) {
      ngoName = ngoRes.rows[0].name;
      newInspection.ngoName = ngoName;
    }

    const insertInspSql = `
      INSERT INTO inspections (
        id, ngo_id, inspector_id, status, observations, photos_count,
        started_at, submitted_at, latitude, longitude, location, location_accuracy_m
      ) VALUES (
        $1, $2, $3, $4, $5, $6, $7, $7, $8, $9,
        ST_SetSRID(ST_MakePoint($9, $8), 4326)::geography,
        10
      )
    `;
    await db.query(insertInspSql, [newId, ngoId, inspector, status, observations, photoCount, now, lat, lng]);

    const newAuditId = `AUD-${Date.now()}`;
    const metadata = {
      inspectionId: newId,
      title: 'Inspection report submitted',
      details: `${newId} - ${status}`,
      actor: `${inspector} (Inspector)`,
      severity: status === 'SATISFACTORY' ? 'NORMAL' : 'WARNING'
    };

    const insertAuditSql = `
      INSERT INTO audit_events (id, event_type, entity_type, entity_id, ngo_id, metadata, created_at)
      VALUES ($1, 'INSPECTION', 'INSPECTION', $2, $3, $4, $5)
    `;
    await db.query(insertAuditSql, [newAuditId, newId, ngoId, JSON.stringify(metadata), now]);
  } catch (err) {
    console.warn('[PostgreSQL Note] DB insert skipped (using JSON file persistence):', err.message);
  }

  // Also add to audit.json
  try {
    const auditEvents = readJsonFile('audit.json');
    auditEvents.unshift({
      id: `AUD-${Date.now()}`,
      time: now.toISOString(),
      title: 'Inspection Report Submitted',
      description: `${newId} - ${ngoName} (${status})`,
      actor: `${inspector} (Inspector)`,
      iconType: 'SUBMIT'
    });
    writeJsonFile('audit.json', auditEvents);
  } catch (_e) {}

  return res.status(201).json({
    success: true,
    inspection: newInspection
  });
});

// POST /api/alerts (and /api/anomalies)
async function handleAlertCreation(req, res) {
  const inputNgoId = req.body.ngoId;
  const message = req.body.message || req.body.details || 'Anomaly reported by auditor.';
  const severity = req.body.severity || 'HIGH';
  const type = req.body.type || 'ANOMALY';
  const reporter = req.body.reporter || 'Field Auditor';

  console.log('[ALERT]');
  console.log('POST /api/alerts');
  console.log('[ALERT]');
  console.log(`ngoId=${inputNgoId}`);

  if (!inputNgoId) {
    console.error('[ALERT] Error: ngoId is required.');
    return res.status(400).json({ error: 'ngoId is required.' });
  }

  if (!message || message.trim().length === 0) {
    console.error('[ALERT] Error: Observation message is required.');
    return res.status(400).json({ error: 'Observation message is required.' });
  }

  try {
    // 1. Validate NGO ID & fetch name
    const ngoRes = await db.query('SELECT id, name FROM ngos WHERE LOWER(id) = LOWER($1)', [inputNgoId]);
    if (ngoRes.rows.length === 0) {
      console.error(`[ALERT] Error: NGO with id '${inputNgoId}' not found in database.`);
      return res.status(404).json({ error: `NGO with ID '${inputNgoId}' not found.` });
    }

    const finalNgoId = ngoRes.rows[0].id;
    const finalNgoName = ngoRes.rows[0].name;

    const now = new Date();
    const alertId = `ALT-${Date.now()}`;
    const auditId = `AUD-${Date.now()}`;

    console.log('[ALERT]');
    console.log('database insert started');

    // 2. Insert alert into PostgreSQL
    const insertAlertSql = `
      INSERT INTO alerts (id, ngo_id, type, observation, status, created_at)
      VALUES ($1, $2, $3, $4, 'OPEN', $5)
    `;
    await db.query(insertAlertSql, [alertId, finalNgoId, type, message, now]);
    console.log('[ALERT]');
    console.log('database insert successful');

    // 3. Update NGO alerts count
    await db.query(`UPDATE ngos SET alerts_count = alerts_count + 1 WHERE id = $1`, [finalNgoId]);

    // 4. Insert audit_events into PostgreSQL
    const metadata = {
      title: 'Anomaly Reported',
      details: message,
      actor: reporter,
      severity: severity === 'HIGH' ? 'HIGH' : 'WARNING'
    };
    const insertAuditSql = `
      INSERT INTO audit_events (id, event_type, entity_type, entity_id, ngo_id, metadata, created_at)
      VALUES ($1, 'ALERT', 'ALERT', $2, $3, $4, $5)
    `;
    await db.query(insertAuditSql, [auditId, alertId, finalNgoId, JSON.stringify(metadata), now]);
    console.log('[ALERT]');
    console.log('audit insert successful');

    console.log('[ALERT]');
    console.log('HTTP 201');

    return res.status(201).json({
      success: true,
      alertId: alertId,
      auditId: auditId,
      timestamp: now.toISOString()
    });
  } catch (err) {
    console.error('[ALERT] Database insert error:', err.message);
    return res.status(500).json({ error: 'Failed to record anomaly. Please try again.' });
  }
}

app.post('/api/alerts', handleAlertCreation);
app.post('/api/anomalies', handleAlertCreation);

// GET /api/audit
app.get('/api/audit', async (req, res) => {
  try {
    const sql = `
      SELECT a.*, n.name AS ngo_name
      FROM audit_events a
      LEFT JOIN ngos n ON a.ngo_id = n.id
      ORDER BY a.created_at DESC
    `;
    const result = await db.query(sql);
    if (result.rows.length > 0) {
      const mapped = result.rows.map(row => {
        const meta = typeof row.metadata === 'string' ? JSON.parse(row.metadata) : (row.metadata || {});
        return {
          id: row.id,
          inspectionId: row.entity_id || row.id,
          ngoId: row.ngo_id || 'ngo-001',
          ngoName: row.ngo_name || 'Sunrise Welfare Foundation',
          title: meta.title || row.event_type || 'Audit Event',
          details: meta.details || 'Event recorded',
          timestamp: row.created_at ? new Date(row.created_at).toISOString() : new Date().toISOString(),
          actor: meta.actor || 'System',
          type: row.event_type || 'SYSTEM',
          severity: meta.severity || 'NORMAL'
        };
      });
      return res.json(mapped);
    }
  } catch (err) {
    console.error('[PostgreSQL Fallback] GET /api/audit failed:', err.message);
  }
  const audits = readJsonFile('audit.json');
  res.json(audits);
});

// POST /api/location/verify (PostGIS Distance Calculation Endpoint)
app.post('/api/location/verify', async (req, res) => {
  const { ngoId, latitude, longitude, accuracy } = req.body;

  if (!ngoId || typeof latitude !== 'number' || typeof longitude !== 'number') {
    return res.status(400).json({
      error: 'ngoId, latitude, and longitude are required numbers.'
    });
  }

  try {
    const sql = `
      SELECT 
        id, name, geofence_radius_m,
        ST_Distance(
          location,
          ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography
        ) AS distance_meters
      FROM ngos
      WHERE LOWER(id) = LOWER($3)
    `;
    const result = await db.query(sql, [longitude, latitude, ngoId]);

    if (result.rows.length === 0) {
      return res.status(404).json({ error: `NGO with ID '${ngoId}' not found.` });
    }

    const row = result.rows[0];
    const distanceMeters = parseFloat(parseFloat(row.distance_meters || 0).toFixed(1));
    const geofenceRadiusMeters = row.geofence_radius_m || 100;
    const insideGeofence = distanceMeters <= geofenceRadiusMeters;

    return res.json({
      ngoId: row.id,
      distanceMeters: distanceMeters,
      geofenceRadiusMeters: geofenceRadiusMeters,
      insideGeofence: insideGeofence,
      accuracyMeters: typeof accuracy === 'number' ? accuracy : 10
    });
  } catch (err) {
    console.error('[PostGIS Error] POST /api/location/verify failed:', err.message);
    return res.status(500).json({ error: 'Failed to verify location. Please try again.' });
  }
});

// GET /api/dashboard
app.get('/api/dashboard', async (req, res) => {
  try {
    const ngosRes = await db.query(`SELECT * FROM ngos ORDER BY id ASC`);
    const alertsRes = await db.query(`SELECT * FROM alerts ORDER BY created_at DESC LIMIT 5`);
    const inspsRes = await db.query(`SELECT COUNT(*)::int AS count FROM inspections`);

    if (ngosRes.rows.length > 0) {
      const mappedNgos = ngosRes.rows.map(mapNgoRowToContract);
      const highRiskCount = mappedNgos.filter(n => n.riskScore && n.riskScore.level === 'HIGH').length;
      const mappedAlerts = alertsRes.rows.map(row => ({
        id: row.id,
        ngoId: row.ngo_id || 'ngo-001',
        ngoName: 'NGO',
        type: row.type || 'ANOMALY',
        title: `Anomaly Reported`,
        description: row.observation || 'Observation reported',
        severity: 'HIGH',
        timestamp: row.created_at ? new Date(row.created_at).toISOString() : new Date().toISOString(),
        status: row.status || 'OPEN'
      }));

      return res.json({
        ngosMonitored: mappedNgos.length,
        highRiskCount: highRiskCount,
        inspectionsCount: inspsRes.rows[0]?.count || 0,
        priorityNgos: mappedNgos.slice(0, 3),
        recentAlerts: mappedAlerts
      });
    }
  } catch (err) {
    console.error('[PostgreSQL Fallback] GET /api/dashboard failed:', err.message);
  }

  const ngos = readJsonFile('ngos.json');
  const alerts = readJsonFile('alerts.json');
  const inspections = readJsonFile('inspections.json');
  const highRiskCount = ngos.filter(n => n.riskScore && n.riskScore.level === 'HIGH').length;

  res.json({
    ngosMonitored: ngos.length,
    highRiskCount: highRiskCount,
    inspectionsCount: inspections.length,
    priorityNgos: ngos.slice(0, 3),
    recentAlerts: alerts.slice(0, 5)
  });
});

// Helper to log call audit events
async function logCallAuditEvent(eventType, inspectionId, metadata = {}) {
  const auditId = `AUD-CALL-${Date.now()}`;
  const now = new Date();
  const ngoId = metadata.ngoId || (inspectionId.includes('ngo-') ? inspectionId : 'ngo-001');
  const meta = {
    inspectionId,
    eventType,
    timestamp: now.toISOString(),
    ...metadata
  };

  try {
    const insertAuditSql = `
      INSERT INTO audit_events (id, event_type, entity_type, entity_id, ngo_id, metadata, created_at)
      VALUES ($1, $2, 'CALL', $3, $4, $5, $6)
    `;
    await db.query(insertAuditSql, [auditId, eventType, inspectionId, ngoId, JSON.stringify(meta), now]);
    console.log(`[Audit] Call event recorded: ${eventType} for ${inspectionId}`);
  } catch (err) {
    console.warn(`[Audit DB Fallback] ${eventType} logged to JSON file:`, err.message);
    try {
      const audits = readJsonFile('audit.json');
      audits.unshift({
        id: auditId,
        eventType,
        entityType: 'CALL',
        entityId: inspectionId,
        ngoId,
        metadata: meta,
        createdAt: now.toISOString()
      });
      const filePath = path.join(DATA_DIR, 'audit.json');
      fs.writeFileSync(filePath, JSON.stringify(audits, null, 2));
    } catch (fErr) {
      console.error('[Audit JSON Error]', fErr.message);
    }
  }
}

// WebRTC Signaling Server Implementation
const wss = new WebSocketServer({ server, path: '/ws' });
const rooms = new Map(); // inspectionId -> Set of ws clients

wss.on('connection', (ws) => {
  console.log('[WebSocket Signaling] Client connected');

  ws.on('message', async (rawMsg) => {
    try {
      const msg = JSON.parse(rawMsg.toString());
      const { type, inspectionId, participantId } = msg;

      if (!type || !inspectionId) {
        return ws.send(JSON.stringify({ type: 'error', message: 'Missing type or inspectionId' }));
      }

      if (!rooms.has(inspectionId)) {
        rooms.set(inspectionId, new Set());
      }
      const room = rooms.get(inspectionId);

      switch (type) {
        case 'join': {
          ws.inspectionId = inspectionId;
          ws.participantId = participantId || `peer-${Math.random().toString(36).substring(2, 7)}`;
          room.add(ws);

          console.log(`[Signaling] '${ws.participantId}' joined room '${inspectionId}'. Total in room: ${room.size}`);

          // Confirm join to caller
          ws.send(JSON.stringify({
            type: 'joined',
            inspectionId,
            participantId: ws.participantId,
            participantCount: room.size
          }));

          // Notify existing participants
          for (const client of room) {
            if (client !== ws && client.readyState === 1 /* OPEN */) {
              client.send(JSON.stringify({
                type: 'participant-joined',
                inspectionId,
                participantId: ws.participantId
              }));
            }
          }

          logCallAuditEvent('CALL_STARTED', inspectionId, { participantId: ws.participantId });
          break;
        }

        case 'offer':
        case 'answer':
        case 'ice-candidate':
        case 'leave': {
          for (const client of room) {
            if (client !== ws && client.readyState === 1 /* OPEN */) {
              client.send(JSON.stringify(msg));
            }
          }

          if (type === 'answer') {
            logCallAuditEvent('CALL_CONNECTED', inspectionId, { participantId: ws.participantId });
          } else if (type === 'leave') {
            logCallAuditEvent('CALL_ENDED', inspectionId, { participantId: ws.participantId });
            room.delete(ws);
            if (room.size === 0) rooms.delete(inspectionId);
          }
          break;
        }

        default:
          console.warn('[Signaling] Unknown type:', type);
      }
    } catch (err) {
      console.error('[Signaling Message Error]', err.message);
    }
  });

  ws.on('close', () => {
    if (ws.inspectionId && rooms.has(ws.inspectionId)) {
      const room = rooms.get(ws.inspectionId);
      room.delete(ws);
      console.log(`[Signaling] '${ws.participantId}' left room '${ws.inspectionId}'. Remaining: ${room.size}`);

      for (const client of room) {
        if (client.readyState === 1 /* OPEN */) {
          client.send(JSON.stringify({
            type: 'participant-left',
            inspectionId: ws.inspectionId,
            participantId: ws.participantId
          }));
        }
      }

      if (room.size === 0) rooms.delete(ws.inspectionId);
      logCallAuditEvent('CALL_ENDED', ws.inspectionId, { participantId: ws.participantId, reason: 'disconnected' });
    }
  });
});

// Initialize Database on Startup
initializeDatabase()
  .then(() => {
    console.log('[PostgreSQL] Database and PostGIS initialized.');
  })
  .catch((err) => {
    console.error('[PostgreSQL Initialization Error]', err.message);
  });

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Drishti 360 Backend running on port ${PORT} with WebRTC signaling at ws://0.0.0.0:${PORT}/ws`);
});

