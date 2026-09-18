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
        const val DATABASE_VERSION = 4

        // Cars Table
        const val TABLE_CARS = "cars"
        const val COL_ID = "id"
        const val COL_MAKE = "make"
        const val COL_MODEL = "model"
        const val COL_YEAR = "year"
        const val COL_PRICE = "price"
        const val COL_MILEAGE = "mileage"
        const val COL_TRANSMISSION = "transmission"
        const val COL_BODY_STYLE = "body_style"
        const val COL_LOCATION = "location"
        const val COL_DESCRIPTION = "description"
        const val COL_SELLER_NAME = "seller_name"
        const val COL_SELLER_PHONE = "seller_phone"
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
                $COL_YEAR INTEGER NOT NULL,
                $COL_PRICE REAL NOT NULL,
                $COL_MILEAGE INTEGER NOT NULL,
                $COL_TRANSMISSION TEXT NOT NULL,
                $COL_BODY_STYLE TEXT NOT NULL,
                $COL_LOCATION TEXT NOT NULL,
                $COL_DESCRIPTION TEXT,
                $COL_SELLER_NAME TEXT NOT NULL,
                $COL_SELLER_PHONE TEXT NOT NULL,
                $COL_PHOTO_1 TEXT,
                $COL_PHOTO_2 TEXT,
                $COL_PHOTO_3 TEXT,
                $COL_IS_FAVORITE INTEGER DEFAULT 0,
                $COL_IS_USER_LISTING INTEGER DEFAULT 1,
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
            put(COL_YEAR, car.year)
            put(COL_PRICE, car.price)
            put(COL_MILEAGE, car.mileage)
            put(COL_TRANSMISSION, car.transmission)
            put(COL_BODY_STYLE, car.bodyStyle)
            put(COL_LOCATION, car.location)
            put(COL_DESCRIPTION, car.description)
            put(COL_SELLER_NAME, car.sellerName)
            put(COL_SELLER_PHONE, car.sellerPhone)
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
            // Keep local favorites status
            val favCursor = db.rawQuery("SELECT $COL_ID FROM $TABLE_CARS WHERE $COL_IS_FAVORITE = 1", null)
            val favIds = mutableSetOf<Long>()
            while (favCursor.moveToNext()) {
                favIds.add(favCursor.getLong(0))
            }
            favCursor.close()

            db.delete(TABLE_CARS, null, null)

            for (car in cars) {
                val isFav = car.isFavorite || favIds.contains(car.id)
                val values = ContentValues().apply {
                    if (car.id > 0) put(COL_ID, car.id)
                    put(COL_MAKE, car.make)
                    put(COL_MODEL, car.model)
                    put(COL_YEAR, car.year)
                    put(COL_PRICE, car.price)
                    put(COL_MILEAGE, car.mileage)
                    put(COL_TRANSMISSION, car.transmission)
                    put(COL_BODY_STYLE, car.bodyStyle)
                    put(COL_LOCATION, car.location)
                    put(COL_DESCRIPTION, car.description)
                    put(COL_SELLER_NAME, car.sellerName)
                    put(COL_SELLER_PHONE, car.sellerPhone)
                    put(COL_PHOTO_1, car.photo1)
                    put(COL_PHOTO_2, car.photo2)
                    put(COL_PHOTO_3, car.photo3)
                    put(COL_IS_FAVORITE, if (isFav) 1 else 0)
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
                "($COL_MAKE LIKE ? OR $COL_MODEL LIKE ? OR $COL_LOCATION LIKE ? OR CAST($COL_YEAR AS TEXT) LIKE ? OR $COL_BODY_STYLE LIKE ? OR $COL_SELLER_NAME LIKE ?)"
            )
            selectionArgs.addAll(listOf(query, query, query, query, query, query))
        }

        if (filter.location.isNotBlank() && filter.location != "All Locations") {
            selectionClauses.add("$COL_LOCATION LIKE ?")
            selectionArgs.add("%${filter.location.trim()}%")
        }

        val selection = if (selectionClauses.isNotEmpty()) {
            selectionClauses.joinToString(" AND ")
        } else null

        val orderBy = when (filter.sortOption) {
            SortOption.RECOMMENDED, SortOption.NEWEST -> "$COL_CREATED_AT DESC"
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

    fun clearAllData() {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, null, null)
        db.delete(TABLE_CARS, null, null)
    }

    private fun cursorToCar(cursor: Cursor): Car {
        return Car(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            make = cursor.getString(cursor.getColumnIndexOrThrow(COL_MAKE)),
            model = cursor.getString(cursor.getColumnIndexOrThrow(COL_MODEL)),
            year = cursor.getInt(cursor.getColumnIndexOrThrow(COL_YEAR)),
            price = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRICE)),
            mileage = cursor.getInt(cursor.getColumnIndexOrThrow(COL_MILEAGE)),
            transmission = cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSMISSION)),
            bodyStyle = cursor.getString(cursor.getColumnIndexOrThrow(COL_BODY_STYLE)),
            location = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)) ?: "",
            sellerName = cursor.getString(cursor.getColumnIndexOrThrow(COL_SELLER_NAME)),
            sellerPhone = cursor.getString(cursor.getColumnIndexOrThrow(COL_SELLER_PHONE)),
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
