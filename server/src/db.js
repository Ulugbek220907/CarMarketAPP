const path = require('path');
const fs = require('fs');
const seedData = require('./seedData');

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
  const dataDir = path.join(__dirname, '..', 'data');
  if (!fs.existsSync(dataDir)) {
    fs.mkdirSync(dataDir, { recursive: true });
  }
  const dbPath = path.join(dataDir, 'drivemarket.db');
  sqliteDb = new sqlite3.Database(dbPath);
  console.log(`[Database] Connected to local SQLite at ${dbPath}`);
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

  // Check if cars exist; if empty, seed them
  const rows = await query('SELECT COUNT(*) as count FROM cars');
  const count = parseInt(rows[0].count || rows[0].COUNT || 0, 10);

  if (count === 0) {
    console.log('[Database] Database is empty. Seeding initial DriveMarket cars...');
    for (const car of seedData.cars) {
      await query(`
        INSERT INTO cars (
          make, model, trim, year, price, mileage, transmission, fuel_type,
          body_style, drivetrain, location, distance, description, seller_name,
          seller_phone, seller_rating, seller_reviews, seller_response, deal_rating,
          carfax_clean, condition, highlights, photo_1, photo_2, photo_3,
          is_favorite, is_user_listing, created_at
        ) VALUES (
          $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14,
          $15, $16, $17, $18, $19, $20, $21, $22, $23, $24, $25, $26, $27, $28
        )
      `, [
        car.make, car.model, car.trim, car.year, car.price, car.mileage, car.transmission, car.fuel_type,
        car.body_style, car.drivetrain, car.location, car.distance, car.description, car.seller_name,
        car.seller_phone, car.seller_rating, car.seller_reviews, car.seller_response, car.deal_rating,
        car.carfax_clean, car.condition, car.highlights, car.photo_1, car.photo_2, car.photo_3,
        car.is_favorite, car.is_user_listing, car.created_at
      ]);
    }

    console.log('[Database] Seeding initial chat messages...');
    for (const msg of seedData.messages) {
      await query(`
        INSERT INTO messages (
          car_id, sender_name, message_text, timestamp, is_from_user,
          is_system, is_official_offer, offer_amount, original_price
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      `, [
        msg.car_id, msg.sender_name, msg.message_text, msg.timestamp,
        msg.is_from_user, msg.is_system, msg.is_official_offer,
        msg.offer_amount, msg.original_price
      ]);
    }
    console.log('[Database] Seeding completed successfully.');
  } else {
    console.log(`[Database] Found ${count} existing vehicles in database.`);
  }
}

module.exports = {
  dbType,
  query,
  initDb
};
