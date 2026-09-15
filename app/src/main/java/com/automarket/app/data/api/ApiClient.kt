package com.automarket.app.data.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val PREF_NAME = "drivemarket_api_prefs"
    private const val KEY_BASE_URL = "custom_base_url"

    // Default Render endpoint - update with your actual Render service URL
    const val DEFAULT_RENDER_URL = "https://carmarketapp.onrender.com/"
    // Local development emulator / test URL
    const val LOCAL_DEV_URL = "http://10.0.2.2:10000/"

    @Volatile
    private var currentBaseUrl: String = DEFAULT_RENDER_URL

    @Volatile
    private var apiService: CarApiService? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_BASE_URL, null)
        if (!saved.isNullOrBlank()) {
            currentBaseUrl = ensureTrailingSlash(saved)
        }
    }

    fun getBaseUrl(): String = currentBaseUrl

    fun setBaseUrl(context: Context, newUrl: String) {
        val formatted = ensureTrailingSlash(newUrl)
        currentBaseUrl = formatted
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, formatted).apply()
        apiService = null // Invalidate cached instance
    }

    fun getService(): CarApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildService(currentBaseUrl).also { apiService = it }
        }
    }

    private fun buildService(url: String): CarApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(35, TimeUnit.SECONDS) // Accommodate Render free-tier cold starts
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(CarApiService::class.java)
    }

    private fun ensureTrailingSlash(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
