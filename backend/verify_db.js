const { Pool } = require('pg');

const pool = new Pool({
  user: 'postgres',
  host: 'localhost',
  database: 'drishti360',
  password: '1234',
  port: 5432,
});

async function verify() {
  try {
    const res1 = await pool.query('SELECT * FROM inspections ORDER BY created_at DESC LIMIT 5');
    console.log("INSPECTIONS:");
    console.table(res1.rows);
    
    const res2 = await pool.query('SELECT * FROM audit_events ORDER BY created_at DESC LIMIT 5');
    console.log("AUDIT EVENTS:");
    console.table(res2.rows);
  } catch (err) {
    console.error(err);
  } finally {
    pool.end();
  }
}

verify();
