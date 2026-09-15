package com.automarket.app.data.model

import java.io.Serializable
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

data class Car(
    val id: Long = 0L,
    val make: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int,
    val transmission: String, // "Automatic", "Manual", "Single-Speed"
    val fuelType: String,     // "Gasoline", "Hybrid", "Electric", "Diesel"
    val bodyStyle: String,    // "Sedan", "SUV", "Coupe", "Truck", "Hatchback"
    val location: String,
    val description: String,
    val sellerName: String,
    val sellerPhone: String,
    val photo1: String? = null,
    val photo2: String? = null,
    val photo3: String? = null,
    val isFavorite: Boolean = false,
    val isUserListing: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    
    // DriveMarket Attributes
    val trim: String = "",
    val dealRating: String = "Great Deal",
    val carfaxClean: Boolean = true,
    val distance: String = "5 miles away",
    val sellerRating: Double = 4.9,
    val sellerReviewCount: Int = 42,
    val sellerResponseTime: String = "Replies < 15 mins",
    val condition: String = "Excellent",
    val drivetrain: String = "AWD",
    val highlights: String = "Autopilot, Premium Audio, Heated Seats, Glass Roof"
) : Serializable {

    val displayTitle: String
        get() = "$year $make $model"

    val displaySubtitle: String
        get() = if (trim.isNotBlank()) trim else "$transmission • $drivetrain"

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

    val formattedMonthlyPayment: String
        get() {
            val monthly = (price / 65.0).roundToInt()
            return "$$monthly/mo est."
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

    fun getHighlightsList(): List<String> {
        return highlights.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
