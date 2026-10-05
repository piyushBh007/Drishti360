const { Pool } = require('pg');
require('dotenv').config();

// Support Supabase/Render DATABASE_URL or individual env vars.
// SSL is required for Supabase and most production Postgres hosts.
let poolConfig;
if (process.env.DATABASE_URL) {
  poolConfig = {
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.DB_SSL === 'false' ? false : { rejectUnauthorized: false },
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 10000
  };
} else {
  const isLocalhost = !process.env.DB_HOST || process.env.DB_HOST === 'localhost' || process.env.DB_HOST === '127.0.0.1';
  poolConfig = {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432', 10),
    database: process.env.DB_NAME || 'drishti360',
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || 'postgres',
    ssl: isLocalhost ? false : { rejectUnauthorized: false },
    max: 20,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 10000
  };
}

const pool = new Pool(poolConfig);

pool.on('error', (err) => {
  console.error('[PostgreSQL Error] Unexpected error on idle client:', err.message);
});

async function query(text, params) {
  const start = Date.now();
  try {
    const res = await pool.query(text, params);
    const duration = Date.now() - start;
    // Optional debug log: console.log('[DB Query]', { text: text.substring(0, 50), duration, rows: res.rowCount });
    return res;
  } catch (err) {
    console.error('[PostgreSQL Query Error]', { text, error: err.message });
    throw err;
  }
}

async function testConnection() {
  try {
    const res = await pool.query('SELECT NOW(), PostGIS_Full_Version()');
    console.log('[PostgreSQL Connected] Server time:', res.rows[0].now);
    console.log('[PostGIS Verified]', res.rows[0].postgis_full_version || 'PostGIS enabled');
    return true;
  } catch (err) {
    console.error('[PostgreSQL Connection Failure] Could not connect to PostgreSQL database:', err.message);
    return false;
  }
}

module.exports = {
  pool,
  query,
  testConnection
};
