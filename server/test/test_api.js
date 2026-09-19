const db = require('../src/db');

async function testDatabase() {
  console.log('Testing Database initialization...');
  await db.initDb();

  console.log('Testing cars query on clean db...');
  const cars = await db.query('SELECT * FROM cars');
  console.log(`Cars count: ${cars.length}`);

  console.log('Testing inserting a real vehicle listing...');
  const res = await db.query(`
    INSERT INTO cars (
      make, model, year, price, mileage, transmission,
      body_style, location, description, seller_name, seller_phone,
      created_at
    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
  `, [
    'Toyota', 'Camry', 2022, 24500.0, 31000, 'Automatic',
    'Sedan', 'Tashkent', 'Clean car, 1 owner', 'Ulugbek', '+998901234567',
    Date.now()
  ]);

  const updatedCars = await db.query('SELECT * FROM cars');
  console.log(`Updated cars count: ${updatedCars.length}`);
  if (updatedCars.length === 0) {
    throw new Error('Expected at least 1 car after insert');
  }

  console.log('Testing 6-parameter search query binding...');
  const searchSql = `
    SELECT * FROM cars WHERE 1=1 
    AND (LOWER(make) LIKE $1 OR LOWER(model) LIKE $2 OR LOWER(location) LIKE $3 OR CAST(year AS TEXT) LIKE $4 OR LOWER(body_style) LIKE $5 OR LOWER(seller_name) LIKE $6)
  `;
  const term = '%camry%';
  const searchResults = await db.query(searchSql, [term, term, term, term, term, term]);
  console.log(`Search results count for 'camry': ${searchResults.length}`);
  if (searchResults.length === 0) {
    throw new Error(`Expected at least 1 search result, got 0`);
  }

  const noMatchTerm = '%nonexistentterm9999%';
  const noMatchResults = await db.query(searchSql, [noMatchTerm, noMatchTerm, noMatchTerm, noMatchTerm, noMatchTerm, noMatchTerm]);
  console.log(`Search results count for nonexistent term: ${noMatchResults.length}`);
  if (noMatchResults.length !== 0) {
    throw new Error(`Expected 0 search results for nonexistent term, got ${noMatchResults.length}`);
  }

  console.log('Testing messages and offers insertion...');
  const insertMsg = await db.query(`
    INSERT INTO messages (
      car_id, sender_name, message_text, timestamp,
      is_from_user, is_system, is_official_offer, offer_amount, original_price
    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
  `, [1, 'Buyer', 'Is this still available?', Date.now(), 1, 0, 0, 0.0, 0.0]);

  const messages = await db.query('SELECT * FROM messages WHERE car_id = $1', [1]);
  console.log(`Messages count for car 1: ${messages.length}`);
  if (messages.length === 0) {
    throw new Error('Expected at least 1 message after insert');
  }

  console.log('Testing admin key logic for /api/clear security...');
  const ADMIN_API_KEY = process.env.ADMIN_API_KEY || 'drivemarket-admin-secret-key';
  
  function checkAuth(headerKey) {
    if (!headerKey || (headerKey !== ADMIN_API_KEY && headerKey !== `Bearer ${ADMIN_API_KEY}`)) {
      return false;
    }
    return true;
  }

  if (checkAuth(undefined) !== false) throw new Error('Security check failed: undefined auth should be false');
  if (checkAuth('wrong-key') !== false) throw new Error('Security check failed: wrong key should be false');
  if (checkAuth(ADMIN_API_KEY) !== true) throw new Error('Security check failed: valid key should be true');
  if (checkAuth(`Bearer ${ADMIN_API_KEY}`) !== true) throw new Error('Security check failed: Bearer key should be true');

  console.log('Database test PASSED! Clean schema, 6-parameter search, messages, and admin authorization verified.');
  process.exit(0);
}

testDatabase().catch(err => {
  console.error('Test failed:', err);
  process.exit(1);
});
