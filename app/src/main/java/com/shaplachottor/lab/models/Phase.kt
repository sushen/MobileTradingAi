package com.shaplachottor.lab.models

data class Phase(
    val phaseId: String = "",
    val title: String = "",
    val description: String = "",
    val focus: String = "",
    val outcome: String = "",
    val identityShift: String = "",
    val level: String = "Beginner", // Beginner, Intermediate, Advanced
    val type: String = TYPE_FREE, // free, premium
    val price: Double = 0.0,
    val currency: String = "USD",
    val startDate: Long = 0L,
    val isVisible: Boolean = true,
    val order: Int = 0,
    val totalSeats: Int = 100,
    val bookedSeats: Int = 0,
    val isLocked: Boolean = true
) {
    companion object {
        const val TYPE_FREE = "free"
        const val TYPE_PREMIUM = "premium"
    }

    val availableSeats: Int
        get() = totalSeats - bookedSeats
    
    val isAvailable: Boolean
        get() = availableSeats > 0
}
