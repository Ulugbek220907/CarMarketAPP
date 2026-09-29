package com.automarket.app.data.model

import java.io.Serializable
import java.text.NumberFormat
import java.util.Locale

data class Offer(
    val id: String = "",
    val carId: String = "",
    val buyerUid: String = "",
    val buyerName: String = "Buyer",
    val offeredPrice: Double = 0.0,
    val originalPrice: Double = 0.0,
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
            return if (diff >= 0) {
                "-${format.format(diff)} below asking"
            } else {
                "+${format.format(-diff)} above asking"
            }
        }
}
