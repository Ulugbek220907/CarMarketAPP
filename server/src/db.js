const path = require('path');
const fs = require('fs');

let dbType = 'sqlite';
let pgPool = null;
let sqliteDb = null;

// Determine DB type based on environment
const databaseUrl = process.env.DATABASE_URL;

if (databaseUrl && (databaseUrl.startsWith('postgres://') || databaseUrl.startsWith('postgresql://'))) {
  dbType = 'postgres';
  const { Pool } = require('pg');
  pgPool = new Pool({
    connectionString: databaseUrl,
    ssl: {
      rejectUnauthorized: false
    }
  });
  console.log('[Database] Connected to Render PostgreSQL');
} else {
  dbType = 'sqlite';
  const sqlite3 = require('sqlite3').verbose();
  let dbPath = ':memory:';
  try {
    const dataDir = path.join(__dirname, '..', 'data');
    if (!fs.existsSync(dataDir)) {
      fs.mkdirSync(dataDir, { recursive: true });
    }
    dbPath = path.join(dataDir, 'drivemarket.db');
  } catch (err) {
    console.warn('[Database] Could not write to server/data, trying /tmp:', err.message);
    try {
      dbPath = path.join('/tmp', 'drivemarket.db');
    } catch (e) {
      dbPath = ':memory:';
    }
  }
  sqliteDb = new sqlite3.Database(dbPath);
  console.log(`[Database] Connected to SQLite database at ${dbPath}`);
}

// Unified query wrapper
async function query(sql, params = []) {
  if (dbType === 'postgres') {
    const res = await pgPool.query(sql, params);
    return res.rows;
  } else {
    return new Promise((resolve, reject) => {
      // Convert $1, $2 to ? for sqlite3
      const sqliteSql = sql.replace(/\$(\d+)/g, '?');
      const isSelect = /^\s*(SELECT|PRAGMA)/i.test(sqliteSql);

      if (isSelect) {
        sqliteDb.all(sqliteSql, params, (err, rows) => {
          if (err) return reject(err);
          resolve(rows);
        });
      } else {
        sqliteDb.run(sqliteSql, params, function (err) {
          if (err) return reject(err);
          resolve([{ id: this.lastID, affectedRows: this.changes }]);
        });
      }
    });
  }
}

// Initialize tables and seed
async function initDb() {
  console.log('[Database] Running schema migrations...');

  if (dbType === 'postgres') {
    await query(`
      CREATE TABLE IF NOT EXISTS cars (
        id SERIAL PRIMARY KEY,
        make VARCHAR(100) NOT NULL,
        model VARCHAR(100) NOT NULL,
        trim VARCHAR(150),
        year INT NOT NULL,
        price DOUBLE PRECISION NOT NULL,
        mileage INT NOT NULL,
        transmission VARCHAR(100) NOT NULL,
        fuel_type VARCHAR(100) NOT NULL,
        body_style VARCHAR(100) NOT NULL,
        drivetrain VARCHAR(100),
        location VARCHAR(150) NOT NULL,
        distance VARCHAR(100),
        description TEXT NOT NULL,
        seller_name VARCHAR(150) NOT NULL,
        seller_phone VARCHAR(50) NOT NULL,
        seller_rating DOUBLE PRECISION DEFAULT 4.9,
        seller_reviews INT DEFAULT 42,
        seller_response VARCHAR(100) DEFAULT 'Replies < 15 mins',
        deal_rating VARCHAR(50) DEFAULT 'Great Deal',
        carfax_clean INT DEFAULT 1,
        condition VARCHAR(50) DEFAULT 'Good',
        highlights TEXT,
        photo_1 TEXT,
        photo_2 TEXT,
        photo_3 TEXT,
        is_favorite INT DEFAULT 0,
        is_user_listing INT DEFAULT 0,
        created_at BIGINT NOT NULL
      )
    `);

    await query(`
      CREATE TABLE IF NOT EXISTS messages (
        id SERIAL PRIMARY KEY,
        car_id INT NOT NULL,
        sender_name VARCHAR(150) NOT NULL,
        message_text TEXT NOT NULL,
        timestamp BIGINT NOT NULL,
        is_from_user INT DEFAULT 0,
        is_system INT DEFAULT 0,
        is_official_offer INT DEFAULT 0,
        offer_amount DOUBLE PRECISION DEFAULT 0.0,
        original_price DOUBLE PRECISION DEFAULT 0.0
      )
    `);
  } else {
    await query(`
      CREATE TABLE IF NOT EXISTS cars (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        make TEXT NOT NULL,
        model TEXT NOT NULL,
        trim TEXT,
        year INTEGER NOT NULL,
        price REAL NOT NULL,
        mileage INTEGER NOT NULL,
        transmission TEXT NOT NULL,
        fuel_type TEXT NOT NULL,
        body_style TEXT NOT NULL,
        drivetrain TEXT,
        location TEXT NOT NULL,
        distance TEXT,
        description TEXT NOT NULL,
        seller_name TEXT NOT NULL,
        seller_phone TEXT NOT NULL,
        seller_rating REAL DEFAULT 4.9,
        seller_reviews INTEGER DEFAULT 42,
        seller_response TEXT DEFAULT 'Replies < 15 mins',
        deal_rating TEXT DEFAULT 'Great Deal',
        carfax_clean INTEGER DEFAULT 1,
        condition TEXT DEFAULT 'Good',
        highlights TEXT,
        photo_1 TEXT,
        photo_2 TEXT,
        photo_3 TEXT,
        is_favorite INTEGER DEFAULT 0,
        is_user_listing INTEGER DEFAULT 0,
        created_at INTEGER NOT NULL
      )
    `);

    await query(`
      CREATE TABLE IF NOT EXISTS messages (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        car_id INTEGER NOT NULL,
        sender_name TEXT NOT NULL,
        message_text TEXT NOT NULL,
        timestamp INTEGER NOT NULL,
        is_from_user INTEGER DEFAULT 0,
        is_system INTEGER DEFAULT 0,
        is_official_offer INTEGER DEFAULT 0,
        offer_amount REAL DEFAULT 0.0,
        original_price REAL DEFAULT 0.0
      )
    `);
  }

  const rows = await query('SELECT COUNT(*) as count FROM cars');
  const count = parseInt(rows[0].count || rows[0].COUNT || 0, 10);
  console.log(`[Database] Ready. Total vehicles in database: ${count}`);
}

module.exports = {
  dbType,
  query,
  initDb
};
