package com.shaplachottor.lab.data

import com.shaplachottor.lab.models.ContentBlockType
import com.shaplachottor.lab.models.Lesson
import com.shaplachottor.lab.models.LessonContentBlock

object Phase2LessonProvider {
    val PHASE_ID = PhaseCatalog.PHASE2

    fun getLessons(): List<Lesson> {
        return listOf(
            Lesson(
                id = "lesson_2_1",
                phaseId = PHASE_ID,
                title = "Data Analysis Fundamentals",
                order = 1,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: The Data Pipeline",
                        body = "Data analysis follows a standard flow: Collection -> Cleaning -> Analysis -> Visualization. In AI workflows, cleaning is often 80% of the work."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Cleaning a Price List",
                        body = "Initial Data: ['10.5', 'None', '$12.0', '15']\nGoal: Convert to clean floats.\nSteps: Remove symbols, handle 'None', cast to float."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Raw Data Cleanup",
                        body = "Write a Python script that takes a list of messy strings like ['  23.1 ', 'missing', '45.0'] and returns [23.1, 0.0, 45.0]."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "Why is it dangerous to simply delete 'missing' values instead of replacing them with a default value like 0.0 or the average?"
                    )
                )
            ),
            Lesson(
                id = "lesson_2_2",
                phaseId = PHASE_ID,
                title = "Working with DataFrames",
                order = 2,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Tabular Data (Pandas)",
                        body = "A DataFrame is like an Excel sheet in Python. It allows you to perform operations on thousands of rows simultaneously using 'vectorized' functions."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Filtering Data",
                        body = "```python\nimport pandas as pd\ndf = pd.read_csv('prices.csv')\nhigh_prices = df[df['price'] > 100]\n```"
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Slicing and Dicing",
                        body = "Use an AI to generate a sample CSV of 10 products with 'Name' and 'Price'. Load it into Pandas and find the average price."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "How does using a DataFrame feel different from using a standard Python List for large datasets?"
                    )
                )
            ),
            Lesson(
                id = "lesson_2_3",
                phaseId = PHASE_ID,
                title = "Visualizing Trends",
                order = 3,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Seeing Patterns",
                        body = "Visualization turns numbers into stories. Line charts show trends over time, while histograms show the distribution of your data."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Plotting a Trend",
                        body = "Using Matplotlib or Plotly to visualize a week of stock prices reveals if the market is trending up, down, or sideways."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Generate a Chart",
                        body = "Ask an AI to provide the Python code to plot a simple sine wave using Matplotlib. Run it and change the color of the line."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "Can you spot an outlier (a value that doesn't fit) more easily in a raw list of 100 numbers or a scatter plot? Why?"
                    )
                )
            )
        )
    }
}
