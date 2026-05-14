package com.shaplachottor.lab.util

import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.PhaseLearningProgress
import com.shaplachottor.lab.models.PhaseProgressionSnapshot
import com.shaplachottor.lab.models.PhaseProgressionState
import com.shaplachottor.lab.models.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhaseProgressionResolver {
    fun resolve(
        phase: Phase,
        allPhases: List<Phase>,
        user: User?,
        booking: Booking?,
        phaseProgress: PhaseLearningProgress?,
        completedPhaseIds: List<String>,
        now: Long = System.currentTimeMillis()
    ): PhaseProgressionSnapshot {
        val sortedPhases = allPhases.sortedBy { it.order }
        val prerequisitePhase = sortedPhases.firstOrNull { it.order == phase.order - 1 }
        val progress = phaseProgress ?: PhaseLearningProgress(phaseId = phase.phaseId)
        val isCompleted = completedPhaseIds.contains(phase.phaseId) || progress.isCompleted
        
        // CORE ACCESS RULE: Phase access is granted if it's in unlockedPhases OR if the booking is APPROVED.
        // This provides redundancy against sync delays between the 'users' and 'bookings' collections.
        val isUnlocked = user?.unlockedPhases.orEmpty().contains(phase.phaseId) ||
            booking?.status == Booking.STATUS_APPROVED

        val prerequisiteMet = prerequisitePhase == null ||
            completedPhaseIds.contains(prerequisitePhase.phaseId)
        val hasFutureStartDate = phase.startDate > now
        val startDateSuffix = if (hasFutureStartDate) {
            val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(Date(phase.startDate))
            " Next classroom starts $formattedDate."
        } else {
            ""
        }

        return when {
            isCompleted -> {
                PhaseProgressionSnapshot(
                    phase = phase,
                    state = PhaseProgressionState.COMPLETED,
                    badgeLabel = "COMPLETED",
                    statusMessage = "Classroom progress reached 100%. Review this class anytime.$startDateSuffix",
                    actionLabel = "Review Classroom",
                    isActionEnabled = true,
                    canEnterClassroom = true,
                    phaseProgress = progress,
                    booking = booking,
                    prerequisitePhase = prerequisitePhase
                )
            }

            // CORE ACCESS: If unlocked, always allow entry regardless of booking status details
            isUnlocked -> {
                val state = if (progress.completedLessons > 0) PhaseProgressionState.IN_PROGRESS else PhaseProgressionState.APPROVED
                val badge = if (progress.completedLessons > 0) "IN PROGRESS" else "APPROVED"
                val message = if (progress.completedLessons > 0) {
                    "Keep learning here, then practice outside the app before requesting the next phase.$startDateSuffix"
                } else {
                    "Teacher approved this classroom. You can enter now.$startDateSuffix"
                }

                PhaseProgressionSnapshot(
                    phase = phase,
                    state = state,
                    badgeLabel = badge,
                    statusMessage = message,
                    actionLabel = "Enter Classroom",
                    isActionEnabled = true,
                    canEnterClassroom = true,
                    phaseProgress = progress,
                    booking = booking,
                    prerequisitePhase = prerequisitePhase
                )
            }

            // Pending Review
            booking?.status == Booking.STATUS_PENDING || booking?.status == Booking.STATUS_REVIEWING -> {
                val isReviewing = booking.status == Booking.STATUS_REVIEWING
                val statusMessage = if (isReviewing) {
                    "Teacher is reviewing your readiness for this classroom."
                } else {
                    val hasExpiry = booking.expiresAt > now
                    if (hasExpiry) {
                        val minutesRemaining = ((booking.expiresAt - now) / 60_000L).coerceAtLeast(0L)
                        String.format(Locale.getDefault(), "Teacher pickup pending. Seat hold expires in %d min.", minutesRemaining)
                    } else {
                        "Pending request expired. Submit a new request when ready."
                    }
                }
                PhaseProgressionSnapshot(
                    phase = phase,
                    state = PhaseProgressionState.REQUEST_PENDING,
                    badgeLabel = "PENDING",
                    statusMessage = statusMessage,
                    actionLabel = "Awaiting Teacher Review",
                    isActionEnabled = false,
                    canEnterClassroom = false,
                    phaseProgress = progress,
                    booking = booking,
                    prerequisitePhase = prerequisitePhase
                )
            }

            // Rejected
            booking?.status == Booking.STATUS_REJECTED -> {
                PhaseProgressionSnapshot(
                    phase = phase,
                    state = PhaseProgressionState.REJECTED,
                    badgeLabel = "REJECTED",
                    statusMessage = "Teacher asked for more practice before the next classroom. You can request again later.$startDateSuffix",
                    actionLabel = "Request Again",
                    isActionEnabled = prerequisiteMet && phase.availableSeats > 0,
                    canEnterClassroom = false,
                    phaseProgress = progress,
                    booking = booking,
                    prerequisitePhase = prerequisitePhase
                )
            }

            // Locked (Prerequisite not met)
            !prerequisiteMet -> {
                val prerequisiteTitle = prerequisitePhase?.title ?: "the previous phase"
                PhaseProgressionSnapshot(
                    phase = phase,
                    state = PhaseProgressionState.LOCKED,
                    badgeLabel = "LOCKED",
                    statusMessage = "Complete $prerequisiteTitle before requesting this classroom.",
                    actionLabel = "Locked",
                    isActionEnabled = false,
                    canEnterClassroom = false,
                    phaseProgress = progress,
                    booking = booking,
                    prerequisitePhase = prerequisitePhase
                )
            }

            // Ready for Request (Prerequisite met but not unlocked/booked)
            prerequisiteMet && !isUnlocked -> {
                if (phase.availableSeats <= 0) {
                    PhaseProgressionSnapshot(
                        phase = phase,
                        state = PhaseProgressionState.LOCKED,
                        badgeLabel = "LOCKED",
                        statusMessage = "Seats are currently full for this classroom.$startDateSuffix",
                        actionLabel = "No Seats Available",
                        isActionEnabled = false,
                        canEnterClassroom = false,
                        phaseProgress = progress,
                        booking = booking,
                        prerequisitePhase = prerequisitePhase
                    )
                } else {
                    PhaseProgressionSnapshot(
                        phase = phase,
                        state = PhaseProgressionState.READY_FOR_REQUEST,
                        badgeLabel = "LOCKED",
                        statusMessage = "Prerequisite complete.$startDateSuffix",
                        actionLabel = "Book Class",
                        isActionEnabled = true,
                        canEnterClassroom = false,
                        phaseProgress = progress,
                        booking = booking,
                        prerequisitePhase = prerequisitePhase
                    )
                }
            }

            else -> {
                // Fallback / Available (Should rarely be hit with above logic)
                PhaseProgressionSnapshot(
                    phase = phase,
                    state = PhaseProgressionState.AVAILABLE,
                    badgeLabel = "LOCKED",
                    statusMessage = "This classroom is available now.$startDateSuffix",
                    actionLabel = "Book Class",
                    isActionEnabled = true,
                    canEnterClassroom = false,
                    phaseProgress = progress,
                    booking = booking,
                    prerequisitePhase = prerequisitePhase
                )
            }
        }
    }
}
