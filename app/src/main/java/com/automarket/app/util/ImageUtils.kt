package com.automarket.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import android.view.View
import android.widget.ImageView
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

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024)

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun getBitmapFromCache(key: String): Bitmap? = memoryCache.get(key)
    fun putBitmapToCache(key: String, bitmap: Bitmap) { memoryCache.put(key, bitmap) }
    fun clearCache() { memoryCache.evictAll() }

    private fun getPhotosDir(context: Context): File {
        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Calculates optimal inSampleSize so that decoded image fits within reqWidth and reqHeight.
     * Uses || so neither dimension exceeds memory limits.
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        if (reqWidth <= 0 || reqHeight <= 0) return 1
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight || halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
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

        return Bitmap.createScaledBitmap(bitmap, finalWidth.coerceAtLeast(1), finalHeight.coerceAtLeast(1), true)
    }

    /**
     * Saves a picked image URI into internal storage (`context.filesDir/photos/`) as compressed JPEG
     * and returns the absolute local file path.
     */
    fun saveImageUriToInternalStorage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 600,
        quality: Int = 75
    ): String? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val inSampleSize = calculateInSampleSize(boundsOptions, maxWidth, maxHeight)
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inJustDecodeBounds = false
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val scaledBitmap = scaleBitmap(bitmap, maxWidth, maxHeight)
            val photosDir = getPhotosDir(context)
            val file = File(photosDir, "photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")

            FileOutputStream(file).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves incoming Base64 photo string into internal storage (`context.filesDir/photos/`) as JPEG
     * and returns the absolute local file path to prevent SQLite CursorWindow 2MB overflow.
     */
    fun saveBase64ToInternalStorage(
        context: Context,
        base64String: String,
        maxWidth: Int = 800,
        maxHeight: Int = 600,
        quality: Int = 75
    ): String? {
        return try {
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, boundsOptions)

            val inSampleSize = calculateInSampleSize(boundsOptions, maxWidth, maxHeight)
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inJustDecodeBounds = false
            }

            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, decodeOptions)
                ?: return null

            val scaledBitmap = scaleBitmap(bitmap, maxWidth, maxHeight)
            val photosDir = getPhotosDir(context)
            val file = File(photosDir, "photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")

            FileOutputStream(file).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a local file into a compressed Base64 data string ("data:image/jpeg;base64,...") for cloud sync.
     */
    fun fileToBase64(filePath: String, quality: Int = 75): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            val inSampleSize = calculateInSampleSize(boundsOptions, 800, 600)
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inJustDecodeBounds = false
            }

            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
            val scaledBitmap = scaleBitmap(bitmap, 800, 600)

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

    /**
     * Converts a content Uri into a compressed Base64 data string (JPEG max 800x600).
     */
    fun uriToBase64(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 600,
        quality: Int = 75
    ): String? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val inSampleSize = calculateInSampleSize(boundsOptions, maxWidth, maxHeight)
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inJustDecodeBounds = false
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

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

    fun toUploadableBase64(context: Context, pathOrData: String?): String? {
        if (pathOrData.isNullOrBlank()) return null
        if (pathOrData.startsWith("data:") || pathOrData.startsWith("http://") || pathOrData.startsWith("https://")) {
            return pathOrData
        }
        if (pathOrData.startsWith("content://")) {
            return uriToBase64(context, Uri.parse(pathOrData))
        }
        return fileToBase64(pathOrData)
    }

    /**
     * Universal async image loader with LruCache & Coroutine Dispatchers.IO decoding.
     */
    fun loadImage(
        imageView: ImageView,
        placeholderView: View? = null,
        pathOrUri: String?,
        targetWidth: Int = 800,
        targetHeight: Int = 450
    ) {
        if (pathOrUri.isNullOrBlank()) {
            imageView.setImageDrawable(null)
            imageView.visibility = View.GONE
            placeholderView?.visibility = View.VISIBLE
            return
        }

        imageView.clearColorFilter()
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        // Check LruCache hit
        val cached = memoryCache.get(pathOrUri)
        if (cached != null) {
            imageView.tag = pathOrUri
            imageView.setImageBitmap(cached)
            imageView.visibility = View.VISIBLE
            placeholderView?.visibility = View.GONE
            return
        }

        imageView.tag = pathOrUri
        imageView.setImageDrawable(null)
        if (placeholderView != null) {
            imageView.visibility = View.GONE
            placeholderView.visibility = View.VISIBLE
        }

        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = decodeBitmapFromSource(imageView.context, pathOrUri, targetWidth, targetHeight)
            if (bitmap != null) {
                memoryCache.put(pathOrUri, bitmap)
            }
            withContext(Dispatchers.Main) {
                if (imageView.tag == pathOrUri) {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        imageView.visibility = View.VISIBLE
                        placeholderView?.visibility = View.GONE
                    } else {
                        imageView.setImageDrawable(null)
                        imageView.visibility = View.GONE
                        placeholderView?.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun decodeBitmapFromSource(
        context: Context,
        pathOrUri: String,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        try {
            // 1. Base64
            if (pathOrUri.startsWith("data:image/") || pathOrUri.startsWith("data:")) {
                val cleanBase64 = pathOrUri.substringAfter(",")
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, boundsOptions)

                val inSampleSize = calculateInSampleSize(boundsOptions, targetWidth, targetHeight)
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    this.inJustDecodeBounds = false
                }
                val raw = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, decodeOptions)
                return raw?.let {
                    val scaled = scaleBitmap(it, targetWidth, targetHeight)
                    if (scaled != it) it.recycle()
                    scaled
                }
            }

            // 2. Remote HTTP/HTTPS
            if (pathOrUri.startsWith("http://") || pathOrUri.startsWith("https://")) {
                val url = URL(pathOrUri)
                val conn = url.openConnection() as HttpURLConnection
                conn.doInput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.connect()
                conn.inputStream.use { input ->
                    val bytes = input.readBytes()
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

                    val inSampleSize = calculateInSampleSize(boundsOptions, targetWidth, targetHeight)
                    val decodeOptions = BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                        this.inJustDecodeBounds = false
                    }
                    val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                    return raw?.let {
                        val scaled = scaleBitmap(it, targetWidth, targetHeight)
                        if (scaled != it) it.recycle()
                        scaled
                    }
                }
            }

            // 3. Local File
            val file = File(pathOrUri)
            if (file.exists()) {
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

                val inSampleSize = calculateInSampleSize(boundsOptions, targetWidth, targetHeight)
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    this.inJustDecodeBounds = false
                }
                val raw = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                return raw?.let {
                    val scaled = scaleBitmap(it, targetWidth, targetHeight)
                    if (scaled != it) it.recycle()
                    scaled
                }
            }

            // 4. Content URI
            val uri = Uri.parse(pathOrUri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

                val inSampleSize = calculateInSampleSize(boundsOptions, targetWidth, targetHeight)
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    this.inJustDecodeBounds = false
                }
                val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                return raw?.let {
                    val scaled = scaleBitmap(it, targetWidth, targetHeight)
                    if (scaled != it) it.recycle()
                    scaled
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
