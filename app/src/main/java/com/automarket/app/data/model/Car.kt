package com.automarket.app.data.model

import java.io.Serializable
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

data class Car(
    val id: Long = 0L,
    val make: String,
    val model: String,
    val year: Int = 2023,
    val price: Double,
    val mileage: Int,
    val transmission: String = "Automatic",
    val bodyStyle: String = "Sedan",
    val location: String = "",
    val description: String = "",
    val sellerName: String = "Seller",
    val sellerPhone: String = "",
    val photo1: String? = null,
    val photo2: String? = null,
    val photo3: String? = null,
    val isFavorite: Boolean = false,
    val isUserListing: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {

    val displayTitle: String
        get() = if (year > 0) "$year $make $model".trim() else "$make $model".trim()

    val specsSummary: String
        get() = "$formattedMileage • $transmission • $bodyStyle"

    val formattedPrice: String
        get() {
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            format.maximumFractionDigits = 0
            return format.format(price)
        }

    val formattedMileage: String
        get() {
            val format = NumberFormat.getNumberInstance(Locale.US)
            return "${format.format(mileage)} mi"
        }

    fun getPhotos(): List<String> {
        val list = mutableListOf<String>()
        photo1?.takeIf { it.isNotBlank() }?.let { list.add(it) }
        photo2?.takeIf { it.isNotBlank() }?.let { list.add(it) }
        photo3?.takeIf { it.isNotBlank() }?.let { list.add(it) }
        return list
    }

    val photoCount: Int
        get() = getPhotos().size
}
