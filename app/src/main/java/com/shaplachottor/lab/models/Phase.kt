package com.shaplachottor.lab.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

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
    @get:PropertyName("visible")
    @set:PropertyName("visible")
    var isVisible: Boolean = true,
    val order: Int = 0,
    val totalSeats: Int = 100,
    val bookedSeats: Int = 0,
    @get:PropertyName("locked")
    @set:PropertyName("locked")
    var isLocked: Boolean = true
) {
    companion object {
        const val TYPE_FREE = "free"
        const val TYPE_PREMIUM = "premium"
    }

    @get:Exclude
    val availableSeats: Int
        get() = totalSeats - bookedSeats
    
    @get:Exclude
    val isAvailable: Boolean
        get() = availableSeats > 0
}
