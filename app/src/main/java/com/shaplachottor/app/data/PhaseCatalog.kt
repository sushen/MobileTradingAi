package com.shaplachottor.app.data

import com.shaplachottor.app.models.Phase

object PhaseCatalog {
    const val PHASE1 = "phase1_foundations"
    const val PHASE2 = "phase2_data_analysis"
    const val PHASE3 = "phase3_oop"
    const val PHASE4 = "phase4_system_design"
    const val PHASE5 = "phase5_simulation"
    const val PHASE6 = "phase6_production"

    val phaseIds = listOf(PHASE1, PHASE2, PHASE3, PHASE4, PHASE5, PHASE6)

    val allPhases = listOf(
        Phase(
            phaseId = PHASE1,
            title = "Foundations",
            description = "Introduction to Trading and AI",
            level = "Beginner",
            order = 1,
            totalSeats = 100
        ),
        Phase(
            phaseId = PHASE2,
            title = "Data Analysis",
            description = "Handling and analyzing financial data",
            level = "Beginner",
            order = 2,
            totalSeats = 100
        ),
        Phase(
            phaseId = PHASE3,
            title = "Object-Oriented Programming",
            description = "Building robust trading systems",
            level = "Intermediate",
            order = 3,
            totalSeats = 50
        ),
        Phase(
            phaseId = PHASE4,
            title = "System Design",
            description = "Architecture for high-frequency trading",
            level = "Intermediate",
            order = 4,
            totalSeats = 50
        ),
        Phase(
            phaseId = PHASE5,
            title = "Simulation & Data Systems",
            description = "Backtesting and data pipelines",
            level = "Advanced",
            order = 5,
            totalSeats = 20
        ),
        Phase(
            phaseId = PHASE6,
            title = "Production Engineering",
            description = "Deploying and monitoring bots",
            level = "Advanced",
            order = 6,
            totalSeats = 20
        )
    )

    fun findById(id: String): Phase? = allPhases.find { it.phaseId == id }
}
