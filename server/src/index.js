require('dotenv').config();
const express = require('express');
const cors = require('cors');
const db = require('./db');
const carsRouter = require('./routes/cars');
const messagesRouter = require('./routes/messages');
const seedData = require('./seedData');

const app = express();
const PORT = process.env.PORT || 10000;

// Middlewares
app.use(cors());
app.use(express.json({ limit: '15mb' }));
app.use(express.urlencoded({ extended: true, limit: '15mb' }));

// Health check endpoint for Render monitoring
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'DriveMarket Cloud Backend',
    database: db.dbType,
    timestamp: new Date().toISOString()
  });
});

// Seed endpoint to reinitialize demo data
app.post('/api/seed', async (req, res) => {
  try {
    await db.query('DELETE FROM messages');
    await db.query('DELETE FROM cars');

    for (const car of seedData.cars) {
      await db.query(`
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

    for (const msg of seedData.messages) {
      await db.query(`
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

    res.json({ success: true, message: 'Database reset and re-seeded successfully' });
  } catch (err) {
    console.error('Error re-seeding database:', err);
    res.status(500).json({ error: 'Failed to re-seed', details: err.message });
  }
});

// Mount Routes
app.use('/api/cars', carsRouter);
app.use('/api/cars/:carId/messages', messagesRouter);

// Global 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

// Global Error Handler
app.use((err, req, res, next) => {
  console.error('Unhandled server error:', err);
  res.status(500).json({ error: 'Internal server error', details: err.message });
});

// Boot server after database initialization
async function startServer() {
  try {
    await db.initDb();
    app.listen(PORT, '0.0.0.0', () => {
      console.log(`=========================================`);
      console.log(`🚗 DriveMarket Cloud API is live on port ${PORT}`);
      console.log(`📡 Healthcheck: http://localhost:${PORT}/health`);
      console.log(`🚙 Vehicles API: http://localhost:${PORT}/api/cars`);
      console.log(`💾 Database Engine: ${db.dbType}`);
      console.log(`=========================================`);
    });
  } catch (err) {
    console.error('Failed to start server:', err);
    process.exit(1);
  }
}

startServer();
