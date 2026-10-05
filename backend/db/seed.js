const fs = require('fs');
const path = require('path');
const { query, testConnection, pool } = require('./connection');
const bcrypt = require('bcryptjs');

const DATA_DIR = path.join(__dirname, '..', 'data');

function readJsonFile(filename) {
  try {
    const filePath = path.join(DATA_DIR, filename);
    if (!fs.existsSync(filePath)) {
      return [];
    }
    const content = fs.readFileSync(filePath, 'utf8');
    return JSON.parse(content);
  } catch (err) {
    console.error(`[Seed Error] Failed to read JSON file ${filename}:`, err.message);
    return [];
  }
}

async function initializeDatabase() {
  console.log('[Database Initialization] Starting PostgreSQL + PostGIS setup...');

  const isConnected = await testConnection();
  if (!isConnected) {
    throw new Error('PostgreSQL database connection failed. Ensure PostgreSQL server is running and credentials in .env are correct.');
  }

  // 1. Enable PostGIS extension & Run Schema
  try {
    await query('CREATE EXTENSION IF NOT EXISTS postgis;');
    const schemaPath = path.join(__dirname, 'schema.sql');
    const schemaSql = fs.readFileSync(schemaPath, 'utf8');
    await query(schemaSql);
    await query(`
      ALTER TABLE cameras ADD COLUMN IF NOT EXISTS stream_url TEXT;
      ALTER TABLE cameras ADD COLUMN IF NOT EXISTS protocol TEXT;
    `);
    console.log('[Database Schema] Extension enabled and tables/indexes created successfully.');
  } catch (err) {
    console.error('[Database Schema Error] Failed to apply schema:', err.message);
    throw err;
  }

  // 2. Seed NGOs from ngos.json
  const ngos = readJsonFile('ngos.json');
  let seededNgosCount = 0;

  for (const ngo of ngos) {
    const id = ngo.id;
    const name = ngo.name || ngo.ngoName || 'NGO Entity';
    const registrationNo = ngo.registrationNo || null;
    const category = ngo.category || null;
    const establishedYear = ngo.establishedYear || null;
    const beneficiaries = ngo.beneficiaries || 0;
    const city = ngo.location?.city || null;
    const state = ngo.location?.state || null;
    const address = ngo.location?.address || null;
    const lat = ngo.location?.latitude || 0;
    const lng = ngo.location?.longitude || 0;
    const geofenceRadius = ngo.geofenceRadiusMeters || 100;
    const riskScore = ngo.riskScore?.score || null;
    const riskLevel = ngo.riskScore?.level || null;
    const riskTrend = ngo.riskScore?.trend || null;
    const attendanceRate = ngo.attendanceRate || null;
    const cctvOnline = ngo.cctvOnlineCount || 0;
    const cctvTotal = ngo.cctvTotalCount || 0;
    const uptimePercent = ngo.networkUptime || null;
    const lastInspection = ngo.lastInspectionDate ? new Date(ngo.lastInspectionDate) : null;
    const alertsCount = ngo.activeAlertsCount || 0;
    const image = ngo.image || null;

    const insertNgoSql = `
      INSERT INTO ngos (
        id, name, registration_no, category, established_year, beneficiaries,
        city, state, address, latitude, longitude, location, geofence_radius_m,
        risk_score, risk_level, risk_trend, attendance_rate, cctv_online, cctv_total,
        uptime_percent, last_inspection, alerts_count, image
      ) VALUES (
        $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11,
        ST_SetSRID(ST_MakePoint($11, $10), 4326)::geography,
        $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22
      )
      ON CONFLICT (id) DO NOTHING;
    `;

    const res = await query(insertNgoSql, [
      id, name, registrationNo, category, establishedYear, beneficiaries,
      city, state, address, lat, lng, geofenceRadius,
      riskScore, riskLevel, riskTrend, attendanceRate, cctvOnline, cctvTotal,
      uptimePercent, lastInspection, alertsCount, image
    ]);

    if (res.rowCount > 0) {
      seededNgosCount++;
    }
  }

  console.log(`[Database Seed] Seeded ${seededNgosCount} new NGOs into PostgreSQL (Total JSON NGOs: ${ngos.length}).`);

  // 3. Seed Cameras from cameras.json
  const cameras = readJsonFile('cameras.json');
  let seededCamerasCount = 0;
  for (let i = 0; i < cameras.length; i++) {
    const cam = cameras[i];
    const id = cam.id || cam.cameraId || cam.cam_id || `cam-${String(i + 1).padStart(3, '0')}`;
    const ngoId = cam.ngo_id || cam.ngoId || cam.ngo || null;
    const name = cam.name || cam.cameraName || cam.title || null;
    let source = cam.source || (cam.type === 'IP_CAMERA' ? 'RTSP' : 'LOCAL_VIDEO');
    let type = cam.type || (source === 'RTSP' ? 'IP_CAMERA' : 'LOCAL_DEMO');
    let protocol = cam.protocol || (source === 'RTSP' ? 'RTSP' : 'LOCAL_FILE');
    let streamUrl = cam.stream_url || cam.streamUrl || null;
    let status = cam.status || 'OFFLINE';

    // RTSP Environment Override for IP Cameras
    if ((source === 'RTSP' || type === 'IP_CAMERA') && process.env.RTSP_STREAM_URL) {
      streamUrl = process.env.RTSP_STREAM_URL;
      status = 'ONLINE';
    }

    const mediaPath = cam.media_path || cam.mediaPath || cam.media || null;

    const insertCamSql = `
      INSERT INTO cameras (id, ngo_id, name, status, source, type, stream_url, protocol, media_path)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      ON CONFLICT (id) DO UPDATE SET
        ngo_id = EXCLUDED.ngo_id,
        name = EXCLUDED.name,
        status = EXCLUDED.status,
        source = EXCLUDED.source,
        type = EXCLUDED.type,
        stream_url = EXCLUDED.stream_url,
        protocol = EXCLUDED.protocol,
        media_path = EXCLUDED.media_path;
    `;
    const res = await query(insertCamSql, [
      id, ngoId, name, status, source, type, streamUrl, protocol, mediaPath
    ]);
    if (res.rowCount > 0) {
      seededCamerasCount++;
    }
  }
  console.log(`[Database Seed] Seeded/updated ${seededCamerasCount} cameras in PostgreSQL (Total JSON Cameras: ${cameras.length}).`);

  // 4. Seed Inspections from inspections.json
  const inspections = readJsonFile('inspections.json');
  for (const insp of inspections) {
    const lat = insp.latitude || 26.2389;
    const lng = insp.longitude || 73.0243;
    const insertInspSql = `
      INSERT INTO inspections (
        id, ngo_id, inspector_id, status, observations, photos_count,
        submitted_at, latitude, longitude, location, location_accuracy_m
      ) VALUES (
        $1, $2, $3, $4, $5, $6, $7, $8, $9,
        ST_SetSRID(ST_MakePoint($9, $8), 4326)::geography,
        $10
      )
      ON CONFLICT (id) DO NOTHING;
    `;
    await query(insertInspSql, [
      insp.id, insp.ngoId || null, insp.inspector || 'Inspector', insp.status || 'COMPLETED',
      insp.observations || null, insp.photoCount || 0,
      insp.timestamp ? new Date(insp.timestamp) : new Date(), lat, lng, 10
    ]);
  }

  // Ensure sequence exists and is synchronized to the highest existing inspection ID
  try {
    await query(`
      CREATE SEQUENCE IF NOT EXISTS inspection_id_seq START WITH 1001;
      SELECT setval('inspection_id_seq', GREATEST(1000, COALESCE((SELECT MAX(CAST(REPLACE(id, 'INSP-', '') AS INTEGER)) FROM inspections WHERE id ~ '^INSP-[0-9]+$'), 1000)));
    `);
    console.log('[Database Seed] Synchronized inspection_id_seq sequence.');
  } catch (seqSyncErr) {
    console.warn('[Database Seed] Sequence sync warning:', seqSyncErr.message);
  }

  // 5. Seed Alerts from alerts.json
  const alerts = readJsonFile('alerts.json');
  for (const alt of alerts) {
    const insertAlertSql = `
      INSERT INTO alerts (id, ngo_id, type, observation, status, created_at)
      VALUES ($1, $2, $3, $4, $5, $6)
      ON CONFLICT (id) DO NOTHING;
    `;
    await query(insertAlertSql, [
      alt.id, alt.ngoId || null, alt.type || 'ANOMALY',
      alt.description || alt.message || 'Alert observation',
      alt.status || 'OPEN', alt.timestamp ? new Date(alt.timestamp) : new Date()
    ]);
  }

  // 6. Seed Audit Events from audit.json
  const auditEvents = readJsonFile('audit.json');
  for (const aud of auditEvents) {
    const insertAuditSql = `
      INSERT INTO audit_events (id, event_type, entity_type, entity_id, ngo_id, metadata, created_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      ON CONFLICT (id) DO NOTHING;
    `;
    const metadata = {
      title: aud.title || '',
      details: aud.details || aud.description || '',
      actor: aud.actor || 'System',
      severity: aud.severity || 'NORMAL'
    };
    await query(insertAuditSql, [
      aud.id, aud.type || 'SYSTEM', 'AUDIT', aud.inspectionId || aud.id,
      aud.ngoId || null, JSON.stringify(metadata), aud.timestamp ? new Date(aud.timestamp) : new Date()
    ]);
  }

  // 7. Seed Admin User
  try {
    const adminHash = await bcrypt.hash('1234', 10);
    const insertAdminSql = `
      INSERT INTO users (id, name, employee_id, username, password_hash, role, active)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      ON CONFLICT (username) DO NOTHING;
    `;
    await query(insertAdminSql, [
      'usr-admin-001', 'Piyush', 'EMP-001', 'piyush', adminHash, 'admin', true
    ]);
    console.log('[Database Seed] Seeded default admin user: piyush');
  } catch (err) {
    console.warn('[Database Seed] Admin user seeding warning:', err.message);
  }

  // 8. Initialize Risk Scores for all NGOs
  try {
    const { getAllRiskScores } = require('../services/riskService');
    const initialRisks = await getAllRiskScores();
    console.log(`[Database Seed] Calculated and persisted initial risk scores for ${initialRisks.length} NGOs.`);
  } catch (riskErr) {
    console.warn('[Database Seed] Risk initialization warning:', riskErr.message);
  }

  console.log('[Database Initialization] Complete. All tables initialized and seeded cleanly.');
}

if (require.main === module) {
  initializeDatabase()
    .then(() => {
      console.log('[DB Script] Finished database initialization.');
      process.exit(0);
    })
    .catch((err) => {
      console.error('[DB Script Error]', err);
      process.exit(1);
    });
}

module.exports = {
  initializeDatabase
};
