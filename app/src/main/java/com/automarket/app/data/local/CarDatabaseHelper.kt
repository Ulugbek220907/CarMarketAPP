package com.automarket.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.automarket.app.data.model.Car
import com.automarket.app.data.model.CarFilter
import com.automarket.app.data.model.CategoryFilter
import com.automarket.app.data.model.ChatMessage
import com.automarket.app.data.model.SortOption

class CarDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "drivemarket.db"
        const val DATABASE_VERSION = 2

        // Cars Table
        const val TABLE_CARS = "cars"
        const val COL_ID = "id"
        const val COL_MAKE = "make"
        const val COL_MODEL = "model"
        const val COL_TRIM = "trim"
        const val COL_YEAR = "year"
        const val COL_PRICE = "price"
        const val COL_MILEAGE = "mileage"
        const val COL_TRANSMISSION = "transmission"
        const val COL_FUEL_TYPE = "fuel_type"
        const val COL_BODY_STYLE = "body_style"
        const val COL_DRIVETRAIN = "drivetrain"
        const val COL_LOCATION = "location"
        const val COL_DISTANCE = "distance"
        const val COL_DESCRIPTION = "description"
        const val COL_SELLER_NAME = "seller_name"
        const val COL_SELLER_PHONE = "seller_phone"
        const val COL_SELLER_RATING = "seller_rating"
        const val COL_SELLER_REVIEWS = "seller_reviews"
        const val COL_SELLER_RESPONSE = "seller_response"
        const val COL_DEAL_RATING = "deal_rating"
        const val COL_CARFAX_CLEAN = "carfax_clean"
        const val COL_CONDITION = "condition"
        const val COL_HIGHLIGHTS = "highlights"
        const val COL_PHOTO_1 = "photo_1"
        const val COL_PHOTO_2 = "photo_2"
        const val COL_PHOTO_3 = "photo_3"
        const val COL_IS_FAVORITE = "is_favorite"
        const val COL_IS_USER_LISTING = "is_user_listing"
        const val COL_CREATED_AT = "created_at"

        // Messages Table
        const val TABLE_MESSAGES = "messages"
        const val COL_MSG_ID = "msg_id"
        const val COL_MSG_CAR_ID = "car_id"
        const val COL_MSG_SENDER = "sender_name"
        const val COL_MSG_TEXT = "message_text"
        const val COL_MSG_TIMESTAMP = "timestamp"
        const val COL_MSG_FROM_USER = "is_from_user"
        const val COL_MSG_SYSTEM = "is_system"
        const val COL_MSG_OFFICIAL_OFFER = "is_official_offer"
        const val COL_MSG_OFFER_AMOUNT = "offer_amount"
        const val COL_MSG_ORIGINAL_PRICE = "original_price"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createCarsTable = """
            CREATE TABLE $TABLE_CARS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MAKE TEXT NOT NULL,
                $COL_MODEL TEXT NOT NULL,
                $COL_TRIM TEXT,
                $COL_YEAR INTEGER NOT NULL,
                $COL_PRICE REAL NOT NULL,
                $COL_MILEAGE INTEGER NOT NULL,
                $COL_TRANSMISSION TEXT NOT NULL,
                $COL_FUEL_TYPE TEXT NOT NULL,
                $COL_BODY_STYLE TEXT NOT NULL,
                $COL_DRIVETRAIN TEXT,
                $COL_LOCATION TEXT NOT NULL,
                $COL_DISTANCE TEXT,
                $COL_DESCRIPTION TEXT NOT NULL,
                $COL_SELLER_NAME TEXT NOT NULL,
                $COL_SELLER_PHONE TEXT NOT NULL,
                $COL_SELLER_RATING REAL DEFAULT 4.9,
                $COL_SELLER_REVIEWS INTEGER DEFAULT 42,
                $COL_SELLER_RESPONSE TEXT DEFAULT 'Replies < 15 mins',
                $COL_DEAL_RATING TEXT DEFAULT 'Great Deal',
                $COL_CARFAX_CLEAN INTEGER DEFAULT 1,
                $COL_CONDITION TEXT DEFAULT 'Good',
                $COL_HIGHLIGHTS TEXT,
                $COL_PHOTO_1 TEXT,
                $COL_PHOTO_2 TEXT,
                $COL_PHOTO_3 TEXT,
                $COL_IS_FAVORITE INTEGER DEFAULT 0,
                $COL_IS_USER_LISTING INTEGER DEFAULT 0,
                $COL_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        val createMessagesTable = """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_MSG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MSG_CAR_ID INTEGER NOT NULL,
                $COL_MSG_SENDER TEXT NOT NULL,
                $COL_MSG_TEXT TEXT NOT NULL,
                $COL_MSG_TIMESTAMP INTEGER NOT NULL,
                $COL_MSG_FROM_USER INTEGER DEFAULT 0,
                $COL_MSG_SYSTEM INTEGER DEFAULT 0,
                $COL_MSG_OFFICIAL_OFFER INTEGER DEFAULT 0,
                $COL_MSG_OFFER_AMOUNT REAL DEFAULT 0.0,
                $COL_MSG_ORIGINAL_PRICE REAL DEFAULT 0.0
            )
        """.trimIndent()

        db.execSQL(createCarsTable)
        db.execSQL(createMessagesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CARS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    fun insertCar(car: Car): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            if (car.id > 0) put(COL_ID, car.id)
            put(COL_MAKE, car.make)
            put(COL_MODEL, car.model)
            put(COL_TRIM, car.trim)
            put(COL_YEAR, car.year)
            put(COL_PRICE, car.price)
            put(COL_MILEAGE, car.mileage)
            put(COL_TRANSMISSION, car.transmission)
            put(COL_FUEL_TYPE, car.fuelType)
            put(COL_BODY_STYLE, car.bodyStyle)
            put(COL_DRIVETRAIN, car.drivetrain)
            put(COL_LOCATION, car.location)
            put(COL_DISTANCE, car.distance)
            put(COL_DESCRIPTION, car.description)
            put(COL_SELLER_NAME, car.sellerName)
            put(COL_SELLER_PHONE, car.sellerPhone)
            put(COL_SELLER_RATING, car.sellerRating)
            put(COL_SELLER_REVIEWS, car.sellerReviewCount)
            put(COL_SELLER_RESPONSE, car.sellerResponseTime)
            put(COL_DEAL_RATING, car.dealRating)
            put(COL_CARFAX_CLEAN, if (car.carfaxClean) 1 else 0)
            put(COL_CONDITION, car.condition)
            put(COL_HIGHLIGHTS, car.highlights)
            put(COL_PHOTO_1, car.photo1)
            put(COL_PHOTO_2, car.photo2)
            put(COL_PHOTO_3, car.photo3)
            put(COL_IS_FAVORITE, if (car.isFavorite) 1 else 0)
            put(COL_IS_USER_LISTING, if (car.isUserListing) 1 else 0)
            put(COL_CREATED_AT, car.createdAt)
        }
        return db.insertWithOnConflict(TABLE_CARS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun syncCars(cars: List<Car>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (car in cars) {
                val values = ContentValues().apply {
                    if (car.id > 0) put(COL_ID, car.id)
                    put(COL_MAKE, car.make)
                    put(COL_MODEL, car.model)
                    put(COL_TRIM, car.trim)
                    put(COL_YEAR, car.year)
                    put(COL_PRICE, car.price)
                    put(COL_MILEAGE, car.mileage)
                    put(COL_TRANSMISSION, car.transmission)
                    put(COL_FUEL_TYPE, car.fuelType)
                    put(COL_BODY_STYLE, car.bodyStyle)
                    put(COL_DRIVETRAIN, car.drivetrain)
                    put(COL_LOCATION, car.location)
                    put(COL_DISTANCE, car.distance)
                    put(COL_DESCRIPTION, car.description)
                    put(COL_SELLER_NAME, car.sellerName)
                    put(COL_SELLER_PHONE, car.sellerPhone)
                    put(COL_SELLER_RATING, car.sellerRating)
                    put(COL_SELLER_REVIEWS, car.sellerReviewCount)
                    put(COL_SELLER_RESPONSE, car.sellerResponseTime)
                    put(COL_DEAL_RATING, car.dealRating)
                    put(COL_CARFAX_CLEAN, if (car.carfaxClean) 1 else 0)
                    put(COL_CONDITION, car.condition)
                    put(COL_HIGHLIGHTS, car.highlights)
                    put(COL_PHOTO_1, car.photo1)
                    put(COL_PHOTO_2, car.photo2)
                    put(COL_PHOTO_3, car.photo3)
                    put(COL_IS_FAVORITE, if (car.isFavorite) 1 else 0)
                    put(COL_IS_USER_LISTING, if (car.isUserListing) 1 else 0)
                    put(COL_CREATED_AT, car.createdAt)
                }
                db.insertWithOnConflict(TABLE_CARS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun syncMessages(carId: Long, messages: List<ChatMessage>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_MESSAGES, "$COL_MSG_CAR_ID = ?", arrayOf(carId.toString()))
            for (msg in messages) {
                val values = ContentValues().apply {
                    if (msg.id > 0) put(COL_MSG_ID, msg.id)
                    put(COL_MSG_CAR_ID, msg.carId)
                    put(COL_MSG_SENDER, msg.senderName)
                    put(COL_MSG_TEXT, msg.messageText)
                    put(COL_MSG_TIMESTAMP, msg.timestamp)
                    put(COL_MSG_FROM_USER, if (msg.isFromUser) 1 else 0)
                    put(COL_MSG_SYSTEM, if (msg.isSystemNotification) 1 else 0)
                    put(COL_MSG_OFFICIAL_OFFER, if (msg.isOfficialOffer) 1 else 0)
                    put(COL_MSG_OFFER_AMOUNT, msg.offerAmount)
                    put(COL_MSG_ORIGINAL_PRICE, msg.originalListPrice)
                }
                db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteCar(carId: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_CARS, "$COL_ID = ?", arrayOf(carId.toString()))
        return rows > 0
    }

    fun toggleFavorite(carId: Long): Boolean {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT $COL_IS_FAVORITE FROM $TABLE_CARS WHERE $COL_ID = ?", arrayOf(carId.toString()))
        var newStatus = false
        if (cursor.moveToFirst()) {
            val current = cursor.getInt(0) == 1
            newStatus = !current
            val values = ContentValues().apply {
                put(COL_IS_FAVORITE, if (newStatus) 1 else 0)
            }
            db.update(TABLE_CARS, values, "$COL_ID = ?", arrayOf(carId.toString()))
        }
        cursor.close()
        return newStatus
    }

    fun getCarById(carId: Long): Car? {
        val db = readableDatabase
        val cursor = db.query(TABLE_CARS, null, "$COL_ID = ?", arrayOf(carId.toString()), null, null, null)
        var car: Car? = null
        if (cursor.moveToFirst()) {
            car = cursorToCar(cursor)
        }
        cursor.close()
        return car
    }

    fun getCars(filter: CarFilter): List<Car> {
        val db = readableDatabase
        val selectionArgs = mutableListOf<String>()
        val selectionClauses = mutableListOf<String>()

        if (filter.searchQuery.isNotBlank()) {
            val query = "%${filter.searchQuery.trim()}%"
            selectionClauses.add(
                "($COL_MAKE LIKE ? OR $COL_MODEL LIKE ? OR $COL_LOCATION LIKE ? OR CAST($COL_YEAR AS TEXT) LIKE ? OR $COL_BODY_STYLE LIKE ?)"
            )
            selectionArgs.addAll(listOf(query, query, query, query, query))
        }

        when (filter.category) {
            CategoryFilter.ALL -> {}
            CategoryFilter.SEDAN -> selectionClauses.add("$COL_BODY_STYLE = 'Sedan'")
            CategoryFilter.SUV -> selectionClauses.add("$COL_BODY_STYLE = 'SUV'")
            CategoryFilter.COUPE -> selectionClauses.add("$COL_BODY_STYLE = 'Coupe'")
            CategoryFilter.ELECTRIC -> selectionClauses.add("$COL_FUEL_TYPE = 'Electric'")
            CategoryFilter.HYBRID -> selectionClauses.add("$COL_FUEL_TYPE = 'Hybrid'")
            CategoryFilter.TRUCK -> selectionClauses.add("$COL_BODY_STYLE = 'Truck'")
            CategoryFilter.LUXURY -> selectionClauses.add("($COL_PRICE >= 50000 OR $COL_MAKE = 'Porsche' OR $COL_MAKE = 'BMW')")
            CategoryFilter.UNDER_15K -> selectionClauses.add("$COL_PRICE < 15000")
            CategoryFilter.UNDER_25K -> selectionClauses.add("$COL_PRICE < 25000")
            CategoryFilter.UNDER_30K -> selectionClauses.add("$COL_PRICE < 30000")
            CategoryFilter.LOW_MILES -> selectionClauses.add("$COL_MILEAGE < 30000")
            CategoryFilter.CERTIFIED -> selectionClauses.add("$COL_CARFAX_CLEAN = 1")
        }

        val selection = if (selectionClauses.isNotEmpty()) {
            selectionClauses.joinToString(" AND ")
        } else null

        val orderBy = when (filter.sortOption) {
            SortOption.RECOMMENDED -> "$COL_CREATED_AT DESC"
            SortOption.NEWEST -> "$COL_CREATED_AT DESC"
            SortOption.PRICE_ASC -> "$COL_PRICE ASC"
            SortOption.PRICE_DESC -> "$COL_PRICE DESC"
            SortOption.MILEAGE_ASC -> "$COL_MILEAGE ASC"
        }

        val cursor = db.query(TABLE_CARS, null, selection, selectionArgs.toTypedArray(), null, null, orderBy)
        val list = mutableListOf<Car>()
        while (cursor.moveToNext()) {
            list.add(cursorToCar(cursor))
        }
        cursor.close()
        return list
    }

    fun getFavoriteCars(): List<Car> {
        val db = readableDatabase
        val cursor = db.query(TABLE_CARS, null, "$COL_IS_FAVORITE = 1", null, null, null, "$COL_CREATED_AT DESC")
        val list = mutableListOf<Car>()
        while (cursor.moveToNext()) {
            list.add(cursorToCar(cursor))
        }
        cursor.close()
        return list
    }

    fun getUserListings(): List<Car> {
        val db = readableDatabase
        val cursor = db.query(TABLE_CARS, null, "$COL_IS_USER_LISTING = 1", null, null, null, "$COL_CREATED_AT DESC")
        val list = mutableListOf<Car>()
        while (cursor.moveToNext()) {
            list.add(cursorToCar(cursor))
        }
        cursor.close()
        return list
    }

    fun getCarCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_CARS", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    // --- Messages & Offers ---

    fun insertMessage(msg: ChatMessage): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_MSG_CAR_ID, msg.carId)
            put(COL_MSG_SENDER, msg.senderName)
            put(COL_MSG_TEXT, msg.messageText)
            put(COL_MSG_TIMESTAMP, msg.timestamp)
            put(COL_MSG_FROM_USER, if (msg.isFromUser) 1 else 0)
            put(COL_MSG_SYSTEM, if (msg.isSystemNotification) 1 else 0)
            put(COL_MSG_OFFICIAL_OFFER, if (msg.isOfficialOffer) 1 else 0)
            put(COL_MSG_OFFER_AMOUNT, msg.offerAmount)
            put(COL_MSG_ORIGINAL_PRICE, msg.originalListPrice)
        }
        return db.insert(TABLE_MESSAGES, null, values)
    }

    fun getMessagesForCar(carId: Long): List<ChatMessage> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            "$COL_MSG_CAR_ID = ?",
            arrayOf(carId.toString()),
            null,
            null,
            "$COL_MSG_TIMESTAMP ASC"
        )
        val list = mutableListOf<ChatMessage>()
        while (cursor.moveToNext()) {
            list.add(cursorToMessage(cursor))
        }
        cursor.close()
        return list
    }

    fun seedInitialMessagesIfEmpty(carId: Long, sellerName: String, listPrice: Double) {
        val current = getMessagesForCar(carId)
        if (current.isEmpty()) {
            val now = System.currentTimeMillis()
            // 1. Buyer opening
            insertMessage(
                ChatMessage(
                    carId = carId,
                    senderName = "Alex",
                    messageText = "Hi $sellerName, is the car still available? Clean title in hand?",
                    timestamp = now - (1000L * 60 * 25),
                    isFromUser = true
                )
            )
            // 2. Seller response
            insertMessage(
                ChatMessage(
                    carId = carId,
                    senderName = sellerName,
                    messageText = "Hi Alex! Yes, title is clean and ready. Battery health is tested at 96%.",
                    timestamp = now - (1000L * 60 * 22),
                    isFromUser = false
                )
            )
            // 3. System offer notification
            insertMessage(
                ChatMessage(
                    carId = carId,
                    senderName = "System",
                    messageText = "Alex made an offer of $29,800",
                    timestamp = now - (1000L * 60 * 18),
                    isSystemNotification = true
                )
            )
            // 4. Official Offer Card
            insertMessage(
                ChatMessage(
                    carId = carId,
                    senderName = "Official Offer",
                    messageText = "Official Offer Received",
                    timestamp = now - (1000L * 60 * 15),
                    isOfficialOffer = true,
                    offerAmount = 29800.0,
                    originalListPrice = listPrice
                )
            )
        }
    }

    private fun cursorToCar(cursor: Cursor): Car {
        return Car(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            make = cursor.getString(cursor.getColumnIndexOrThrow(COL_MAKE)),
            model = cursor.getString(cursor.getColumnIndexOrThrow(COL_MODEL)),
            trim = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRIM)) ?: "",
            year = cursor.getInt(cursor.getColumnIndexOrThrow(COL_YEAR)),
            price = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRICE)),
            mileage = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MILEAGE)),
            transmission = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSMISSION)),
            fuelType = cursor.getString(cursor.getColumnIndexOrThrow(COL_FUEL_TYPE)),
            bodyStyle = cursor.getString(cursor.getColumnIndexOrThrow(COL_BODY_STYLE)),
            drivetrain = cursor.getString(cursor.getColumnIndexOrThrow(COL_DRIVETRAIN)) ?: "AWD",
            location = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION)),
            distance = cursor.getString(cursor.getColumnIndexOrThrow(COL_DISTANCE)) ?: "5 miles away",
            description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)),
            sellerName = cursor.getString(cursor.getColumnIndexOrThrow(COL_SELLER_NAME)),
            sellerPhone = cursor.getString(cursor.getColumnIndexOrThrow(COL_SELLER_PHONE)),
            sellerRating = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SELLER_RATING)),
            sellerReviewCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SELLER_REVIEWS)),
            sellerResponseTime = cursor.getString(cursor.getColumnIndexOrThrow(COL_SELLER_RESPONSE)) ?: "Replies < 15 mins",
            dealRating = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEAL_RATING)) ?: "Great Deal",
            carfaxClean = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CARFAX_CLEAN)) == 1,
            condition = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONDITION)) ?: "Good",
            highlights = cursor.getString(cursor.getColumnIndexOrThrow(COL_HIGHLIGHTS)) ?: "",
            photo1 = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_1)),
            photo2 = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_2)),
            photo3 = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_3)),
            isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_FAVORITE)) == 1,
            isUserListing = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_USER_LISTING)) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT))
        )
    }

    private fun cursorToMessage(cursor: Cursor): ChatMessage {
        return ChatMessage(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_MSG_ID)),
            carId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_MSG_CAR_ID)),
            senderName = cursor.getString(cursor.getColumnIndexOrThrow(COL_MSG_SENDER)),
            messageText = cursor.getString(cursor.getColumnIndexOrThrow(COL_MSG_TEXT)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_MSG_TIMESTAMP)),
            isFromUser = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MSG_FROM_USER)) == 1,
            isSystemNotification = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MSG_SYSTEM)) == 1,
            isOfficialOffer = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MSG_OFFICIAL_OFFER)) == 1,
            offerAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_MSG_OFFER_AMOUNT)),
            originalListPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_MSG_ORIGINAL_PRICE))
        )
    }
}
