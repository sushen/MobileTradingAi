package com.shaplachottor.lab.models

data class LearningJourneyProgress(
    val currentPhaseProgress: CurrentPhaseProgress? = null,
    val overallLearningProgress: OverallLearningProgress = OverallLearningProgress(),
    val phaseProgressById: Map<String, PhaseLearningProgress> = emptyMap(),
    val completedPhaseIds: List<String> = emptyList(),
    val unlockedPhaseIds: List<String> = emptyList()
) {
    val activePhaseId: String?
        get() = currentPhaseProgress?.phase?.phaseId
}

data class CurrentPhaseProgress(
    val phase: Phase,
    val progress: PhaseLearningProgress
)

data class OverallLearningProgress(
    val percent: Int = 0,
    val completedPhases: Int = 0,
    val totalPhases: Int = 0
)

data class PhaseLearningProgress(
    val phaseId: String = "",
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val percent: Int = 0
) {
    val isCompleted: Boolean
        get() = totalLessons > 0 && completedLessons >= totalLessons
}
