package com.shaplachottor.lab.data

import com.shaplachottor.lab.models.ContentBlockType
import com.shaplachottor.lab.models.Lesson
import com.shaplachottor.lab.models.LessonContentBlock

object Phase6LessonProvider {
    val PHASE_ID = PhaseCatalog.PHASE6

    fun getLessons(): List<Lesson> {
        return listOf(
            Lesson(
                id = "lesson_6_1",
                phaseId = PHASE_ID,
                title = "CI/CD for AI",
                order = 1,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Continuous Delivery",
                        body = "CI/CD (Continuous Integration / Continuous Deployment) ensures that every change to your AI code or model is automatically tested and deployed to production if it passes quality checks."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: GitHub Actions",
                        body = "A GitHub Action can be set up to run your unit tests every time you push code. If a test fails (e.g., your bot makes an invalid trade in simulation), the deployment is blocked."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Workflow Design",
                        body = "List the 3 most important tests an AI trading bot must pass before it is allowed to trade with real capital (e.g., Connectivity check, Strategy Sanity, Risk Limit verification)."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "How does automation reduce 'human error' in the deployment of complex AI systems?"
                    )
                )
            ),
            Lesson(
                id = "lesson_6_2",
                phaseId = PHASE_ID,
                title = "Monitoring & Alerts",
                order = 2,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Real-time Observability",
                        body = "Monitoring is the process of collecting data about your system's performance. Alerts notify you immediately when something goes wrong (e.g., your bot loses 5% in an hour)."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Dashboarding",
                        body = "Using tools like Grafana or custom dashboards to track 'Bot Latency' and 'Success Rate' allows you to spot issues before they become catastrophes."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Alert Thresholds",
                        body = "Define a 'Critical' alert for a trading system. What specific metric would trigger it, and what should be the immediate automated response (e.g., 'Kill Switch')?"
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "If a system is running perfectly today, why do we still need to spend time building monitoring tools for it?"
                    )
                )
            ),
            Lesson(
                id = "lesson_6_3",
                phaseId = PHASE_ID,
                title = "Reliability Engineering",
                order = 3,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: SRE Principles",
                        body = "Site Reliability Engineering (SRE) focuses on making systems highly available and scalable. It treats operations as a software problem."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Redundancy",
                        body = "Running your bot on two different servers in two different regions ensures that if one data center goes offline, your trades can still be managed by the other."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Failure Mode Analysis",
                        body = "Analyze what would happen if your internet connection fails while your bot has an open position. How can you design the system to handle this 'Failure Mode'?"
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "What is more important in a production AI system: 100% accuracy or 100% reliability? Can you have one without the other?"
                    )
                )
            )
        )
    }
}
