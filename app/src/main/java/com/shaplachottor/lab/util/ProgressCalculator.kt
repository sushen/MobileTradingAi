package com.shaplachottor.lab.util

import com.shaplachottor.lab.data.PhaseCatalog
import com.shaplachottor.lab.models.AdvancedFeatures

object ProgressCalculator {
    fun calculatePhaseProgress(completedCount: Int, totalLessons: Int): Int {
        if (totalLessons == 0) return 0
        return (completedCount * 100) / totalLessons
    }

    fun calculateOverallProgress(phaseProgressMap: Map<String, Int>, phaseIds: List<String>): Int {
        if (phaseIds.isEmpty()) return 0
        val totalProgress = phaseIds.sumOf { pid ->
            phaseProgressMap[pid] ?: 0
        }
        return totalProgress / phaseIds.size
    }

    fun calculateUnlockedFeatures(overallProgress: Int): AdvancedFeatures {
        return AdvancedFeatures(
            tradingBot = overallProgress >= 30,
            investment = overallProgress >= 60,
            affiliate = overallProgress >= 100
        )
    }

    fun shouldMarkPhaseAsCompleted(phaseProgressPercent: Int): Boolean {
        return phaseProgressPercent == 100
    }
}
