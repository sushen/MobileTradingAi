package com.shaplachottor.lab.models

enum class ContentBlockType {
    CONCEPT, EXAMPLE, EXERCISE, REFLECTION
}

data class LessonContentBlock(
    val type: ContentBlockType = ContentBlockType.CONCEPT,
    val title: String = "",
    val body: String = ""
)
