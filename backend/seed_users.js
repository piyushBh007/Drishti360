const bcrypt = require('bcryptjs');
const { Client } = require('pg');

async function run() {
    const client = new Client({ connectionString: 'postgres://postgres:1234@localhost:5432/drishti360' });
    await client.connect();
    
    // Create users table
    await client.query(`
        CREATE TABLE IF NOT EXISTS users (
            id VARCHAR(50) PRIMARY KEY,
            name TEXT NOT NULL,
            employee_id TEXT UNIQUE,
            username TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL,
            role TEXT NOT NULL DEFAULT 'inspector',
            region TEXT,
            active BOOLEAN DEFAULT TRUE,
            assigned_ngos JSONB,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    `);
    
    const hash = await bcrypt.hash('admin123', 10);
    const inspectorHash = await bcrypt.hash('inspector123', 10);
    
    await client.query(`
        INSERT INTO users (id, name, employee_id, username, password_hash, role, active)
        VALUES 
            ('usr_admin_1', 'System Admin', 'ADM-001', 'admin', $1, 'admin', TRUE)
        ON CONFLICT (username) DO NOTHING;
    `, [hash]);
    
    await client.query(`
        INSERT INTO users (id, name, employee_id, username, password_hash, role, region, active, assigned_ngos)
        VALUES 
            ('usr_insp_1', 'Inspector Aarav Mehta', 'INS-104', 'aarav', $1, 'inspector', 'Rajasthan', TRUE, '["ngo-001", "ngo-002", "ngo-003"]')
        ON CONFLICT (username) DO NOTHING;
    `, [inspectorHash]);
    
    console.log('Users seeded!');
    await client.end();
}

run().catch(console.error);
