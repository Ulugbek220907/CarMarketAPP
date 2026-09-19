package com.automarket.app

import com.automarket.app.data.api.ApiClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiClientTest {

    @Test
    fun testValidHttpUrl() {
        assertTrue(ApiClient.isValidUrl("http://10.0.2.2:10000"))
        assertTrue(ApiClient.isValidUrl("http://10.0.2.2:10000/"))
        assertTrue(ApiClient.isValidUrl("https://carmarket-api-q66k.onrender.com"))
        assertTrue(ApiClient.isValidUrl("https://carmarket-api-q66k.onrender.com/"))
        assertTrue(ApiClient.isValidUrl("https://api.example.com/v1/"))
    }

    @Test
    fun testInvalidUrls() {
        assertFalse(ApiClient.isValidUrl(""))
        assertFalse(ApiClient.isValidUrl("   "))
        assertFalse(ApiClient.isValidUrl("ftp://example.com"))
        assertFalse(ApiClient.isValidUrl("http://"))
        assertFalse(ApiClient.isValidUrl("https://"))
        assertFalse(ApiClient.isValidUrl("not-a-valid-url"))
        assertFalse(ApiClient.isValidUrl("://invalid"))
    }
}
