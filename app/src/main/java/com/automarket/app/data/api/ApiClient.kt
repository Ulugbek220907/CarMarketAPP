package com.automarket.app.data.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ApiClient {

    private const val PREF_NAME = "drivemarket_api_prefs"
    private const val KEY_BASE_URL = "custom_base_url"

    // Live Render production endpoint
    const val DEFAULT_RENDER_URL = "https://carmarket-api-q66k.onrender.com/"
    // Local development emulator / test URL
    const val LOCAL_DEV_URL = "http://10.0.2.2:10000/"

    @Volatile
    private var currentBaseUrl: String = DEFAULT_RENDER_URL

    @Volatile
    private var apiService: CarApiService? = null

    fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val formatted = ensureTrailingSlash(url)
        return try {
            val httpUrl = formatted.toHttpUrlOrNull()
            httpUrl != null && (httpUrl.scheme == "http" || httpUrl.scheme == "https") && httpUrl.host.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_BASE_URL, null)
        if (!saved.isNullOrBlank()) {
            val formatted = ensureTrailingSlash(saved)
            if (isValidUrl(formatted)) {
                currentBaseUrl = formatted
            } else {
                currentBaseUrl = DEFAULT_RENDER_URL
            }
        }
    }

    fun getBaseUrl(): String = currentBaseUrl

    fun setBaseUrl(context: Context, newUrl: String): Boolean {
        val formatted = ensureTrailingSlash(newUrl)
        if (!isValidUrl(formatted)) {
            return false
        }
        currentBaseUrl = formatted
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, formatted).apply()
        apiService = null // Invalidate cached instance
        return true
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
