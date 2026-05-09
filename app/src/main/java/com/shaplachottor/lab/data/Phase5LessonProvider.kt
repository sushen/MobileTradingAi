package com.shaplachottor.lab.data

import com.shaplachottor.lab.models.ContentBlockType
import com.shaplachottor.lab.models.Lesson
import com.shaplachottor.lab.models.LessonContentBlock

object Phase5LessonProvider {
    val PHASE_ID = PhaseCatalog.PHASE5

    fun getLessons(): List<Lesson> {
        return listOf(
            Lesson(
                id = "lesson_5_1",
                phaseId = PHASE_ID,
                title = "Pipeline Simulation",
                order = 1,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Monte Carlo Simulations",
                        body = "Simulation allows us to test strategies against thousands of random market scenarios to understand the 'probability' of success rather than just looking at past performance."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Random Walk",
                        body = "A random walk simulation models a stock price by adding a random percentage change at each step. Repeating this 10,000 times creates a distribution of possible future prices."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Scenario Testing",
                        body = "Ask an AI to generate Python code for a 'Monte Carlo simulation' that predicts the future value of a $1,000 investment over 12 months with 5% average growth and 10% volatility."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "Why is simulation more useful for risk management than simply assuming 'the future will look exactly like the past'?"
                    )
                )
            ),
            Lesson(
                id = "lesson_5_2",
                phaseId = PHASE_ID,
                title = "Decision Systems",
                order = 2,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Logic Engines",
                        body = "A decision system takes inputs (data, indicators, sentiment) and applies a ruleset to output an action (Buy, Sell, Hold). In advanced AI, these rules are learned rather than hardcoded."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Threshold Logic",
                        body = "IF (RSI < 30 AND Volume > Average) THEN Action = 'Buy'. This is a simple deterministic decision system."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Rule Design",
                        body = "Design a decision system in plain English that would prevent a bot from trading during high-impact news events (like an interest rate announcement)."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "What happens to a decision system if the 'logic' is too rigid? What happens if it is too flexible?"
                    )
                )
            ),
            Lesson(
                id = "lesson_5_3",
                phaseId = PHASE_ID,
                title = "Data Consistency",
                order = 3,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Atomic Updates",
                        body = "In high-stakes systems, data must be consistent. An 'Atomic' operation ensures that a multi-step update (like moving money from one account to another) either succeeds completely or fails completely."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Transactional Integrity",
                        body = "If you approve a seat but the count doesn't increment, your data is 'corrupt'. Using a Firestore Transaction prevents this by locking the documents during the update."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Identifying Race Conditions",
                        body = "Two users click 'Book last seat' at the exact same millisecond. Explain how a transaction prevents both from getting the seat."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "How does data inconsistency lead to system failure in a financial application?"
                    )
                )
            )
        )
    }
}
