package com.shaplachottor.lab.util

import com.shaplachottor.lab.models.Lesson

object SequentialLessonProgressResolver {

    data class SequentialLessonState(
        val orderedLessons: List<Lesson>,
        val rawCompletedLessonIds: Set<String>,
        val completedLessonIds: Set<String>,
        val unlockedLessonIds: Set<String>,
        val invalidCompletedLessonIds: Set<String>,
        val unknownCompletedLessonIds: Set<String>
    ) {
        val completedCount: Int get() = completedLessonIds.size
        val totalCount: Int get() = orderedLessons.size

        fun indexOfLesson(lessonId: String): Int {
            return orderedLessons.indexOfFirst { it.id == lessonId }
        }

        fun toRenderedLessons(): List<Lesson> {
            return orderedLessons.map { lesson ->
                lesson.copy(isCompleted = completedLessonIds.contains(lesson.id))
            }
        }
    }

    sealed class CompletionChange {
        data class Success(val completedLessonIds: Set<String>) : CompletionChange()
        data class Rejected(val reason: String) : CompletionChange()
    }

    /**
     * Resolves raw completion data into a canonical sequential state.
     * Only a contiguous prefix of completed lessons is considered validly completed.
     */
    fun resolve(lessons: List<Lesson>, rawCompletedLessonIds: Collection<String>): SequentialLessonState {
        val rawSet = rawCompletedLessonIds.toSet()
        val orderedLessons = lessons.sortedBy { it.order }
        val lessonIds = orderedLessons.map { it.id }.toSet()

        val unknownCompleted = rawSet.filter { !lessonIds.contains(it) }.toSet()
        
        var foundGap = false
        val completed = mutableSetOf<String>()
        val invalid = mutableSetOf<String>()
        val unlocked = mutableSetOf<String>()

        orderedLessons.forEachIndexed { index, lesson ->
            // A lesson is unlocked if it's the first one OR the previous one is completed
            if (index == 0 || completed.contains(orderedLessons[index - 1].id)) {
                unlocked.add(lesson.id)
            }

            val isRawCompleted = rawSet.contains(lesson.id)
            if (isRawCompleted) {
                if (!foundGap) {
                    completed.add(lesson.id)
                } else {
                    invalid.add(lesson.id)
                }
            } else {
                foundGap = true
            }
        }

        return SequentialLessonState(
            orderedLessons = orderedLessons,
            rawCompletedLessonIds = rawSet,
            completedLessonIds = completed,
            unlockedLessonIds = unlocked,
            invalidCompletedLessonIds = invalid,
            unknownCompletedLessonIds = unknownCompleted
        )
    }

    /**
     * Applies a completion change and returns the new raw set or a rejection.
     * Enforces that lessons must be completed in order and cannot be uncompleted if subsequent
     * lessons are finished.
     */
    fun applyCompletionChange(
        state: SequentialLessonState,
        lessonId: String,
        isCompleted: Boolean
    ): CompletionChange {
        val lessonIndex = state.indexOfLesson(lessonId)
        if (lessonIndex == -1) return CompletionChange.Rejected("Lesson not found in this phase")

        if (isCompleted) {
            if (!state.unlockedLessonIds.contains(lessonId)) {
                return CompletionChange.Rejected("Complete previous lessons first")
            }
            return CompletionChange.Success(state.rawCompletedLessonIds + lessonId)
        } else {
            if (lessonIndex < state.orderedLessons.size - 1 && state.completedLessonIds.contains(state.orderedLessons[lessonIndex + 1].id)) {
                return CompletionChange.Rejected("Cannot uncomplete while subsequent lessons are finished")
            }
            return CompletionChange.Success(state.rawCompletedLessonIds - lessonId)
        }
    }
}
