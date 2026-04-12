package com.tradingai.app.models

data class Course(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val level: String = "Beginner", // Beginner, Intermediate, Advanced
    val videoUrl: String = "",
    val docLink: String = ""
)
