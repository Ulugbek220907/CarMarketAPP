const express = require('express');
const router = express.Router();
const db = require('../db');

function formatCar(row) {
  if (!row) return null;
  return {
    id: Number(row.id),
    make: row.make || '',
    model: row.model || '',
    year: Number(row.year || 0),
    price: Number(row.price || 0),
    mileage: Number(row.mileage || 0),
    transmission: row.transmission || 'Automatic',
    bodyStyle: row.body_style || 'Sedan',
    location: row.location || '',
    description: row.description || '',
    sellerName: row.seller_name || 'Seller',
    sellerPhone: row.seller_phone || '',
    photo1: row.photo_1 || null,
    photo2: row.photo_2 || null,
    photo3: row.photo_3 || null,
    isFavorite: Boolean(row.is_favorite === 1 || row.is_favorite === true),
    isUserListing: Boolean(row.is_user_listing === 1 || row.is_user_listing === true),
    createdAt: Number(row.created_at || Date.now())
  };
}

// GET /api/cars - List cars with filters
router.get('/', async (req, res) => {
  try {
    const { search, favorites, userListings, location } = req.query;

    let sql = 'SELECT * FROM cars WHERE 1=1';
    const params = [];
    let pIdx = 1;

    if (favorites === 'true' || favorites === '1') {
      sql += ` AND is_favorite = 1`;
    }

    if (userListings === 'true' || userListings === '1') {
      sql += ` AND is_user_listing = 1`;
    }

    if (location && location.trim().length > 0 && location !== 'All Locations') {
      sql += ` AND LOWER(location) LIKE $${pIdx}`;
      params.push(`%${location.trim().toLowerCase()}%`);
      pIdx++;
    }

    if (search && search.trim().length > 0) {
      const term = `%${search.trim().toLowerCase()}%`;
      sql += ` AND (LOWER(make) LIKE $${pIdx} OR LOWER(model) LIKE $${pIdx + 1} OR LOWER(location) LIKE $${pIdx + 2} OR CAST(year AS TEXT) LIKE $${pIdx + 3} OR LOWER(body_style) LIKE $${pIdx + 4} OR LOWER(seller_name) LIKE $${pIdx + 5})`;
      params.push(term, term, term, term, term, term);
      pIdx += 6;
    }

    sql += ' ORDER BY created_at DESC';

    const rows = await db.query(sql, params);
    const cars = rows.map(formatCar);
    res.json(cars);
  } catch (err) {
    console.error('Error fetching cars:', err);
    res.status(500).json({ error: 'Failed to fetch vehicles', details: err.message });
  }
});

// GET /api/cars/:id - Single car detail
router.get('/:id', async (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    const rows = await db.query('SELECT * FROM cars WHERE id = $1', [id]);
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Vehicle not found' });
    }
    res.json(formatCar(rows[0]));
  } catch (err) {
    console.error('Error fetching car detail:', err);
    res.status(500).json({ error: 'Failed to fetch vehicle detail', details: err.message });
  }
});

// POST /api/cars - Create a new listing
router.post('/', async (req, res) => {
  try {
    const b = req.body;
    const createdAt = b.createdAt || Date.now();

    const sql = `
      INSERT INTO cars (
        make, model, year, price, mileage, transmission, body_style,
        location, description, seller_name, seller_phone,
        photo_1, photo_2, photo_3, is_favorite, is_user_listing, created_at
      ) VALUES (
        $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17
      ) ${db.dbType === 'postgres' ? 'RETURNING *' : ''}
    `;

    const params = [
      b.make || 'Vehicle',
      b.model || 'Model',
      parseInt(b.year || 2023, 10),
      parseFloat(b.price || 0),
      parseInt(b.mileage || 0, 10),
      b.transmission || 'Automatic',
      b.bodyStyle || 'Sedan',
      b.location || '',
      b.description || '',
      b.sellerName || 'Seller',
      b.sellerPhone || '',
      b.photo1 || null,
      b.photo2 || null,
      b.photo3 || null,
      b.isFavorite ? 1 : 0,
      1, // is_user_listing = true
      createdAt
    ];

    const result = await db.query(sql, params);

    let createdCar;
    if (db.dbType === 'postgres' && result.length > 0) {
      createdCar = formatCar(result[0]);
    } else {
      const insertId = result[0]?.id;
      const createdRows = await db.query('SELECT * FROM cars WHERE id = $1', [insertId]);
      createdCar = formatCar(createdRows[0]);
    }

    res.status(201).json(createdCar);
  } catch (err) {
    console.error('Error creating car listing:', err);
    res.status(500).json({ error: 'Failed to create listing', details: err.message });
  }
});

// POST /api/cars/:id/favorite - Toggle favorite
router.post('/:id/favorite', async (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    const existing = await db.query('SELECT is_favorite FROM cars WHERE id = $1', [id]);
    if (existing.length === 0) {
      return res.status(404).json({ error: 'Vehicle not found' });
    }

    const currentFav = existing[0].is_favorite === 1 || existing[0].is_favorite === true;
    const newStatus = currentFav ? 0 : 1;

    await db.query('UPDATE cars SET is_favorite = $1 WHERE id = $2', [newStatus, id]);
    res.json({ id, isFavorite: Boolean(newStatus === 1) });
  } catch (err) {
    console.error('Error toggling favorite:', err);
    res.status(500).json({ error: 'Failed to toggle favorite', details: err.message });
  }
});

// DELETE /api/cars/:id - Delete car
router.delete('/:id', async (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    await db.query('DELETE FROM messages WHERE car_id = $1', [id]);
    await db.query('DELETE FROM cars WHERE id = $1', [id]);
    res.json({ success: true, id });
  } catch (err) {
    console.error('Error deleting car:', err);
    res.status(500).json({ error: 'Failed to delete vehicle', details: err.message });
  }
});

module.exports = router;
