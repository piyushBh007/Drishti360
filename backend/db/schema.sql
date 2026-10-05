-- Drishti360 PostgreSQL + PostGIS Schema Definition

CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. NGOs Table
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

-- Spatial index on NGO locations
CREATE INDEX IF NOT EXISTS idx_ngos_location ON ngos USING GIST(location);

-- 2. Cameras Table
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

-- 3. Inspections Table & Sequence
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

-- Spatial index on Inspection locations
CREATE INDEX IF NOT EXISTS idx_inspections_location ON inspections USING GIST(location);

-- 4. Alerts Table
CREATE TABLE IF NOT EXISTS alerts (
    id VARCHAR(50) PRIMARY KEY,
    ngo_id VARCHAR(50) REFERENCES ngos(id) ON DELETE CASCADE,
    type TEXT,
    observation TEXT,
    status TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Audit Events Table
CREATE TABLE IF NOT EXISTS audit_events (
    id VARCHAR(50) PRIMARY KEY,
    event_type TEXT,
    entity_type TEXT,
    entity_id TEXT,
    ngo_id VARCHAR(50),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Risk Scores Table
CREATE TABLE IF NOT EXISTS risk_scores (
    id BIGSERIAL PRIMARY KEY,
    ngo_id VARCHAR(50) REFERENCES ngos(id) ON DELETE CASCADE,
    score DOUBLE PRECISION,
    level TEXT,
    model_version TEXT,
    features JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Camera Activity Table (YOLO Aggregated Observations)
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

-- 8. Users Table
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
