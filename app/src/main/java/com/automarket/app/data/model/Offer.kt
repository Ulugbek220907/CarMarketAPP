package com.automarket.app.data.model

import java.io.Serializable
import java.text.NumberFormat
import java.util.Locale

data class Offer(
    val id: Long = 0L,
    val carId: Long,
    val buyerName: String,
    val offeredPrice: Double,
    val originalPrice: Double,
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED, COUNTERED
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {

    val formattedOfferedPrice: String
        get() {
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            format.maximumFractionDigits = 0
            return format.format(offeredPrice)
        }

    val formattedOriginalPrice: String
        get() {
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            format.maximumFractionDigits = 0
            return format.format(originalPrice)
        }

    val differenceText: String
        get() {
            val diff = originalPrice - offeredPrice
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            format.maximumFractionDigits = 0
            return "-${format.format(diff)} below asking"
        }
}
