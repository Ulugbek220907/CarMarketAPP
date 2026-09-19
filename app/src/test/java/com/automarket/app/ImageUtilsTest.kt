package com.automarket.app

import android.graphics.BitmapFactory
import com.automarket.app.util.ImageUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageUtilsTest {

    @Test
    fun testCalculateInSampleSize_smallerDimensions() {
        val options = BitmapFactory.Options().apply {
            outWidth = 400
            outHeight = 300
        }
        val sampleSize = ImageUtils.calculateInSampleSize(options, 800, 600)
        assertEquals(1, sampleSize)
    }

    @Test
    fun testCalculateInSampleSize_largerWidthOnly() {
        val options = BitmapFactory.Options().apply {
            outWidth = 3200
            outHeight = 500
        }
        val sampleSize = ImageUtils.calculateInSampleSize(options, 800, 600)
        // With || condition, sampleSize increases until neither halfWidth nor halfHeight exceed req bounds
        assertEquals(4, sampleSize)
    }

    @Test
    fun testCalculateInSampleSize_largerHeightOnly() {
        val options = BitmapFactory.Options().apply {
            outWidth = 500
            outHeight = 2400
        }
        val sampleSize = ImageUtils.calculateInSampleSize(options, 800, 600)
        assertEquals(4, sampleSize)
    }

    @Test
    fun testCalculateInSampleSize_bothLarger() {
        val options = BitmapFactory.Options().apply {
            outWidth = 3200
            outHeight = 2400
        }
        val sampleSize = ImageUtils.calculateInSampleSize(options, 800, 600)
        assertEquals(4, sampleSize)
    }

    @Test
    fun testCalculateInSampleSize_invalidReqBounds() {
        val options = BitmapFactory.Options().apply {
            outWidth = 1000
            outHeight = 1000
        }
        assertEquals(1, ImageUtils.calculateInSampleSize(options, 0, 0))
        assertEquals(1, ImageUtils.calculateInSampleSize(options, -100, 500))
    }
}
