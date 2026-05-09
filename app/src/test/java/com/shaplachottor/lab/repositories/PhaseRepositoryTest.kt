package com.shaplachottor.lab.repositories

import com.google.firebase.auth.FirebaseUser
import com.shaplachottor.lab.data.AppStore
import com.shaplachottor.lab.data.AuthSessionProvider
import com.shaplachottor.lab.data.PhaseCatalog
import com.shaplachottor.lab.models.BookingRequestOutcome
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PhaseRepositoryTest {

    private lateinit var repository: PhaseRepository
    private val authProvider = mockk<AuthSessionProvider>()
    private val appStore = mockk<AppStore>()
    private val firebaseUser = mockk<FirebaseUser>()

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        repository = PhaseRepository(authProvider, appStore)
        every { authProvider.currentUser() } returns firebaseUser
        every { firebaseUser.uid } returns "test_user_id"
        every { firebaseUser.email } returns "student@example.com"
        coEvery { appStore.getCompletedLessonIds(any(), any()) } returns emptyList()
        coEvery { appStore.getPhase(any()) } returns null
    }

    @Test
    fun `updateLessonProgress should fail if previous lesson is not completed`() = runBlocking {
        // Arrange: Phase 1. User has NOT completed L1, but tries to complete L2
        // Act
        val result = repository.updateLessonProgress(PhaseCatalog.PHASE1, "lesson_1_2", true)

        // Assert
        assertFalse("Should block completing L2 if L1 is not done", result)
    }

    @Test
    fun `requestSeat should fail if previous phase is not completed`() = runBlocking {
        // Arrange: User wants Phase 2 but hasn't completed Phase 1
        val phase2 = PhaseCatalog.allPhases[1] // Phase 2
        val user = User(id = "test_user_id", completedPhases = emptyList())
        coEvery { appStore.getUser("test_user_id") } returns user

        // Act
        val result = repository.requestSeat(phase2, "123", "456")

        // Assert
        assertEquals(BookingRequestOutcome.PREREQUISITE_NOT_MET, result.outcome)
    }

    @Test
    fun `canAccessPhase should return false if previous phase not completed`() = runBlocking {
        // Arrange: User has unlocked Phase 2 (accidentally) but hasn't completed Phase 1
        val user = User(
            id = "test_user_id", 
            unlockedPhases = listOf(PhaseCatalog.PHASE1, PhaseCatalog.PHASE2),
            completedPhases = emptyList() // PHASE1 missing
        )
        coEvery { appStore.getUser("test_user_id") } returns user

        // Act
        val canAccess = repository.canAccessPhase(PhaseCatalog.PHASE2)

        // Assert
        assertFalse("Should deny access to Phase 2 if Phase 1 isn't completed", canAccess)
    }

    @Test
    fun `canAccessPhase should return true for Phase 1 regardless of completion`() = runBlocking {
        // Phase 1 has no prerequisites
        val user = User(
            id = "test_user_id", 
            unlockedPhases = listOf(PhaseCatalog.PHASE1),
            completedPhases = emptyList()
        )
        coEvery { appStore.getUser("test_user_id") } returns user

        val canAccess = repository.canAccessPhase(PhaseCatalog.PHASE1)
        assertTrue(canAccess)
    }

    @Test
    fun `getLearningJourneyProgress should treat phase1 as active for new free user`() = runBlocking {
        val user = User(id = "test_user_id")
        coEvery { appStore.getUser("test_user_id") } returns user

        val learningJourneyProgress = repository.getLearningJourneyProgress()

        assertNotNull(learningJourneyProgress)
        assertEquals(PhaseCatalog.PHASE1, learningJourneyProgress?.activePhaseId)
        assertEquals(0, learningJourneyProgress?.currentPhaseProgress?.progress?.completedLessons)
        assertEquals(4, learningJourneyProgress?.currentPhaseProgress?.progress?.totalLessons)
        assertEquals(0, learningJourneyProgress?.overallLearningProgress?.percent)
    }

    @Test
    fun `getLearningJourneyProgress should keep first incomplete accessible phase as active`() = runBlocking {
        val user = User(
            id = "test_user_id",
            unlockedPhases = listOf(PhaseCatalog.PHASE2)
        )
        coEvery { appStore.getUser("test_user_id") } returns user
        coEvery {
            appStore.getCompletedLessonIds("test_user_id", PhaseCatalog.PHASE1)
        } returns listOf("lesson_1_1")

        val learningJourneyProgress = repository.getLearningJourneyProgress()

        assertNotNull(learningJourneyProgress)
        assertEquals(PhaseCatalog.PHASE1, learningJourneyProgress?.activePhaseId)
        assertEquals(1, learningJourneyProgress?.currentPhaseProgress?.progress?.completedLessons)
        assertEquals(4, learningJourneyProgress?.currentPhaseProgress?.progress?.totalLessons)
        assertEquals(4, learningJourneyProgress?.overallLearningProgress?.percent)
    }

    @Test
    fun `getLearningJourneyProgress should move to phase2 after phase1 completion`() = runBlocking {
        val user = User(
            id = "test_user_id",
            unlockedPhases = listOf(PhaseCatalog.PHASE2)
        )
        coEvery { appStore.getUser("test_user_id") } returns user
        coEvery {
            appStore.getCompletedLessonIds("test_user_id", PhaseCatalog.PHASE1)
        } returns listOf("lesson_1_1", "lesson_1_2", "lesson_1_3", "lesson_1_4")

        val learningJourneyProgress = repository.getLearningJourneyProgress()

        assertNotNull(learningJourneyProgress)
        assertEquals(PhaseCatalog.PHASE2, learningJourneyProgress?.activePhaseId)
        assertEquals(16, learningJourneyProgress?.overallLearningProgress?.percent)
        assertTrue(learningJourneyProgress?.completedPhaseIds?.contains(PhaseCatalog.PHASE1) == true)
    }
}
