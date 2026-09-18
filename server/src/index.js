require('dotenv').config();
const express = require('express');
const cors = require('cors');
const db = require('./db');
const carsRouter = require('./routes/cars');
const messagesRouter = require('./routes/messages');

const app = express();
const PORT = process.env.PORT || 10000;

// Middlewares
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// Health check endpoint for Render monitoring
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'DriveMarket Cloud Backend',
    database: db.dbType,
    timestamp: new Date().toISOString()
  });
});

// Clear endpoint to wipe all data
app.post('/api/clear', async (req, res) => {
  try {
    await db.query('DELETE FROM messages');
    await db.query('DELETE FROM cars');
    res.json({ success: true, message: 'All vehicles and messages wiped successfully' });
  } catch (err) {
    console.error('Error clearing database:', err);
    res.status(500).json({ error: 'Failed to clear database', details: err.message });
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
