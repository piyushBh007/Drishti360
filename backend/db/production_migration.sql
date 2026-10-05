-- =============================================================================
-- Drishti360 — Production Database Migration Script
-- Target: Supabase PostgreSQL with PostGIS
-- =============================================================================
-- Run this script in the Supabase SQL Editor (Dashboard → SQL Editor → New Query)
-- after creating your Supabase project.
--
-- IMPORTANT:
--   - Run once to initialize the production database.
--   - ON CONFLICT DO NOTHING ensures existing data is never overwritten.
--   - Never run DROP TABLE in production.
-- =============================================================================

-- 1. Enable PostGIS Extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- =============================================================================
-- 2. Tables
-- =============================================================================

-- NGOs
CREATE TABLE IF NOT EXISTS ngos (
    id VARCHAR(50) PRIMARY KEY,
    name TEXT NOT NULL,
    registration_no TEXT,
    category TEXT,
    established_year INTEGER,
    beneficiaries INTEGER,
    city TEXT,
    state TEXT,
    address TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    location GEOGRAPHY(POINT, 4326),
    geofence_radius_m INTEGER DEFAULT 100,
    risk_score INTEGER,
    risk_level TEXT,
    risk_trend TEXT,
    attendance_rate DOUBLE PRECISION,
    cctv_online INTEGER,
    cctv_total INTEGER,
    uptime_percent DOUBLE PRECISION,
    last_inspection TIMESTAMP,
    alerts_count INTEGER DEFAULT 0,
    image TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ngos_location ON ngos USING GIST(location);

-- Cameras
CREATE TABLE IF NOT EXISTS cameras (
    id VARCHAR(50) PRIMARY KEY,
    ngo_id VARCHAR(50) REFERENCES ngos(id) ON DELETE CASCADE,
    name TEXT,
    status TEXT,
    source TEXT,
    type TEXT,
    stream_url TEXT,
    protocol TEXT,
    media_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE cameras ADD COLUMN IF NOT EXISTS stream_url TEXT;
ALTER TABLE cameras ADD COLUMN IF NOT EXISTS protocol TEXT;

-- Inspections Table & Sequence
CREATE SEQUENCE IF NOT EXISTS inspection_id_seq START WITH 1001;
CREATE TABLE IF NOT EXISTS inspections (
    id VARCHAR(50) PRIMARY KEY,
    ngo_id VARCHAR(50) REFERENCES ngos(id) ON DELETE CASCADE,
    inspector_id TEXT,
    status TEXT,
    observations TEXT,
    photos_count INTEGER DEFAULT 0,
    started_at TIMESTAMP,
    submitted_at TIMESTAMP,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    location GEOGRAPHY(POINT, 4326),
    location_accuracy_m DOUBLE PRECISION,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inspections_location ON inspections USING GIST(location);

-- Alerts
CREATE TABLE IF NOT EXISTS alerts (
    id VARCHAR(50) PRIMARY KEY,
    ngo_id VARCHAR(50) REFERENCES ngos(id) ON DELETE CASCADE,
    type TEXT,
    observation TEXT,
    status TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Audit Events
CREATE TABLE IF NOT EXISTS audit_events (
    id VARCHAR(50) PRIMARY KEY,
    event_type TEXT,
    entity_type TEXT,
    entity_id TEXT,
    ngo_id VARCHAR(50),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Risk Scores
CREATE TABLE IF NOT EXISTS risk_scores (
    id BIGSERIAL PRIMARY KEY,
    ngo_id VARCHAR(50) REFERENCES ngos(id) ON DELETE CASCADE,
    score DOUBLE PRECISION,
    level TEXT,
    model_version TEXT,
    features JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Camera Activity (YOLO Aggregated Observations)
CREATE TABLE IF NOT EXISTS camera_activity (
    id SERIAL PRIMARY KEY,
    camera_id VARCHAR(50) NOT NULL REFERENCES cameras(id) ON DELETE CASCADE,
    ngo_id VARCHAR(50) REFERENCES ngos(id) ON DELETE SET NULL,
    observed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    observation_duration_seconds NUMERIC(10, 2) NOT NULL DEFAULT 0,
    average_person_count NUMERIC(10, 2) NOT NULL DEFAULT 0,
    maximum_person_count INTEGER NOT NULL DEFAULT 0,
    minimum_person_count INTEGER NOT NULL DEFAULT 0,
    frames_processed INTEGER NOT NULL DEFAULT 0,
    detection_rate NUMERIC(5, 4) NOT NULL DEFAULT 0,
    model_name VARCHAR(50) DEFAULT 'YOLOv8n',
    model_version VARCHAR(50) DEFAULT '8.0'
);

CREATE INDEX IF NOT EXISTS idx_camera_activity_camera_id ON camera_activity(camera_id);
CREATE INDEX IF NOT EXISTS idx_camera_activity_ngo_id ON camera_activity(ngo_id);
CREATE INDEX IF NOT EXISTS idx_camera_activity_observed_at ON camera_activity(observed_at);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    name TEXT NOT NULL,
    employee_id TEXT UNIQUE,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'inspector',
    region TEXT,
    active BOOLEAN DEFAULT true,
    assigned_ngos JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 3. Seed: Initial NGOs
-- NOTE: Coordinates use PostGIS standard: ST_MakePoint(longitude, latitude)
-- NOTE: ON CONFLICT DO NOTHING — safe to re-run; never overwrites existing data.
-- =============================================================================

INSERT INTO ngos (
    id, name, registration_no, category, established_year, beneficiaries,
    city, state, address, latitude, longitude, location, geofence_radius_m,
    risk_score, risk_level, risk_trend, attendance_rate, cctv_online, cctv_total,
    uptime_percent, last_inspection, alerts_count, image
) VALUES
(
    'ngo-001', 'Sunrise Welfare Foundation', 'NGO-MH-2015-0142', 'Child Welfare', 2015, 450,
    'Mira-Bhayandar', 'Maharashtra',
    'Plot No. 7, Sector 4, Mira Road East, Thane - 401107',
    19.2296, 72.8540,
    ST_SetSRID(ST_MakePoint(72.8540, 19.2296), 4326)::geography,
    150,
    72, 'HIGH', 'INCREASING', 68.5, 2, 3, 94.2,
    NOW() - INTERVAL '5 days', 2, 'ngo-001.png'
),
(
    'ngo-002', 'Green Earth Society', 'NGO-RJ-2018-0391', 'Environment', 2018, 310,
    'Jodhpur', 'Rajasthan',
    '12 Mandore Road, Jodhpur, Rajasthan - 342001',
    26.2389, 73.0243,
    ST_SetSRID(ST_MakePoint(73.0243, 26.2389), 4326)::geography,
    100,
    45, 'MEDIUM', 'STABLE', 82.1, 1, 2, 98.7,
    NOW() - INTERVAL '12 days', 1, 'ngo-002.png'
),
(
    'ngo-003', 'Digital Literacy India', 'NGO-KA-2020-0204', 'Education', 2020, 280,
    'Bangalore', 'Karnataka',
    '22 Whitefield Main Road, Bangalore - 560066',
    12.9716, 77.5946,
    ST_SetSRID(ST_MakePoint(77.5946, 12.9716), 4326)::geography,
    100,
    28, 'LOW', 'IMPROVING', 91.3, 1, 1, 99.1,
    NOW() - INTERVAL '20 days', 0, 'ngo-003.png'
)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 4. Seed: Cameras (mapped to NGO-001 for demo CCTV feeds)
-- media_path is null in production — recorded feeds are referenced by Android
-- from res/raw directly and do NOT go through the backend.
-- =============================================================================

INSERT INTO cameras (id, ngo_id, name, status, source, type, stream_url, protocol, media_path) VALUES
('camera-001', 'ngo-001', 'Camera 01 - Main Hall',    'ONLINE', 'LOCAL_VIDEO', 'LOCAL_DEMO', NULL, 'LOCAL_FILE', NULL),
('camera-002', 'ngo-001', 'Camera 02 - Entrance',     'ONLINE', 'LOCAL_VIDEO', 'LOCAL_DEMO', NULL, 'LOCAL_FILE', NULL),
('camera-003', 'ngo-001', 'Camera 03 - Storage Room', 'OFFLINE','LOCAL_VIDEO', 'LOCAL_DEMO', NULL, 'LOCAL_FILE', NULL)
ON CONFLICT (id) DO NOTHING;

-- Synchronize inspection_id_seq with existing inspections
SELECT setval('inspection_id_seq', GREATEST(1000, COALESCE((SELECT MAX(CAST(REPLACE(id, 'INSP-', '') AS INTEGER)) FROM inspections WHERE id ~ '^INSP-[0-9]+$'), 1000)));

-- =============================================================================
-- 5. Seed: Admin User
-- Password must be bcrypt hashed — set it via the backend seed script.
-- The Node backend's initializeDatabase() seeds the admin on first startup.
-- =============================================================================
-- NOTE: Do NOT insert a plaintext password here.
-- The admin is seeded by the Node backend automatically on first start.

-- =============================================================================
-- 6. Done
-- =============================================================================
-- After running this script:
--   1. Copy your Supabase connection string (Project Settings → Database → URI).
--   2. Set DATABASE_URL on your Render backend service.
--   3. On first backend startup, initializeDatabase() will:
--      a. Verify PostGIS is enabled (already done above).
--      b. Create admin user with hashed password.
--      c. Seed any missing data from data/*.json files.
-- =============================================================================
