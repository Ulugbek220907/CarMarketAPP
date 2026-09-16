package com.automarket.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.View
import android.widget.ImageView
import com.automarket.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object ImageUtils {

    /**
     * Converts a content Uri into a compressed Base64 data string (JPEG max 800x600).
     * This allows photos to be stored directly in Render database and rendered on ANY phone.
     */
    fun uriToBase64(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 600,
        quality: Int = 75
    ): String? {
        return try {
            // First decode bounds to avoid OutOfMemory
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            var inSampleSize = 1
            if (boundsOptions.outHeight > maxHeight || boundsOptions.outWidth > maxWidth) {
                val halfHeight = boundsOptions.outHeight / 2
                val halfWidth = boundsOptions.outWidth / 2
                while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                    inSampleSize *= 2
                }
            }

            // Decode actual sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inJustDecodeBounds = false
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // Scale if still slightly oversized
            val scaledBitmap = scaleBitmap(bitmap, maxWidth, maxHeight)

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()

            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight
        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    /**
     * Universal image loader supporting:
     * 1. Base64 data strings ("data:image/jpeg;base64,...") -> Cross-device Render photos
     * 2. Remote HTTP/HTTPS URLs
     * 3. Local File paths
     * 4. Content URIs
     */
    fun loadImage(
        imageView: ImageView,
        placeholderView: View? = null,
        pathOrUri: String?,
        targetWidth: Int = 800,
        targetHeight: Int = 450
    ) {
        if (pathOrUri.isNullOrBlank()) {
            imageView.visibility = View.GONE
            placeholderView?.visibility = View.VISIBLE
            return
        }

        imageView.clearColorFilter()
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        // 1. Check for Base64 image payload (Cloud synced from Render)
        if (pathOrUri.startsWith("data:image/") || pathOrUri.startsWith("data:")) {
            try {
                val base64Data = pathOrUri.substringAfter(",")
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                    placeholderView?.visibility = View.GONE
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Check for Remote HTTP/HTTPS URL
        if (pathOrUri.startsWith("http://") || pathOrUri.startsWith("https://")) {
            imageView.visibility = View.VISIBLE
            placeholderView?.visibility = View.GONE
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL(pathOrUri)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.doInput = true
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.connect()
                    val input: InputStream = conn.inputStream
                    val bitmap = BitmapFactory.decodeStream(input)
                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                            imageView.visibility = View.VISIBLE
                            placeholderView?.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        imageView.visibility = View.GONE
                        placeholderView?.visibility = View.VISIBLE
                    }
                }
            }
            return
        }

        // 3. Check for local File path
        try {
            val file = File(pathOrUri)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                    placeholderView?.visibility = View.GONE
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Check for Content URI
        try {
            val uri = Uri.parse(pathOrUri)
            val input: InputStream? = imageView.context.contentResolver.openInputStream(uri)
            if (input != null) {
                val bitmap = BitmapFactory.decodeStream(input)
                input.close()
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                    placeholderView?.visibility = View.GONE
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback to placeholder
        imageView.visibility = View.GONE
        placeholderView?.visibility = View.VISIBLE
    }
}
