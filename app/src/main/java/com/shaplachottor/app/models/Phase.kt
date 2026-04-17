package com.shaplachottor.app.models

data class Phase(
    val phaseId: String = "",
    val title: String = "",
    val description: String = "",
    val focus: String = "",
    val outcome: String = "",
    val identityShift: String = "",
    val level: String = "Beginner", // Beginner, Intermediate, Advanced
    val order: Int = 0,
    val totalSeats: Int = 100,
    val bookedSeats: Int = 0,
    val isLocked: Boolean = true
) {
    val availableSeats: Int
        get() = totalSeats - bookedSeats
    
    val isAvailable: Boolean
        get() = availableSeats > 0
}
