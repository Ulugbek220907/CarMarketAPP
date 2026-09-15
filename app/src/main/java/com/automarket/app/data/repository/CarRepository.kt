package com.automarket.app.data.repository

import android.content.Context
import android.util.Log
import com.automarket.app.data.api.ApiClient
import com.automarket.app.data.local.CarDatabaseHelper
import com.automarket.app.data.model.Car
import com.automarket.app.data.model.CarFilter
import com.automarket.app.data.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarRepository(private val context: Context) {

    private val dbHelper = CarDatabaseHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        ApiClient.init(context)
        // Auto-sync with Render cloud backend on startup
        scope.launch {
            try {
                refreshCarsFromBackend()
            } catch (e: Exception) {
                Log.w("CarRepository", "Initial cloud sync deferred: ${e.message}")
            }
        }
    }

    // --- Fast Local Cache Operations (Offline-First) ---

    fun getCars(filter: CarFilter): List<Car> {
        return dbHelper.getCars(filter)
    }

    fun getCarById(id: Long): Car? {
        return dbHelper.getCarById(id)
    }

    fun addCar(car: Car): Long {
        val localId = dbHelper.insertCar(car)
        // Push newly created listing to Render backend asynchronously
        scope.launch {
            try {
                val api = ApiClient.getService()
                val response = api.createCar(car.copy(id = localId))
                if (response.isSuccessful && response.body() != null) {
                    val serverCar = response.body()!!
                    dbHelper.insertCar(serverCar)
                    Log.d("CarRepository", "Vehicle synced to Render with ID ${serverCar.id}")
                }
            } catch (e: Exception) {
                Log.w("CarRepository", "Failed to sync created vehicle to Render: ${e.message}")
            }
        }
        return localId
    }

    fun deleteCar(id: Long): Boolean {
        val deleted = dbHelper.deleteCar(id)
        scope.launch {
            try {
                ApiClient.getService().deleteCar(id)
            } catch (e: Exception) {
                Log.w("CarRepository", "Failed to delete vehicle on Render: ${e.message}")
            }
        }
        return deleted
    }

    fun toggleFavorite(id: Long): Boolean {
        val newStatus = dbHelper.toggleFavorite(id)
        scope.launch {
            try {
                ApiClient.getService().toggleFavorite(id)
            } catch (e: Exception) {
                Log.w("CarRepository", "Failed to sync favorite status to Render: ${e.message}")
            }
        }
        return newStatus
    }

    fun getFavorites(): List<Car> {
        return dbHelper.getFavoriteCars()
    }

    fun getUserListings(): List<Car> {
        return dbHelper.getUserListings()
    }

    fun getCarCount(): Int {
        return dbHelper.getCarCount()
    }

    fun getMessagesForCar(carId: Long): List<ChatMessage> {
        return dbHelper.getMessagesForCar(carId)
    }

    fun sendMessage(message: ChatMessage): Long {
        val localId = dbHelper.insertMessage(message)
        scope.launch {
            try {
                ApiClient.getService().sendMessage(message.carId, message)
            } catch (e: Exception) {
                Log.w("CarRepository", "Failed to send chat message to Render: ${e.message}")
            }
        }
        return localId
    }

    fun seedChatIfEmpty(carId: Long, sellerName: String, listPrice: Double) {
        dbHelper.seedInitialMessagesIfEmpty(carId, sellerName, listPrice)
    }

    // --- Render Backend Synchronization ---

    suspend fun refreshCarsFromBackend(): Result<List<Car>> = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.getService()
            val response = api.getCars()
            if (response.isSuccessful && response.body() != null) {
                val serverCars = response.body()!!
                if (serverCars.isNotEmpty()) {
                    dbHelper.syncCars(serverCars)
                }
                Result.success(serverCars)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshMessagesFromBackend(carId: Long): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.getService()
            val response = api.getMessages(carId)
            if (response.isSuccessful && response.body() != null) {
                val serverMessages = response.body()!!
                if (serverMessages.isNotEmpty()) {
                    dbHelper.syncMessages(carId, serverMessages)
                }
                Result.success(serverMessages)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitOfferToBackend(
        carId: Long,
        amount: Double,
        originalPrice: Double,
        buyerName: String = "Alex"
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.getService()
            val payload = mapOf(
                "offerAmount" to amount,
                "originalListPrice" to originalPrice,
                "buyerName" to buyerName
            )
            val response = api.createOffer(carId, payload)
            if (response.isSuccessful && response.body() != null) {
                val offerMsg = response.body()!!
                dbHelper.insertMessage(offerMsg)
                Result.success(offerMsg)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
