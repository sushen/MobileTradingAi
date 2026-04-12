package com.tradingai.app.models

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "user", // roles: user, investor, affiliate, student
    val createdAt: Long = System.currentTimeMillis()
)
