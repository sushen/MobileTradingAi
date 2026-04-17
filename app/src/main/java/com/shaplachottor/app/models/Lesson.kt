package com.shaplachottor.app.models

data class Lesson(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val type: String = "text" // "text", "video", "quiz"
)
