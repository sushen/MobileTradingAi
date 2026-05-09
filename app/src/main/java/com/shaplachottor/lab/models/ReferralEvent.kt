package com.shaplachottor.lab.models

data class ReferralEvent(
    val referrerId: String = "",
    val referredUserId: String = "",
    val status: String = "joined", // joined, converted
    val timestamp: Long = 0,
    val referredUserName: String = "Researcher" // Optional: for display
)
