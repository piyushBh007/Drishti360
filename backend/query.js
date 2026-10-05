const db = require('./db/connection.js');
async function run() {
  const res = await db.query('SELECT id, name, latitude, longitude, ST_AsText(location) FROM ngos ORDER BY created_at DESC LIMIT 5;');
  console.log(res.rows);
  process.exit(0);
}
run();
