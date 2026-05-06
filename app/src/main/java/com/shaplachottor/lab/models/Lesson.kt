package com.shaplachottor.lab.models

data class Lesson(
    val id: String = "",
    val phaseId: String = "",
    val title: String = "",
    val contentBlocks: List<LessonContentBlock> = emptyList(),
    val isCompleted: Boolean = false,
    val order: Int = 0
)
