const express = require('express');
const router = express.Router({ mergeParams: true });
const db = require('../db');

function formatMessage(row) {
  if (!row) return null;
  return {
    id: Number(row.id),
    carId: Number(row.car_id),
    senderName: row.sender_name || '',
    messageText: row.message_text || '',
    timestamp: Number(row.timestamp || Date.now()),
    isFromUser: Boolean(row.is_from_user === 1 || row.is_from_user === true),
    isSystemNotification: Boolean(row.is_system === 1 || row.is_system === true),
    isOfficialOffer: Boolean(row.is_official_offer === 1 || row.is_official_offer === true),
    offerAmount: Number(row.offer_amount || 0.0),
    originalListPrice: Number(row.original_price || 0.0)
  };
}

// GET /api/cars/:carId/messages
router.get('/', async (req, res) => {
  try {
    const carId = parseInt(req.params.carId, 10);
    const rows = await db.query(
      'SELECT * FROM messages WHERE car_id = $1 ORDER BY timestamp ASC',
      [carId]
    );
    res.json(rows.map(formatMessage));
  } catch (err) {
    console.error('Error getting messages:', err);
    res.status(500).json({ error: 'Failed to retrieve messages', details: err.message });
  }
});

// POST /api/cars/:carId/messages - Send a message
router.post('/', async (req, res) => {
  try {
    const carId = parseInt(req.params.carId, 10);
    const b = req.body;
    const timestamp = b.timestamp || Date.now();

    const sql = `
      INSERT INTO messages (
        car_id, sender_name, message_text, timestamp,
        is_from_user, is_system, is_official_offer,
        offer_amount, original_price
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      ${db.dbType === 'postgres' ? 'RETURNING *' : ''}
    `;

    const params = [
      carId,
      b.senderName || 'Alex',
      b.messageText || '',
      timestamp,
      b.isFromUser ? 1 : 0,
      b.isSystemNotification ? 1 : 0,
      b.isOfficialOffer ? 1 : 0,
      parseFloat(b.offerAmount || 0.0),
      parseFloat(b.originalListPrice || 0.0)
    ];

    const result = await db.query(sql, params);

    let createdMessage;
    if (db.dbType === 'postgres' && result.length > 0) {
      createdMessage = formatMessage(result[0]);
    } else {
      const insertId = result[0]?.id;
      const createdRows = await db.query('SELECT * FROM messages WHERE id = $1', [insertId]);
      createdMessage = formatMessage(createdRows[0]);
    }

    res.status(201).json(createdMessage);
  } catch (err) {
    console.error('Error sending message:', err);
    res.status(500).json({ error: 'Failed to post message', details: err.message });
  }
});

// POST /api/cars/:carId/offers - Submit official offer
router.post('/offers', async (req, res) => {
  try {
    const carId = parseInt(req.params.carId, 10);
    const { offerAmount, originalListPrice, buyerName } = req.body;
    const now = Date.now();

    // 1. Insert system alert
    await db.query(`
      INSERT INTO messages (
        car_id, sender_name, message_text, timestamp,
        is_from_user, is_system, is_official_offer, offer_amount, original_price
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
    `, [
      carId,
      'System',
      `${buyerName || 'Alex'} made an offer of $${parseFloat(offerAmount).toLocaleString()}`,
      now,
      0, 1, 0, 0, 0
    ]);

    // 2. Insert official offer card
    const sql = `
      INSERT INTO messages (
        car_id, sender_name, message_text, timestamp,
        is_from_user, is_system, is_official_offer, offer_amount, original_price
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      ${db.dbType === 'postgres' ? 'RETURNING *' : ''}
    `;

    const result = await db.query(sql, [
      carId,
      'Official Offer',
      'Official Offer Received',
      now + 100,
      0, 0, 1,
      parseFloat(offerAmount || 0),
      parseFloat(originalListPrice || 0)
    ]);

    let createdOffer;
    if (db.dbType === 'postgres' && result.length > 0) {
      createdOffer = formatMessage(result[0]);
    } else {
      const insertId = result[0]?.id;
      const createdRows = await db.query('SELECT * FROM messages WHERE id = $1', [insertId]);
      createdOffer = formatMessage(createdRows[0]);
    }

    res.status(201).json(createdOffer);
  } catch (err) {
    console.error('Error creating offer:', err);
    res.status(500).json({ error: 'Failed to create offer', details: err.message });
  }
});

module.exports = router;
