package com.shaplachottor.lab.data

import com.shaplachottor.lab.models.ContentBlockType
import com.shaplachottor.lab.models.Lesson
import com.shaplachottor.lab.models.LessonContentBlock

object Phase3LessonProvider {
    val PHASE_ID = PhaseCatalog.PHASE3

    fun getLessons(): List<Lesson> {
        return listOf(
            Lesson(
                id = "lesson_3_1",
                phaseId = PHASE_ID,
                title = "OOP Principles",
                order = 1,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Objects & Classes",
                        body = "Object-Oriented Programming (OOP) is a paradigm based on 'objects' which contain data (attributes) and code (methods). Classes are blueprints for these objects."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: A Trading Bot Class",
                        body = "```python\nclass TradingBot:\n    def __init__(self, strategy):\n        self.strategy = strategy\n    def execute_trade(self):\n        print(f'Executing {self.strategy}')\n```"
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Create a User Class",
                        body = "Define a Class named 'User' with attributes 'name' and 'balance'. Add a method 'deposit' that increases the balance."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "How does grouping data and functions together into a class help prevent bugs in a large system?"
                    )
                )
            ),
            Lesson(
                id = "lesson_3_2",
                phaseId = PHASE_ID,
                title = "Design Patterns",
                order = 2,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Reusable Solutions",
                        body = "Design patterns are standard solutions to common problems in software design. For example, the 'Singleton' ensures only one instance of a class exists."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: The Factory Pattern",
                        body = "A Factory class creates different types of objects (e.g., 'MovingAverageStrategy', 'RSIDivergenceStrategy') without the caller knowing the specific class."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Pattern Identification",
                        body = "Research the 'Observer' pattern. How could it be used to notify a user when a stock price reaches a certain target?"
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "Why is it better to use a proven design pattern rather than inventing your own unique logic for every problem?"
                    )
                )
            ),
            Lesson(
                id = "lesson_3_3",
                phaseId = PHASE_ID,
                title = "Refactoring Code",
                order = 3,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Clean Code",
                        body = "Refactoring is the process of restructuring existing computer code without changing its external behavior. It improves readability and reduces complexity."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Extract Method",
                        body = "Instead of one giant function doing everything, break it into smaller functions like 'validate_data()', 'calculate_profit()', and 'format_output()'."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Cleanup",
                        body = "Take a messy piece of code provided by an AI and ask it to 'refactor this using meaningful variable names and smaller functions'."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "Is refactored code 'better' if it does the exact same thing as the old code? Why or why not?"
                    )
                )
            )
        )
    }
}
