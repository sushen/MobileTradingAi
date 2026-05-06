package com.shaplachottor.lab.data

import com.shaplachottor.lab.models.ContentBlockType
import com.shaplachottor.lab.models.Lesson
import com.shaplachottor.lab.models.LessonContentBlock

object Phase1LessonProvider {
    val PHASE_1_ID = PhaseCatalog.PHASE1

    fun getLessons(): List<Lesson> {
        return listOf(
            Lesson(
                id = "lesson_1_1",
                phaseId = PHASE_1_ID,
                title = "AI-to-AI Workflow",
                order = 1,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: AI-to-AI Workflow",
                        body = "AI workflows involve chaining multiple AI interactions together. Instead of expecting a perfect answer in one go, we use multi-step thinking, refinement, and task delegation between different AI models or sessions."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Refinement Loop",
                        body = "Prompt 1: 'Write a Python function to sort a list.'\nResponse 1: [Generic Sort]\nPrompt 2: 'Now optimize this for a very large list and add comments.'\nResponse 2: [Optimized Refined Output]"
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Improve Your Output",
                        body = "1. Ask an AI to explain 'Python loops'.\n2. Take that response and ask the AI to 'explain it as if I am a 10-year old using a pizza analogy'. Observe the difference."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "What changed between the first and second output? Did the structure, tone, or clarity improve?"
                    )
                )
            ),
            Lesson(
                id = "lesson_1_2",
                phaseId = PHASE_1_ID,
                title = "Prompting & AI Conversation",
                order = 2,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Structured Prompting",
                        body = "A good prompt usually contains: \n- Role: 'Act as a Senior Developer'\n- Task: 'Write a script'\n- Context: 'For a data processing pipeline'\n- Format: 'In Markdown with comments'"
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Bad vs Good",
                        body = "Bad: 'Tell me about Python.'\nGood: 'Act as a Python tutor. Provide a 5-step learning plan for a beginner, focusing on data science libraries like Pandas.'"
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: The Structured Prompt",
                        body = "Generate a 'Python learning plan' using a structured prompt containing a Role, Task, Context, and Format."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "Compare the results of a simple prompt vs your structured prompt. Which one is more actionable?"
                    )
                )
            ),
            Lesson(
                id = "lesson_1_3",
                phaseId = PHASE_1_ID,
                title = "Python Basics",
                order = 3,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Programming Fundamentals",
                        body = "Python relies on Variables (storing data), Loops (repeating actions), Conditions (making decisions), and Functions (reusable logic)."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Simple Logic",
                        body = "```python\nfor i in range(5):\n    if i % 2 == 0:\n        print(f'{i} is even')\n```"
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Modify the Code",
                        body = "Use an AI to modify the example code to: \n1. Change the range to 10.\n2. Print 'Odd' for odd numbers instead."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "What did the code do after your modifications? How did the logic change?"
                    )
                )
            ),
            Lesson(
                id = "lesson_1_4",
                phaseId = PHASE_1_ID,
                title = "Thinking in Code",
                order = 4,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Logic over Syntax",
                        body = "Before writing code, define the steps. Problem -> Logic Steps -> Implementation. AI is better at syntax, you must be better at the logic steps."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Problem Solving",
                        body = "Goal: Print numbers 1-10.\nSteps: \n1. Start at 1.\n2. Loop until 10.\n3. In each step, print the current number.\n4. Increment number."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Breakdown",
                        body = "Pick a simple problem (e.g., 'Sum of all numbers in a list') and break it down into 3-4 logical steps before asking AI to code it."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "How did translating the idea into logic steps help you understand what the code was actually doing?"
                    )
                )
            )
        )
    }
}
