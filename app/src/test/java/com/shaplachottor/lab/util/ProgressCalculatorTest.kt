package com.shaplachottor.lab.util

import com.shaplachottor.lab.data.PhaseCatalog
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCalculatorTest {

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
    }

    @Test
    fun testCalculatePhaseProgress() {
        assertEquals(0, ProgressCalculator.calculatePhaseProgress(0, 10))
        assertEquals(50, ProgressCalculator.calculatePhaseProgress(5, 10))
        assertEquals(100, ProgressCalculator.calculatePhaseProgress(10, 10))
        assertEquals(0, ProgressCalculator.calculatePhaseProgress(5, 0))
    }

    @Test
    fun testCalculateOverallProgressUsesPhaseWeightedJourney() {
        val allStats = listOf(
            ProgressCalculator.PhaseStats(
                phaseId = PhaseCatalog.PHASE1,
                completedCount = 1,
                totalCount = 4
            ),
            ProgressCalculator.PhaseStats(
                phaseId = PhaseCatalog.PHASE2,
                completedCount = 1,
                totalCount = 4
            ),
            ProgressCalculator.PhaseStats(
                phaseId = PhaseCatalog.PHASE3,
                completedCount = 0,
                totalCount = 0
            )
        )

        assertEquals(16, ProgressCalculator.calculateOverallProgress(allStats))
    }

    @Test
    fun testCalculateOverallProgressTreatsEachPhaseAsEqualJourneyWeight() {
        val allStats = listOf(
            ProgressCalculator.PhaseStats(PhaseCatalog.PHASE1, completedCount = 4, totalCount = 4),
            ProgressCalculator.PhaseStats(PhaseCatalog.PHASE2, completedCount = 0, totalCount = 3),
            ProgressCalculator.PhaseStats(PhaseCatalog.PHASE3, completedCount = 0, totalCount = 3),
            ProgressCalculator.PhaseStats(PhaseCatalog.PHASE4, completedCount = 0, totalCount = 3),
            ProgressCalculator.PhaseStats(PhaseCatalog.PHASE5, completedCount = 0, totalCount = 3),
            ProgressCalculator.PhaseStats(PhaseCatalog.PHASE6, completedCount = 0, totalCount = 3)
        )

        assertEquals(16, ProgressCalculator.calculateOverallProgress(allStats))
    }

    @Test
    fun testCalculateUnlockedFeatures() {
        val featuresUnder30 = ProgressCalculator.calculateUnlockedFeatures(25)
        assertFalse(featuresUnder30.tradingBot)
        assertFalse(featuresUnder30.investment)
        assertFalse(featuresUnder30.affiliate)

        val featuresAt30 = ProgressCalculator.calculateUnlockedFeatures(30)
        assertTrue(featuresAt30.tradingBot)
        assertFalse(featuresAt30.investment)
        assertFalse(featuresAt30.affiliate)

        val featuresAt60 = ProgressCalculator.calculateUnlockedFeatures(60)
        assertTrue(featuresAt60.tradingBot)
        assertTrue(featuresAt60.investment)
        assertFalse(featuresAt60.affiliate)

        val featuresAt100 = ProgressCalculator.calculateUnlockedFeatures(100)
        assertTrue(featuresAt100.tradingBot)
        assertTrue(featuresAt100.investment)
        assertTrue(featuresAt100.affiliate)
    }

    @Test
    fun testShouldMarkPhaseAsCompleted() {
        assertTrue(ProgressCalculator.shouldMarkPhaseAsCompleted(4, 4))
        assertFalse(ProgressCalculator.shouldMarkPhaseAsCompleted(3, 4))
        assertFalse(ProgressCalculator.shouldMarkPhaseAsCompleted(0, 0))
    }
}
