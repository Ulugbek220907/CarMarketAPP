package com.automarket.app.data.model

import java.io.Serializable
import java.text.NumberFormat
import java.util.Locale

data class Car(
    val id: String = "",
    val make: String = "",
    val model: String = "",
    val year: Int = 2024,
    val price: Double = 0.0,
    val mileage: Int = 0,
    val transmission: String = "Automatic",
    val bodyStyle: String = "Sedan",
    val location: String = "",
    val description: String = "",
    val sellerName: String = "Seller",
    val sellerPhone: String = "",
    val sellerUid: String = "",
    val photoUrls: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val isUserListing: Boolean = false,
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

    val monthlyEstimate: String
        get() {
            if (price <= 0) return "$0/mo est."
            // 72 months, ~5.4% APR estimate
            val monthly = (price * 1.15) / 72.0
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            format.maximumFractionDigits = 0
            return "${format.format(monthly)}/mo est."
        }

    fun getPhotos(): List<String> = photoUrls

    val photoCount: Int
        get() = photoUrls.size

    val primaryPhotoUrl: String?
        get() = photoUrls.firstOrNull()

    // Backwards compatibility properties
    val photo1: String? get() = photoUrls.getOrNull(0)
    val photo2: String? get() = photoUrls.getOrNull(1)
    val photo3: String? get() = photoUrls.getOrNull(2)
}
