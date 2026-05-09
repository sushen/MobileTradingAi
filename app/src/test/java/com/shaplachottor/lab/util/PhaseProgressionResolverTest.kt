package com.shaplachottor.lab.util

import com.shaplachottor.lab.data.PhaseCatalog
import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.PhaseLearningProgress
import com.shaplachottor.lab.models.PhaseProgressionState
import com.shaplachottor.lab.models.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseProgressionResolverTest {

    @Test
    fun `phase1 should be approved and ready to enter for a new learner`() {
        val phase1 = PhaseCatalog.findById(PhaseCatalog.PHASE1)!!

        val snapshot = PhaseProgressionResolver.resolve(
            phase = phase1,
            allPhases = PhaseCatalog.allPhases,
            user = User(id = "student_1"),
            booking = null,
            phaseProgress = PhaseLearningProgress(phaseId = phase1.phaseId),
            completedPhaseIds = emptyList()
        )

        assertEquals(PhaseProgressionState.APPROVED, snapshot.state)
        assertTrue(snapshot.canEnterClassroom)
        assertEquals("Enter Classroom", snapshot.actionLabel)
    }

    @Test
    fun `next premium phase should be ready for request after prerequisite completion`() {
        val phase2 = PhaseCatalog.findById(PhaseCatalog.PHASE2)!!

        val snapshot = PhaseProgressionResolver.resolve(
            phase = phase2,
            allPhases = PhaseCatalog.allPhases,
            user = User(id = "student_1", completedPhases = listOf(PhaseCatalog.PHASE1)),
            booking = null,
            phaseProgress = PhaseLearningProgress(phaseId = phase2.phaseId),
            completedPhaseIds = listOf(PhaseCatalog.PHASE1)
        )

        assertEquals(PhaseProgressionState.READY_FOR_REQUEST, snapshot.state)
        assertFalse(snapshot.canEnterClassroom)
        assertTrue(snapshot.isActionEnabled)
        assertEquals("Request Next Phase", snapshot.actionLabel)
    }

    @Test
    fun `reviewing booking should stay pending until teacher decides`() {
        val phase2 = PhaseCatalog.findById(PhaseCatalog.PHASE2)!!
        val booking = Booking(
            bookingId = "student_1_phase2",
            userId = "student_1",
            phaseId = phase2.phaseId,
            status = Booking.STATUS_REVIEWING
        )

        val snapshot = PhaseProgressionResolver.resolve(
            phase = phase2,
            allPhases = PhaseCatalog.allPhases,
            user = User(id = "student_1"),
            booking = booking,
            phaseProgress = PhaseLearningProgress(phaseId = phase2.phaseId),
            completedPhaseIds = listOf(PhaseCatalog.PHASE1)
        )

        assertEquals(PhaseProgressionState.REQUEST_PENDING, snapshot.state)
        assertFalse(snapshot.isActionEnabled)
        assertEquals("Awaiting Teacher Review", snapshot.actionLabel)
    }

    @Test
    fun `premium phase should stay locked until prerequisite is completed`() {
        val phase2 = PhaseCatalog.findById(PhaseCatalog.PHASE2)!!

        val snapshot = PhaseProgressionResolver.resolve(
            phase = phase2,
            allPhases = PhaseCatalog.allPhases,
            user = User(id = "student_1"),
            booking = null,
            phaseProgress = PhaseLearningProgress(phaseId = phase2.phaseId),
            completedPhaseIds = emptyList()
        )

        assertEquals(PhaseProgressionState.LOCKED, snapshot.state)
        assertFalse(snapshot.isActionEnabled)
        assertTrue(snapshot.statusMessage.contains("Complete Foundations before requesting"))
    }

    @Test
    fun `approved classroom should allow entry without auto-marking in progress`() {
        val phase2 = PhaseCatalog.findById(PhaseCatalog.PHASE2)!!
        val booking = Booking(
            bookingId = "student_1_phase2",
            userId = "student_1",
            phaseId = phase2.phaseId,
            status = Booking.STATUS_APPROVED
        )

        val snapshot = PhaseProgressionResolver.resolve(
            phase = phase2,
            allPhases = PhaseCatalog.allPhases,
            user = User(id = "student_1", unlockedPhases = listOf(phase2.phaseId)),
            booking = booking,
            phaseProgress = PhaseLearningProgress(phaseId = phase2.phaseId),
            completedPhaseIds = listOf(PhaseCatalog.PHASE1)
        )

        assertEquals(PhaseProgressionState.APPROVED, snapshot.state)
        assertTrue(snapshot.canEnterClassroom)
        assertEquals("Enter Classroom", snapshot.actionLabel)
    }
}
