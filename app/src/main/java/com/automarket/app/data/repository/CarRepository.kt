package com.automarket.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.automarket.app.data.model.Car
import com.automarket.app.data.model.CarFilter
import com.automarket.app.data.model.CategoryFilter
import com.automarket.app.data.model.ChatMessage
import com.automarket.app.data.model.SortOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class CarRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val prefs = context.getSharedPreferences("automarket_prefs", Context.MODE_PRIVATE)
    private val favoriteIds = mutableSetOf<String>()

    private val _cars = mutableListOf<Car>()
    private val updateListeners = mutableListOf<() -> Unit>()
    private var carsSnapshotListener: ListenerRegistration? = null
    private var hasAttemptedSeed = false

    init {
        loadFavoritesFromPrefs()
        populateInitialShowcaseCache()
        startRealtimeCarsListener()
        setupAuthStateListener()
    }

    private fun loadFavoritesFromPrefs() {
        val saved = prefs.getStringSet("favorites_set", emptySet()) ?: emptySet()
        synchronized(favoriteIds) {
            favoriteIds.clear()
            favoriteIds.addAll(saved)
        }
    }

    private fun saveFavoritesToPrefs() {
        synchronized(favoriteIds) {
            prefs.edit().putStringSet("favorites_set", HashSet(favoriteIds)).apply()
        }
    }

    private fun setupAuthStateListener() {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid ?: ""
            Log.d("CarRepository", "Auth state changed, user uid: $uid")
            if (uid.isNotEmpty()) {
                val guestUid = prefs.getString("guest_uid", "") ?: ""
                synchronized(_cars) {
                    for (i in _cars.indices) {
                        val car = _cars[i]
                        val isUser = car.sellerUid == uid || (guestUid.isNotEmpty() && car.sellerUid == guestUid) || car.isUserListing
                        _cars[i] = car.copy(isUserListing = isUser)
                    }
                }
                notifyListeners()
            }
        }
    }

    fun getCurrentUserUid(): String {
        val authUid = auth.currentUser?.uid
        if (!authUid.isNullOrEmpty()) return authUid
        var guestUid = prefs.getString("guest_uid", null)
        if (guestUid.isNullOrEmpty()) {
            guestUid = "guest_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("guest_uid", guestUid).apply()
        }
        return guestUid
    }

    fun getCurrentUserName(): String {
        return auth.currentUser?.displayName?.ifBlank { null }
            ?: prefs.getString("seller_name", null)?.ifBlank { null }
            ?: context.getSharedPreferences("seller_profile_prefs", Context.MODE_PRIVATE).getString("seller_name", null)?.ifBlank { null }
            ?: "Verified User"
    }

    fun getCurrentUserPhone(): String {
        return prefs.getString("seller_phone", "")?.ifBlank { null }
            ?: context.getSharedPreferences("seller_profile_prefs", Context.MODE_PRIVATE).getString("phone", "")
            ?: ""
    }

    fun updateUserProfile(displayName: String, phone: String = "") {
        prefs.edit()
            .putString("seller_name", displayName)
            .putString("seller_phone", phone)
            .apply()

        context.getSharedPreferences("seller_profile_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("seller_name", displayName)
            .putString("phone", phone)
            .apply()

        val user = auth.currentUser
        if (user != null && displayName.isNotBlank()) {
            val req = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(req).addOnSuccessListener {
                Log.d("CarRepository", "Firebase Auth displayName updated: $displayName")
            }
        }
    }

    private fun startRealtimeCarsListener() {
        carsSnapshotListener?.remove()
        carsSnapshotListener = firestore.collection("cars")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("CarRepository", "Firestore cars listen failed", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val currentUid = auth.currentUser?.uid ?: ""
                    val favs = synchronized(favoriteIds) { favoriteIds.toSet() }

                    val carList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val car = doc.toObject(Car::class.java) ?: return@mapNotNull null
                            val sellerUid = car.sellerUid.ifEmpty { doc.getString("sellerUid") ?: "" }
                            val isUser = (currentUid.isNotEmpty() && sellerUid == currentUid) || car.isUserListing
                            car.copy(
                                id = doc.id,
                                isFavorite = favs.contains(doc.id),
                                isUserListing = isUser,
                                sellerUid = sellerUid
                            )
                        } catch (e: Exception) {
                            Log.e("CarRepository", "Error deserializing car ${doc.id}", e)
                            null
                        }
                    }

                    synchronized(_cars) {
                        _cars.clear()
                        _cars.addAll(carList)
                    }

                    notifyListeners()

                    // Automatically seed initial showcase inventory if collection is completely empty
                    if (carList.isEmpty() && !hasAttemptedSeed) {
                        hasAttemptedSeed = true
                        scope.launch {
                            seedShowcaseVehicles()
                        }
                    }
                }
            }
    }

    fun addUpdateListener(listener: () -> Unit) {
        synchronized(updateListeners) {
            updateListeners.add(listener)
        }
    }

    fun removeUpdateListener(listener: () -> Unit) {
        synchronized(updateListeners) {
            updateListeners.remove(listener)
        }
    }

    private fun notifyListeners() {
        val listenersCopy = synchronized(updateListeners) { updateListeners.toList() }
        CoroutineScope(Dispatchers.Main).launch {
            for (listener in listenersCopy) {
                listener.invoke()
            }
        }
    }

    // --- Vehicle Retrieval & Filtering ---

    fun getCars(filter: CarFilter): List<Car> {
        val all = synchronized(_cars) { _cars.toList() }
        return all.filter { car ->
            matchesFilter(car, filter)
        }.sortedWith(getComparator(filter.sortOption))
    }

    fun getCarById(id: String): Car? {
        return synchronized(_cars) {
            _cars.find { it.id == id }
        }
    }

    fun getFavorites(): List<Car> {
        return synchronized(_cars) {
            _cars.filter { it.isFavorite }
        }
    }

    fun getUserListings(): List<Car> {
        val currentUid = getCurrentUserUid()
        return synchronized(_cars) {
            _cars.filter {
                (currentUid.isNotEmpty() && it.sellerUid == currentUid) || it.isUserListing
            }
        }
    }

    fun getCarCount(): Int {
        return synchronized(_cars) { _cars.size }
    }

    fun toggleFavorite(carId: String): Boolean {
        val isFav: Boolean
        synchronized(favoriteIds) {
            if (favoriteIds.contains(carId)) {
                favoriteIds.remove(carId)
                isFav = false
            } else {
                favoriteIds.add(carId)
                isFav = true
            }
        }
        saveFavoritesToPrefs()

        synchronized(_cars) {
            val idx = _cars.indexOfFirst { it.id == carId }
            if (idx != -1) {
                _cars[idx] = _cars[idx].copy(isFavorite = isFav)
            }
        }
        notifyListeners()
        return isFav
    }

    // --- Vehicle Creation with Firebase Storage Upload ---

    suspend fun addCarWithUpload(
        car: Car,
        localPhotoPaths: List<String>,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Car> = withContext(Dispatchers.IO) {
        try {
            val currentUid = getCurrentUserUid()
            val docRef = firestore.collection("cars").document()
            val carId = docRef.id

            val uploadedUrls = mutableListOf<String>()
            val totalPhotos = localPhotoPaths.size

            for ((index, path) in localPhotoPaths.withIndex()) {
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    uploadedUrls.add(path)
                } else {
                    try {
                        val file = File(path)
                        val uri = if (file.exists()) Uri.fromFile(file) else Uri.parse(path)
                        val photoRef = storage.reference.child("vehicle_photos/$carId/photo_${System.currentTimeMillis()}_$index.jpg")

                        val uploadTask = photoRef.putFile(uri)
                        uploadTask.addOnProgressListener { taskSnapshot ->
                            val progress = if (taskSnapshot.totalByteCount > 0) {
                                ((index + (taskSnapshot.bytesTransferred.toDouble() / taskSnapshot.totalByteCount)) / totalPhotos * 100).toInt()
                            } else 0
                            onProgress?.invoke(progress.coerceIn(0, 99))
                        }.await()

                        val downloadUrl = photoRef.downloadUrl.await().toString()
                        uploadedUrls.add(downloadUrl)
                    } catch (e: Exception) {
                        Log.w("CarRepository", "Failed to upload photo to Firebase Storage: ${e.message}. Using local path fallback.")
                        uploadedUrls.add(path)
                    }
                }
            }

            onProgress?.invoke(100)

            val newCar = car.copy(
                id = carId,
                sellerUid = currentUid,
                photoUrls = uploadedUrls,
                isUserListing = true,
                createdAt = System.currentTimeMillis()
            )

            docRef.set(newCar).await()

            synchronized(_cars) {
                _cars.add(0, newCar)
            }
            notifyListeners()

            Result.success(newCar)
        } catch (e: Exception) {
            Log.e("CarRepository", "Error creating vehicle in Firestore", e)
            Result.failure(e)
        }
    }

    fun addCar(car: Car, onComplete: ((Boolean) -> Unit)? = null): String {
        val docRef = firestore.collection("cars").document()
        val currentUid = getCurrentUserUid()
        val newCar = car.copy(
            id = docRef.id,
            sellerUid = currentUid,
            isUserListing = true
        )

        scope.launch {
            try {
                docRef.set(newCar).await()
                synchronized(_cars) {
                    _cars.add(0, newCar)
                }
                notifyListeners()
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true)
                }
            } catch (e: Exception) {
                Log.e("CarRepository", "Failed to add car: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false)
                }
            }
        }
        return docRef.id
    }

    fun deleteCar(carId: String, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        synchronized(_cars) {
            _cars.removeAll { it.id == carId }
        }
        notifyListeners()

        scope.launch {
            try {
                firestore.collection("cars").document(carId).delete().await()
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true)
                }
            } catch (e: Exception) {
                Log.w("CarRepository", "Failed to delete car $carId from Firestore: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false)
                }
            }
        }
        return true
    }

    // --- Real-time Chat & Offers Architecture ---

    fun observeMessages(carId: String, onUpdate: (List<ChatMessage>) -> Unit): ListenerRegistration {
        val currentUid = getCurrentUserUid()
        return firestore.collection("chats")
            .document(carId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("CarRepository", "Messages listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        val msg = doc.toObject(ChatMessage::class.java) ?: return@mapNotNull null
                        val senderUid = msg.senderUid.ifEmpty { doc.getString("senderUid") ?: "" }
                        val isFromUser = currentUid.isNotEmpty() && senderUid == currentUid
                        msg.copy(
                            id = doc.id,
                            carId = carId,
                            senderUid = senderUid,
                            isFromUser = isFromUser
                        )
                    }
                    onUpdate(messages)
                }
            }
    }

    suspend fun sendMessage(
        carId: String,
        messageText: String,
        senderName: String
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val currentUid = getCurrentUserUid()
            val chatRef = firestore.collection("chats").document(carId).collection("messages").document()

            val msg = ChatMessage(
                id = chatRef.id,
                carId = carId,
                senderUid = currentUid,
                senderName = senderName,
                messageText = messageText,
                timestamp = System.currentTimeMillis(),
                isFromUser = false,
                isSystemNotification = false,
                isOfficialOffer = false
            )

            chatRef.set(msg).await()
            Result.success(msg.copy(isFromUser = true))
        } catch (e: Exception) {
            Log.e("CarRepository", "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun submitOffer(
        carId: String,
        amount: Double,
        originalPrice: Double,
        buyerName: String
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val currentUid = getCurrentUserUid()
            val chatRef = firestore.collection("chats").document(carId).collection("messages").document()

            val offerMsg = ChatMessage(
                id = chatRef.id,
                carId = carId,
                senderUid = currentUid,
                senderName = buyerName,
                messageText = "Official Purchase Offer: $${String.format("%,.0f", amount)}",
                timestamp = System.currentTimeMillis(),
                isFromUser = false,
                isSystemNotification = false,
                isOfficialOffer = true,
                offerAmount = amount,
                originalListPrice = originalPrice,
                offerStatus = "PENDING"
            )

            chatRef.set(offerMsg).await()
            Result.success(offerMsg.copy(isFromUser = true))
        } catch (e: Exception) {
            Log.e("CarRepository", "Error submitting offer: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateOfferStatus(
        carId: String,
        messageId: String,
        status: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("chats")
                .document(carId)
                .collection("messages")
                .document(messageId)
                .update("offerStatus", status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Helper Filter & Sort Functions ---

    private fun matchesFilter(car: Car, filter: CarFilter): Boolean {
        // Location Filter
        if (filter.location.isNotBlank() && filter.location != "All Locations") {
            if (!car.location.contains(filter.location, ignoreCase = true)) {
                return false
            }
        }

        // Search Query
        if (filter.searchQuery.isNotBlank()) {
            val q = filter.searchQuery.trim().lowercase()
            val matches = car.make.lowercase().contains(q) ||
                    car.model.lowercase().contains(q) ||
                    car.displayTitle.lowercase().contains(q) ||
                    car.bodyStyle.lowercase().contains(q) ||
                    car.location.lowercase().contains(q) ||
                    car.year.toString().contains(q) ||
                    car.description.lowercase().contains(q)
            if (!matches) return false
        }

        // Category Filter
        return when (filter.category) {
            CategoryFilter.ALL -> true
            CategoryFilter.SUV -> car.bodyStyle.equals("SUV", ignoreCase = true) || car.bodyStyle.contains("Crossover", ignoreCase = true)
            CategoryFilter.SEDAN -> car.bodyStyle.equals("Sedan", ignoreCase = true)
            CategoryFilter.ELECTRIC -> car.description.contains("Electric", ignoreCase = true) || car.model.contains("Model 3", ignoreCase = true) || car.model.contains("Lightning", ignoreCase = true) || car.transmission.contains("Direct", ignoreCase = true)
            CategoryFilter.TRUCK -> car.bodyStyle.equals("Truck", ignoreCase = true) || car.model.contains("F-150", ignoreCase = true)
            CategoryFilter.LUXURY -> car.price >= 50000 || car.make.equals("Porsche", ignoreCase = true) || car.make.equals("BMW", ignoreCase = true) || car.make.equals("Mercedes-Benz", ignoreCase = true)
            CategoryFilter.HYBRID -> car.description.contains("Hybrid", ignoreCase = true) || car.bodyStyle.contains("Hybrid", ignoreCase = true)
            CategoryFilter.COUPE -> car.bodyStyle.equals("Coupe", ignoreCase = true)
            CategoryFilter.UNDER_15K -> car.price < 15000
            CategoryFilter.UNDER_25K -> car.price < 25000
            CategoryFilter.UNDER_30K -> car.price < 30000
            CategoryFilter.LOW_MILES -> car.mileage < 30000
            CategoryFilter.CERTIFIED -> true
        }
    }

    private fun getComparator(sortOption: SortOption): Comparator<Car> {
        return when (sortOption) {
            SortOption.RECOMMENDED -> compareByDescending { it.createdAt }
            SortOption.NEWEST -> compareByDescending { it.year }
            SortOption.PRICE_ASC -> compareBy { it.price }
            SortOption.PRICE_DESC -> compareByDescending { it.price }
            SortOption.MILEAGE_ASC -> compareBy { it.mileage }
        }
    }

    // --- Seed Initial Vehicles for pristine immediate look ---

    fun getInitialShowcaseCars(): List<Car> {
        val favs = synchronized(favoriteIds) { favoriteIds.toSet() }
        val currentUid = auth.currentUser?.uid ?: ""
        return listOf(
            Car(
                id = "showcase_tesla_model3",
                make = "Tesla",
                model = "Model 3",
                year = 2022,
                price = 31450.0,
                mileage = 28450,
                transmission = "Automatic",
                bodyStyle = "Sedan",
                location = "Austin, TX",
                description = "Pristine condition Long Range Dual Motor with Full Self-Driving capability, premium white interior, glass panoramic roof, and CARFAX 1-owner clean title.",
                sellerName = "Marcus Chen",
                sellerPhone = "+1 (512) 555-0194",
                sellerUid = "seller_marcus",
                photoUrls = listOf(
                    "https://images.unsplash.com/photo-1560958089-b8a1929cea89?w=1080&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1536700503339-1e4b06520771?w=1080&auto=format&fit=crop"
                ),
                isFavorite = favs.contains("showcase_tesla_model3"),
                isUserListing = (currentUid.isNotEmpty() && currentUid == "seller_marcus"),
                createdAt = System.currentTimeMillis() - 3600000
            ),
            Car(
                id = "showcase_porsche_911",
                make = "Porsche",
                model = "911 Carrera S",
                year = 2023,
                price = 128900.0,
                mileage = 8200,
                transmission = "PDK 8-Speed",
                bodyStyle = "Coupe",
                location = "Miami, FL",
                description = "Stunning Chalk exterior with Bordeaux Red leather interior, Sport Chrono package, 20/21 Carrera Classic wheels, and sports exhaust system. Factory warranty active.",
                sellerName = "Elena Vance",
                sellerPhone = "+1 (305) 555-0182",
                sellerUid = "seller_elena",
                photoUrls = listOf(
                    "https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?w=1080&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=1080&auto=format&fit=crop"
                ),
                isFavorite = favs.contains("showcase_porsche_911"),
                isUserListing = (currentUid.isNotEmpty() && currentUid == "seller_elena"),
                createdAt = System.currentTimeMillis() - 7200000
            ),
            Car(
                id = "showcase_toyota_rav4",
                make = "Toyota",
                model = "RAV4 XLE Premium",
                year = 2021,
                price = 26800.0,
                mileage = 34200,
                transmission = "8-Speed Automatic",
                bodyStyle = "SUV",
                location = "Seattle, WA",
                description = "Clean title 1-owner AWD with Moonroof, SofTex leather seats, Apple CarPlay / Android Auto, and Toyota Safety Sense 2.0. Perfect family road-tripper.",
                sellerName = "David Ross",
                sellerPhone = "+1 (206) 555-0139",
                sellerUid = "seller_david",
                photoUrls = listOf(
                    "https://images.unsplash.com/photo-1581540222194-0def2dda95b8?w=1080&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1621007947382-bb3c3994e3fb?w=1080&auto=format&fit=crop"
                ),
                isFavorite = favs.contains("showcase_toyota_rav4"),
                isUserListing = (currentUid.isNotEmpty() && currentUid == "seller_david"),
                createdAt = System.currentTimeMillis() - 10800000
            ),
            Car(
                id = "showcase_ford_f150",
                make = "Ford",
                model = "F-150 Lightning",
                year = 2023,
                price = 54500.0,
                mileage = 16100,
                transmission = "Direct Drive",
                bodyStyle = "Truck",
                location = "Dallas, TX",
                description = "Extended-range battery with 320 miles range, 9.6kW Pro Power Onboard generator, panoramic vista roof, Co-Pilot360 active drive assist, and spray-in bedliner.",
                sellerName = "Travis McCoy",
                sellerPhone = "+1 (214) 555-0177",
                sellerUid = "seller_travis",
                photoUrls = listOf(
                    "https://images.unsplash.com/photo-1583121274602-3e2820c69888?w=1080&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1605893477799-b99e3b8b93fe?w=1080&auto=format&fit=crop"
                ),
                isFavorite = favs.contains("showcase_ford_f150"),
                isUserListing = (currentUid.isNotEmpty() && currentUid == "seller_travis"),
                createdAt = System.currentTimeMillis() - 14400000
            ),
            Car(
                id = "showcase_bmw_m4",
                make = "BMW",
                model = "M4 Competition",
                year = 2022,
                price = 74200.0,
                mileage = 19500,
                transmission = "8-Speed M Steptronic",
                bodyStyle = "Coupe",
                location = "Los Angeles, CA",
                description = "Isle of Man Green Metallic over Kyalami Orange full merino leather. Carbon fiber exterior package, M Carbon bucket seats, executive package, and Harman Kardon audio.",
                sellerName = "Samantha Wu",
                sellerPhone = "+1 (310) 555-0165",
                sellerUid = "seller_samantha",
                photoUrls = listOf(
                    "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=1080&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1617814076367-b759c7d7e738?w=1080&auto=format&fit=crop"
                ),
                isFavorite = favs.contains("showcase_bmw_m4"),
                isUserListing = (currentUid.isNotEmpty() && currentUid == "seller_samantha"),
                createdAt = System.currentTimeMillis() - 18000000
            )
        )
    }

    private fun populateInitialShowcaseCache() {
        val seeds = getInitialShowcaseCars()
        synchronized(_cars) {
            if (_cars.isEmpty()) {
                _cars.addAll(seeds)
            }
        }
    }

    private suspend fun seedShowcaseVehicles() = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("cars").limit(1).get().await()
            if (!snapshot.isEmpty) return@withContext

            val seeds = getInitialShowcaseCars()
            for (seedCar in seeds) {
                val doc = firestore.collection("cars").document()
                doc.set(seedCar.copy(id = doc.id)).await()
            }
            Log.d("CarRepository", "Seeded ${seeds.size} showcase vehicles to Cloud Firestore")
        } catch (e: Exception) {
            Log.w("CarRepository", "Showcase seeding deferred: ${e.message}")
        }
    }
}
