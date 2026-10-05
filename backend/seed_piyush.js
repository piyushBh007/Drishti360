const bcrypt = require('bcryptjs');
const { Client } = require('pg');

async function run() {
    const client = new Client({ connectionString: 'postgres://postgres:1234@localhost:5432/drishti360' });
    await client.connect();
    
    try {
        const username = 'piyush';
        
        // Check if user already exists
        const res = await client.query('SELECT id FROM users WHERE username = $1', [username]);
        if (res.rows.length === 0) {
            console.log(`User ${username} does not exist. Creating...`);
            
            const hash = await bcrypt.hash('1234', 10);
            
            await client.query(`
                INSERT INTO users (id, name, employee_id, username, password_hash, role, active)
                VALUES 
                    ($1, $2, $3, $4, $5, $6, $7)
                ON CONFLICT (username) DO NOTHING;
            `, ['usr_admin_piyush', 'Piyush', 'ADM-PIYUSH', username, hash, 'admin', true]);
            
            console.log(`User ${username} created successfully!`);
        } else {
            console.log(`User ${username} already exists. Skipping.`);
        }
        
        // Verify via SQL
        const verify = await client.query('SELECT id, name, username, role, active FROM users WHERE username = $1', [username]);
        console.log('Verification result:', verify.rows[0]);
    } catch (err) {
        console.error('Error seeding user:', err);
    } finally {
        await client.end();
    }
}

run();
