package com.automarket.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.view.View
import android.widget.ImageView
import coil.load
import coil.request.CachePolicy
import com.automarket.app.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {

    private fun getPhotosDir(context: Context): File {
        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Calculates optimal inSampleSize so that decoded image fits within reqWidth and reqHeight.
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
     * Saves a picked image URI into internal storage as compressed JPEG
     * for fast, efficient Firebase Storage uploading.
     */
    fun saveImageUriToInternalStorage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1200,
        maxHeight: Int = 900,
        quality: Int = 82
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
            val file = File(photosDir, "vehicle_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")

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
     * Modern, high-performance image loader powered by Coil with disk/memory caching,
     * crossfade transitions, and placeholder handling.
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

        // If local file path
        val model: Any = if (pathOrUri.startsWith("http://") || pathOrUri.startsWith("https://") || pathOrUri.startsWith("content://")) {
            pathOrUri
        } else {
            val file = File(pathOrUri)
            if (file.exists()) file else pathOrUri
        }

        imageView.load(model) {
            crossfade(true)
            crossfade(250)
            diskCachePolicy(CachePolicy.ENABLED)
            memoryCachePolicy(CachePolicy.ENABLED)
            listener(
                onStart = {
                    // Keep existing or show placeholder
                },
                onSuccess = { _, _ ->
                    imageView.visibility = View.VISIBLE
                    placeholderView?.visibility = View.GONE
                },
                onError = { _, _ ->
                    imageView.visibility = View.GONE
                    placeholderView?.visibility = View.VISIBLE
                }
            )
        }
    }
}
