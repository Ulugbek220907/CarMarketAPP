package com.automarket.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.automarket.app.R
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageUtils {

    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val photosDir = File(context.filesDir, "car_photos")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }

            val fileName = "car_photo_${UUID.randomUUID()}.jpg"
            val destFile = File(photosDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

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

        // Check if it is a sample placeholder identifier
        if (pathOrUri.startsWith("sample_")) {
            imageView.visibility = View.VISIBLE
            placeholderView?.visibility = View.GONE
            imageView.setImageResource(R.drawable.ic_car_silhouette)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.setColorFilter(imageView.context.getColor(R.color.gray_400))
            return
        }

        imageView.clearColorFilter()
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        try {
            val file = File(pathOrUri)
            if (file.exists()) {
                val bitmap = decodeSampledBitmapFromFile(file.absolutePath, targetWidth, targetHeight)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                    placeholderView?.visibility = View.GONE
                    return
                }
            }

            // Try URI decoding
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

        // Fallback
        imageView.visibility = View.GONE
        placeholderView?.visibility = View.VISIBLE
    }

    private fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
