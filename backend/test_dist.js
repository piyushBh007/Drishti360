const { Client } = require('pg'); 
const c = new Client({user:'postgres', host:'localhost', database:'drishti360', password:'1234', port:5432}); 
c.connect().then(()=>c.query("SELECT name, ST_Distance(location, ST_SetSRID(ST_MakePoint(-122.084000, 37.421998), 4326)::geography) AS distance_meters FROM ngos WHERE id IN ('ngo-001', 'ngo-002')")).then(r=>{console.table(r.rows); process.exit(0)}).catch(console.error);
