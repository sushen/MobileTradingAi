package com.shaplachottor.lab.data

import com.shaplachottor.lab.models.ContentBlockType
import com.shaplachottor.lab.models.Lesson
import com.shaplachottor.lab.models.LessonContentBlock

object Phase4LessonProvider {
    val PHASE_ID = PhaseCatalog.PHASE4

    fun getLessons(): List<Lesson> {
        return listOf(
            Lesson(
                id = "lesson_4_1",
                phaseId = PHASE_ID,
                title = "Scalability Basics",
                order = 1,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Vertical vs Horizontal Scaling",
                        body = "Vertical scaling means adding more power (CPU/RAM) to an existing server. Horizontal scaling means adding more servers to handle the load."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Load Balancing",
                        body = "A Load Balancer distributes incoming traffic across multiple servers so that no single server becomes a bottleneck."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Designing for Growth",
                        body = "Imagine your app grows from 100 users to 1,000,000. Write down 3 parts of your system that would break first and how you would fix them."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "Why is horizontal scaling generally preferred for massive modern applications like Netflix or Google?"
                    )
                )
            ),
            Lesson(
                id = "lesson_4_2",
                phaseId = PHASE_ID,
                title = "Backend Architecture",
                order = 2,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Microservices vs Monolith",
                        body = "A Monolith is a single unit containing all logic. Microservices break the app into small, independent services that communicate over a network."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Communication",
                        body = "In a microservices setup, an 'Order Service' might notify a 'Shipping Service' via an API call or a Message Queue (like RabbitMQ)."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Service Mapping",
                        body = "Sketch out the different services needed for an E-commerce app (e.g., Auth, Catalog, Cart, Payment)."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "What are the trade-offs in complexity when moving from a simple monolith to multiple microservices?"
                    )
                )
            ),
            Lesson(
                id = "lesson_4_3",
                phaseId = PHASE_ID,
                title = "Database Optimization",
                order = 3,
                contentBlocks = listOf(
                    LessonContentBlock(
                        type = ContentBlockType.CONCEPT,
                        title = "Concept: Indexing & Sharding",
                        body = "Indexing makes data retrieval faster (like a book's index). Sharding splits a large database into smaller, faster chunks across different servers."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXAMPLE,
                        title = "Example: Query Performance",
                        body = "Searching for a user by Email in a table of 10 million rows is 100x faster if the 'Email' column has an Index."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.EXERCISE,
                        title = "Exercise: Index Logic",
                        body = "Ask an AI to explain the difference between a Primary Key and a Composite Index in SQL."
                    ),
                    LessonContentBlock(
                        type = ContentBlockType.REFLECTION,
                        title = "Reflection",
                        body = "Can you have too many indexes? How does adding an index affect the speed of writing (saving) new data?"
                    )
                )
            )
        )
    }
}
