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
    const res1 = await pool.query('SELECT * FROM camera_activity WHERE camera_id = \'camera-002\' LIMIT 10');
    console.log("CAM 02 ROWS:");
    console.table(res1.rows);

    const res2 = await pool.query('SELECT * FROM camera_activity WHERE camera_id = \'camera-001\' LIMIT 10');
    console.log("CAM 01 ROWS:");
    console.table(res2.rows);

  } catch (err) {
    console.error(err);
  } finally {
    pool.end();
  }
}

verify();
