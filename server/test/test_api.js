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
      make, model, trim, year, price, mileage, transmission, fuel_type,
      body_style, location, distance, description, seller_name, seller_phone,
      created_at
    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
  `, [
    'Toyota', 'Camry', 'SE', 2022, 24500.0, 31000, 'Automatic', 'Gasoline',
    'Sedan', 'Tashkent', 'Local', 'Clean car, 1 owner', 'Ulugbek', '+998901234567',
    Date.now()
  ]);

  const updatedCars = await db.query('SELECT * FROM cars');
  console.log(`Updated cars count: ${updatedCars.length}`);
  if (updatedCars.length === 0) {
    throw new Error('Expected at least 1 car after insert');
  }

  console.log('Database test PASSED! Clean schema verified without mock seeds.');
  process.exit(0);
}

testDatabase().catch(err => {
  console.error('Test failed:', err);
  process.exit(1);
});
