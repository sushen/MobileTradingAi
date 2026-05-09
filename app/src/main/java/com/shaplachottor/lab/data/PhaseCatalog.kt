package com.shaplachottor.lab.data

import com.shaplachottor.lab.models.Phase

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
            type = Phase.TYPE_FREE,
            order = 1,
            totalSeats = 100
        ),
        Phase(
            phaseId = PHASE2,
            title = "Data Analysis",
            description = "Master practical data analysis techniques for AI and trading workflows.",
            level = "Beginner",
            type = Phase.TYPE_PREMIUM,
            price = 49.99,
            order = 2,
            totalSeats = 50,
            startDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L) // Next week
        ),
        Phase(
            phaseId = PHASE3,
            title = "Object-Oriented Programming",
            description = "Build reusable systems and strong architecture using OOP principles.",
            level = "Intermediate",
            type = Phase.TYPE_PREMIUM,
            price = 99.99,
            order = 3,
            totalSeats = 30
        ),
        Phase(
            phaseId = PHASE4,
            title = "System Design",
            description = "Design scalable services and robust backend flows for production systems.",
            level = "Intermediate",
            type = Phase.TYPE_PREMIUM,
            price = 149.99,
            order = 4,
            totalSeats = 20
        ),
        Phase(
            phaseId = PHASE5,
            title = "Simulation & Data Systems",
            description = "Build simulation pipelines and data systems for model-backed decisions.",
            level = "Advanced",
            type = Phase.TYPE_PREMIUM,
            price = 199.99,
            order = 5,
            totalSeats = 15
        ),
        Phase(
            phaseId = PHASE6,
            title = "Production Engineering",
            description = "Ship production-grade AI workflows with reliability and monitoring.",
            level = "Advanced",
            type = Phase.TYPE_PREMIUM,
            price = 249.99,
            order = 6,
            totalSeats = 10
        )
    )

    fun findById(id: String): Phase? = allPhases.find { it.phaseId == id }
}
