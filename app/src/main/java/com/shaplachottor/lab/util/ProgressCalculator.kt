package com.shaplachottor.lab.util

import android.util.Log
import com.shaplachottor.lab.models.AdvancedFeatures

object ProgressCalculator {
    private const val TAG = "ProgressCalculator"

    data class PhaseStats(
        val phaseId: String,
        val completedCount: Int,
        val totalCount: Int
    )

    /**
     * Calculates progress inside a single phase.
     *
     * A phase with no lessons yields 0 percent so the journey never reports
     * phantom completion for content that does not exist yet.
     */
    fun calculatePhaseProgress(completedCount: Int, totalCount: Int): Int {
        if (totalCount == 0) {
            Log.d(TAG, "Phase has 0 lessons. Returning 0% phase progress.")
            return 0
        }
        val safeCompletedCount = completedCount.coerceIn(0, totalCount)
        val progress = (safeCompletedCount * 100) / totalCount
        return progress.coerceIn(0, 100)
    }

    /**
     * Calculates overall learning progress across the full journey.
     *
     * Each phase contributes an equal share of the total platform journey.
     * Lesson completion only fills that phase's share.
     */
    fun calculateOverallProgress(allStats: List<PhaseStats>): Int {
        if (allStats.isEmpty()) return 0

        val totalPhases = allStats.size
        val journeyUnitsPerPhase = 10_000L
        val earnedJourneyUnits = allStats.sumOf { stat ->
            if (stat.totalCount <= 0) {
                Log.d(
                    TAG,
                    "Phase ${stat.phaseId} has 0 lessons. Contributing 0 journey units."
                )
                0L
            } else {
                val safeCompletedCount = stat.completedCount.coerceIn(0, stat.totalCount)
                val phaseUnits = (safeCompletedCount.toLong() * journeyUnitsPerPhase) / stat.totalCount
                val phasePercent = calculatePhaseProgress(safeCompletedCount, stat.totalCount)
                Log.d(
                    TAG,
                    "Phase ${stat.phaseId} contribution: completed=$safeCompletedCount/${stat.totalCount}, phasePercent=$phasePercent, phaseUnits=$phaseUnits"
                )
                phaseUnits
            }
        }

        val totalJourneyUnits = totalPhases * journeyUnitsPerPhase
        val overallProgress = if (totalJourneyUnits == 0L) {
            0
        } else {
            ((earnedJourneyUnits * 100) / totalJourneyUnits).toInt()
        }

        Log.d(TAG, "Overall Progress Calculation:")
        Log.d(TAG, "- Total Phases: $totalPhases")
        Log.d(TAG, "- Earned Journey Units: $earnedJourneyUnits / $totalJourneyUnits")
        Log.d(TAG, "- Calculated Overall: $overallProgress%")

        return overallProgress.coerceIn(0, 100)
    }

    fun calculateUnlockedFeatures(overallProgress: Int): AdvancedFeatures {
        val features = AdvancedFeatures(
            tradingBot = overallProgress >= 30,
            investment = overallProgress >= 60,
            affiliate = overallProgress >= 100
        )
        Log.d(TAG, "Unlocked Features: Bot=${features.tradingBot}, Invest=${features.investment}, Affiliate=${features.affiliate} (Progress: $overallProgress%)")
        return features
    }

    fun shouldMarkPhaseAsCompleted(completedCount: Int, totalCount: Int): Boolean {
        return totalCount > 0 && completedCount >= totalCount
    }
}
