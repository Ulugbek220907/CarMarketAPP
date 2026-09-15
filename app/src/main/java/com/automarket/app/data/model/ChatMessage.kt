package com.automarket.app.data.model

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val id: Long = 0L,
    val carId: Long,
    val senderName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromUser: Boolean = false,
    val isSystemNotification: Boolean = false,
    val isOfficialOffer: Boolean = false,
    val offerAmount: Double = 0.0,
    val originalListPrice: Double = 0.0
) : Serializable {

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("h:mm a", Locale.US)
            return sdf.format(Date(timestamp))
        }
}
