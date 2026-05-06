package com.shaplachottor.lab.util

import com.shaplachottor.lab.data.PhaseCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCalculatorTest {

    @Test
    fun testCalculatePhaseProgress() {
        assertEquals(0, ProgressCalculator.calculatePhaseProgress(0, 10))
        assertEquals(50, ProgressCalculator.calculatePhaseProgress(5, 10))
        assertEquals(100, ProgressCalculator.calculatePhaseProgress(10, 10))
        assertEquals(0, ProgressCalculator.calculatePhaseProgress(5, 0))
    }

    @Test
    fun testCalculateOverallProgress() {
        val phaseProgressMap = mapOf(
            PhaseCatalog.PHASE1 to 100,
            PhaseCatalog.PHASE2 to 50
        )
        val phaseIds = listOf(PhaseCatalog.PHASE1, PhaseCatalog.PHASE2, PhaseCatalog.PHASE3, PhaseCatalog.PHASE4, PhaseCatalog.PHASE5, PhaseCatalog.PHASE6)
        
        // (100 + 50 + 0 + 0 + 0 + 0) / 6 = 150 / 6 = 25
        assertEquals(25, ProgressCalculator.calculateOverallProgress(phaseProgressMap, phaseIds))
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
        assertTrue(ProgressCalculator.shouldMarkPhaseAsCompleted(100))
        assertFalse(ProgressCalculator.shouldMarkPhaseAsCompleted(99))
        assertFalse(ProgressCalculator.shouldMarkPhaseAsCompleted(0))
    }
}
