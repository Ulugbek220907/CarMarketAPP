package com.automarket.app

import android.app.Application
import android.util.Log
import com.automarket.app.data.repository.CarRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.persistentCacheSettings

class AutoMarketApplication : Application() {

    lateinit var carRepository: CarRepository
        private set

    val repository: CarRepository
        get() = carRepository

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Google Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d("AutoMarketApp", "Firebase successfully initialized")
        } catch (e: Exception) {
            Log.w("AutoMarketApp", "Firebase initialization warning: ${e.message}")
        }

        // 2. Configure native Firestore offline disk persistence
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(persistentCacheSettings {})
                .build()
            firestore.firestoreSettings = settings
            Log.d("AutoMarketApp", "Firestore offline persistence enabled")
        } catch (e: Exception) {
            Log.d("AutoMarketApp", "Firestore settings configuration: ${e.message}")
        }

        // 3. Anonymous Authentication for seamless guest listings and chats
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnSuccessListener { result ->
                        Log.d("AutoMarketApp", "Firebase anonymous user signed in: ${result.user?.uid}")
                        val savedName = getSharedPreferences("seller_profile_prefs", MODE_PRIVATE).getString("seller_name", null)
                        if (!savedName.isNullOrBlank() && result.user?.displayName.isNullOrBlank()) {
                            val req = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(savedName)
                                .build()
                            result.user?.updateProfile(req)
                        }
                    }
                    .addOnFailureListener {
                        Log.w("AutoMarketApp", "Firebase Auth offline/deferred: ${it.message}")
                    }
            }
        } catch (e: Exception) {
            Log.w("AutoMarketApp", "Firebase Auth warning: ${e.message}")
        }

        // 4. Initialize Repository
        carRepository = CarRepository(this)
    }
}
