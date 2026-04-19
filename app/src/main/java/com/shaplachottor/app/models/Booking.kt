package com.shaplachottor.app.models

data class Booking(
    val bookingId: String = "",
    val userId: String = "",
    val phaseId: String = "",
    val phoneNumber: String = "",
    val whatsappNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + EXPIRATION_WINDOW_MILLIS,
    val status: String = STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_EXPIRED = "expired"
        const val STATUS_BOOKED_LEGACY = "booked"
        const val EXPIRATION_WINDOW_MILLIS = 15 * 60 * 1000L
    }
}
