package com.shaplachottor.lab.models

enum class PhaseProgressionState {
    LOCKED,
    READY_FOR_REQUEST,
    REQUEST_PENDING,
    APPROVED,
    IN_PROGRESS,
    COMPLETED,
    REJECTED,
    AVAILABLE
}

data class PhaseProgressionSnapshot(
    val phase: Phase,
    val state: PhaseProgressionState,
    val badgeLabel: String,
    val statusMessage: String,
    val actionLabel: String,
    val isActionEnabled: Boolean,
    val canEnterClassroom: Boolean,
    val phaseProgress: PhaseLearningProgress = PhaseLearningProgress(phaseId = phase.phaseId),
    val booking: Booking? = null,
    val prerequisitePhase: Phase? = null
)
