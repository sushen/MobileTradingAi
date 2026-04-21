package com.shaplachottor.app.data

import com.shaplachottor.app.models.Phase

object PhaseCatalog {
    const val PHASE1 = "phase1"
    const val PHASE2 = "phase2"
    const val PHASE3 = "phase3"
    const val PHASE4 = "phase4"
    const val PHASE5 = "phase5"
    const val PHASE6 = "phase6"

    val phaseIds = listOf(PHASE1, PHASE2, PHASE3, PHASE4, PHASE5, PHASE6)

    val allPhases = listOf(
        Phase(
            phaseId = PHASE1,
            title = "Foundations",
            description = "Learn core programming fundamentals required for all future phases. Focus on building basic coding ability and logical thinking.",
            level = "Beginner",
            order = 1,
            totalSeats = 100
        ),
        Phase(
            phaseId = PHASE2,
            title = "Data Analysis",
            description = "Master practical data analysis techniques for AI and trading workflows.",
            level = "Beginner",
            order = 2,
            totalSeats = 100
        ),
        Phase(
            phaseId = PHASE3,
            title = "Object-Oriented Programming",
            description = "Build reusable systems and strong architecture using OOP principles.",
            level = "Intermediate",
            order = 3,
            totalSeats = 100
        ),
        Phase(
            phaseId = PHASE4,
            title = "System Design",
            description = "Design scalable services and robust backend flows for production systems.",
            level = "Intermediate",
            order = 4,
            totalSeats = 100
        ),
        Phase(
            phaseId = PHASE5,
            title = "Simulation & Data Systems",
            description = "Build simulation pipelines and data systems for model-backed decisions.",
            level = "Advanced",
            order = 5,
            totalSeats = 100
        ),
        Phase(
            phaseId = PHASE6,
            title = "Production Engineering",
            description = "Ship production-grade AI workflows with reliability and monitoring.",
            level = "Advanced",
            order = 6,
            totalSeats = 100
        )
    )

    fun findById(id: String): Phase? = allPhases.find { it.phaseId == id }
}
