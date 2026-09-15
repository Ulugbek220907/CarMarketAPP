const db = require('../src/db');
const seedData = require('../src/seedData');

async function testDatabase() {
  console.log('Testing Database initialization...');
  await db.initDb();

  console.log('Testing cars query...');
  const cars = await db.query('SELECT * FROM cars');
  console.log(`Cars count: ${cars.length}`);
  if (cars.length !== 6) {
    throw new Error(`Expected 6 cars, got ${cars.length}`);
  }

  console.log('Testing messages query...');
  const messages = await db.query('SELECT * FROM messages WHERE car_id = 1');
  console.log(`Messages count for car 1: ${messages.length}`);
  if (messages.length !== 4) {
    throw new Error(`Expected 4 messages, got ${messages.length}`);
  }

  console.log('Database test PASSED! All tables and seeds verified.');
  process.exit(0);
}

testDatabase().catch(err => {
  console.error('Test failed:', err);
  process.exit(1);
});
