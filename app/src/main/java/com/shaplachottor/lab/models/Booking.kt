package com.shaplachottor.lab.models

data class Booking(
    val bookingId: String = "",
    val userId: String = "",
    val phaseId: String = "",
    val completedPhaseId: String? = null,
    val whatsappNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + Booking.EXPIRATION_WINDOW_MILLIS,
    val status: String = Booking.STATUS_PENDING,
    val reviewedAt: Long = 0L,
    val approvedAt: Long = 0L,
    val lastUpdatedAt: Long = createdAt,
    val reviewedByEmail: String = ""
) {
    val isAwaitingTeacherReview: Boolean
        get() = status == STATUS_PENDING || status == STATUS_REVIEWING

    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_REVIEWING = "reviewing"
        const val STATUS_APPROVED = "approved"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_EXPIRED = "expired"
        const val STATUS_BOOKED_LEGACY = "booked"
        const val EXPIRATION_WINDOW_MILLIS = 15 * 60 * 1000L
    }
}
