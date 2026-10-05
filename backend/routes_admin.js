const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('./db/connection');

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || 'drishti360_secret_key_2026';
if (process.env.NODE_ENV === 'production' && !process.env.JWT_SECRET) {
    console.error('[SECURITY WARNING] JWT_SECRET is not set in production! Please define JWT_SECRET environment variable.');
}

// Middleware for Admin
const requireAdmin = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader) return res.status(401).json({ error: 'Unauthorized' });
    const token = authHeader.split(' ')[1];
    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        if (decoded.role !== 'admin') return res.status(403).json({ error: 'Forbidden: Admin access required' });
        req.user = decoded;
        next();
    } catch (err) {
        return res.status(401).json({ error: 'Invalid token' });
    }
};

// Middleware for Auth (Admin or Inspector)
const requireAuth = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader) return res.status(401).json({ error: 'Unauthorized' });
    const token = authHeader.split(' ')[1];
    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        req.user = decoded;
        next();
    } catch (err) {
        return res.status(401).json({ error: 'Invalid token' });
    }
};

// POST /api/auth/login
router.post('/auth/login', async (req, res) => {
    const { username, password } = req.body;
    try {
        const result = await db.query('SELECT * FROM users WHERE username = $1', [username]);
        if (result.rows.length === 0) return res.status(401).json({ error: 'Invalid credentials' });
        
        const user = result.rows[0];
        if (!user.active) return res.status(403).json({ error: 'Account disabled' });
        
        const match = await bcrypt.compare(password, user.password_hash);
        if (!match) return res.status(401).json({ error: 'Invalid credentials' });
        
        const token = jwt.sign({ id: user.id, username: user.username, role: user.role }, JWT_SECRET, { expiresIn: '24h' });
        
        const { password_hash, ...userWithoutPassword } = user;
        res.json({ token, user: userWithoutPassword });
    } catch (err) {
        console.error('Login error:', err);
        require('./utils/errorHandler').handleApiError(res, err);
    }
});

// GET /api/inspectors - Requires Admin
router.get('/inspectors', requireAuth, async (req, res) => {
    try {
        const result = await db.query('SELECT id, name, employee_id, username, role, region, active, assigned_ngos, created_at, updated_at FROM users WHERE role = $1 ORDER BY name', ['inspector']);
        res.json(result.rows);
    } catch (err) {
        require('./utils/errorHandler').handleApiError(res, err);
    }
});

// GET /api/auth/me - Get current user profile
router.get('/auth/me', requireAuth, async (req, res) => {
    try {
        const result = await db.query('SELECT id, name, employee_id, username, role, region, active, assigned_ngos, created_at, updated_at FROM users WHERE id = $1', [req.user.id]);
        if (result.rows.length === 0) return res.status(404).json({ error: 'User not found' });
        res.json(result.rows[0]);
    } catch (err) {
        require('./utils/errorHandler').handleApiError(res, err);
    }
});

// POST /api/inspectors - Requires Admin
router.post('/inspectors', requireAdmin, async (req, res) => {
    const { id, name, employee_id, username, password, region, assigned_ngos } = req.body;
    try {
        const hash = await bcrypt.hash(password, 10);
        const newId = id || `usr_insp_${Date.now()}`;
        const result = await db.query(
            'INSERT INTO users (id, name, employee_id, username, password_hash, role, region, assigned_ngos) VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING id, name, employee_id, username, role, region, active',
            [newId, name, employee_id, username, hash, 'inspector', region, JSON.stringify(assigned_ngos || [])]
        );
        
        // Create Audit Event
        const auditId = `AUD-${Date.now()}`;
        const metadata = {
            title: 'Inspector Created',
            details: `Inspector ${name} (${employee_id}) created.`,
            actor: req.user.username || 'System Admin',
            severity: 'NORMAL'
        };
        await db.query(
            'INSERT INTO audit_events (id, event_type, entity_type, entity_id, metadata, created_at) VALUES ($1, $2, $3, $4, $5, $6)',
            [auditId, 'SYSTEM', 'USER', newId, JSON.stringify(metadata), new Date()]
        );
        
        res.json(result.rows[0]);
    } catch (err) {
        require('./utils/errorHandler').handleApiError(res, err);
    }
});

// PUT /api/inspectors/:id - Requires Admin
router.put('/inspectors/:id', requireAdmin, async (req, res) => {
    const { name, employee_id, active, region, assigned_ngos } = req.body;
    try {
        const result = await db.query(
            'UPDATE users SET name = $1, employee_id = $2, active = $3, region = $4, assigned_ngos = $5, updated_at = CURRENT_TIMESTAMP WHERE id = $6 RETURNING id, name, employee_id, username, role, region, active',
            [name, employee_id, active, region, JSON.stringify(assigned_ngos || []), req.params.id]
        );
        res.json(result.rows[0]);
    } catch (err) {
        require('./utils/errorHandler').handleApiError(res, err);
    }
});

// POST /api/ngos - Requires Admin
router.post('/ngos', requireAdmin, async (req, res) => {
    const { id, name, registration_no, category, city, state, address, latitude, longitude, geofence_radius_m } = req.body;
    try {
        const newId = id || `ngo-${Date.now()}`;
        // Insert into PostGIS
        const query = `
            INSERT INTO ngos (id, name, registration_no, category, city, state, address, latitude, longitude, location, geofence_radius_m)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, ST_SetSRID(ST_MakePoint($9, $8), 4326), $10)
            RETURNING *
        `;
        const result = await db.query(query, [newId, name, registration_no, category, city, state, address, latitude, longitude, geofence_radius_m || 100]);
        res.json(result.rows[0]);
    } catch (err) {
        require('./utils/errorHandler').handleApiError(res, err);
    }
});

// PUT /api/ngos/:id - Requires Admin
router.put('/ngos/:id', requireAdmin, async (req, res) => {
    const { name, registration_no, category, city, state, address, latitude, longitude, geofence_radius_m } = req.body;
    try {
        const query = `
            UPDATE ngos 
            SET name = $1, registration_no = $2, category = $3, city = $4, state = $5, address = $6, latitude = $7, longitude = $8,
                location = ST_SetSRID(ST_MakePoint($8, $7), 4326), geofence_radius_m = $9, updated_at = CURRENT_TIMESTAMP
            WHERE id = $10
            RETURNING *
        `;
        const result = await db.query(query, [name, registration_no, category, city, state, address, latitude, longitude, geofence_radius_m, req.params.id]);
        res.json(result.rows[0]);
    } catch (err) {
        require('./utils/errorHandler').handleApiError(res, err);
    }
});

module.exports = router;


// DELETE /api/inspectors/:id
router.delete('/inspectors/:id', requireAdmin, async (req, res) => {
    try {
        await db.query('DELETE FROM users WHERE id = $1', [req.params.id]);
        res.json({ success: true });
    } catch (err) {
        require('./utils/errorHandler').handleApiError(res, err);
    }
});

// DELETE /api/ngos/:id
router.delete('/ngos/:id', requireAdmin, async (req, res) => {
    try {
        await db.query('DELETE FROM ngos WHERE id = $1', [req.params.id]);
        res.json({ success: true });
    } catch (err) {
        require('./utils/errorHandler').handleApiError(res, err);
    }
});
