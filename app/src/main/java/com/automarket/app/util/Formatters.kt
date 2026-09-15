package com.automarket.app.util

import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatters {

    fun formatPrice(price: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        format.maximumFractionDigits = 0
        return format.format(price)
    }

    fun formatMileage(mileage: Int): String {
        val format = NumberFormat.getNumberInstance(Locale.US)
        return "${format.format(mileage)} mi"
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            hours < 1 -> "Listed just now"
            hours == 1L -> "Listed 1 hour ago"
            hours < 24 -> "Listed $hours hours ago"
            days == 1L -> "Listed 1 day ago"
            days < 30 -> "Listed $days days ago"
            else -> "Listed recently"
        }
    }
}
