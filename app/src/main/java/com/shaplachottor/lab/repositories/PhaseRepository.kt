package com.shaplachottor.lab.repositories

import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.data.AppStore
import com.shaplachottor.lab.data.AuthSessionProvider
import com.shaplachottor.lab.data.PhaseCatalog
import com.shaplachottor.lab.models.AdvancedFeatures
import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.BookingRequestOutcome
import com.shaplachottor.lab.models.BookingRequestResult
import com.shaplachottor.lab.models.Lesson
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

open class PhaseRepository(
    private val authSessionProvider: AuthSessionProvider = AppGraph.authSessionProvider(),
    private val appStore: AppStore = AppGraph.appStore()
) {

    suspend fun ensurePhasesSeeded(): Boolean {
        return try {
            val existingPhases = appStore.getPhases()
            val existingIds = existingPhases.map { it.phaseId }.toSet()
            
            PhaseCatalog.allPhases.forEach { phase ->
                if (!existingIds.contains(phase.phaseId)) {
                    appStore.setPhase(phase)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPhases(): List<Phase> {
        return try {
            ensurePhasesSeeded()
            val phases = appStore.getPhases()
            if (phases.isEmpty()) {
                PhaseCatalog.allPhases
            } else {
                phases.sortedBy { it.order }
            }
        } catch (e: Exception) {
            PhaseCatalog.allPhases
        }
    }

    suspend fun getPhaseById(phaseId: String): Phase? {
        return try {
            appStore.getPhase(phaseId) ?: PhaseCatalog.findById(phaseId)
        } catch (e: Exception) {
            PhaseCatalog.findById(phaseId)
        }
    }

    suspend fun getLessonsForPhase(phaseId: String): List<Lesson> {
        val userId = authSessionProvider.currentUser()?.uid
        val completedIds = if (userId != null) {
            appStore.getCompletedLessonIds(userId, phaseId)
        } else {
            emptyList()
        }

        return try {
            val baseLessons = when (phaseId) {
                PhaseCatalog.PHASE1 -> listOf(
                    Lesson("L1", "Introduction to AI Coding", "Basics of how AI works in software development", false, "video"),
                    Lesson("L2", "Setting up Environment", "Installing required IDEs and libraries", false, "text"),
                    Lesson("L3", "First Program", "Building a simple AI-assisted application", false, "quiz")
                )
                PhaseCatalog.PHASE2 -> listOf(
                    Lesson("L1", "Data Analysis Fundamentals", "Introduction to data analysis for AI", false, "video"),
                    Lesson("L2", "Working with DataFrames", "Pandas and data manipulation techniques", false, "text"),
                    Lesson("L3", "Visualizing Trends", "Creating meaningful charts from raw data", false, "quiz")
                )
                PhaseCatalog.PHASE3 -> listOf(
                    Lesson("L1", "OOP Principles", "Understanding classes, objects, and inheritance", false, "video"),
                    Lesson("L2", "Design Patterns", "Common architectural patterns in software", false, "text"),
                    Lesson("L3", "Refactoring Code", "Improving system structure for reusability", false, "quiz")
                )
                PhaseCatalog.PHASE4 -> listOf(
                    Lesson("L1", "Scalability Basics", "Fundamentals of high-traffic system design", false, "video"),
                    Lesson("L2", "Backend Architecture", "Building robust server-side flows", false, "text"),
                    Lesson("L3", "Database Optimization", "Designing for performance and reliability", false, "quiz")
                )
                PhaseCatalog.PHASE5 -> listOf(
                    Lesson("L1", "Pipeline Simulation", "Building data-driven simulation environments", false, "video"),
                    Lesson("L2", "Decision Systems", "Logic for model-backed decision making", false, "text"),
                    Lesson("L3", "Data Consistency", "Managing state in complex data systems", false, "quiz")
                )
                PhaseCatalog.PHASE6 -> listOf(
                    Lesson("L1", "CI/CD for AI", "Automated pipelines for production workflows", false, "video"),
                    Lesson("L2", "Monitoring & Alerts", "Observability for deployed AI systems", false, "text"),
                    Lesson("L3", "Reliability Engineering", "Maintenance and scaling in production", false, "quiz")
                )
                else -> emptyList()
            }
            
            baseLessons.map { lesson ->
                lesson.copy(isCompleted = completedIds.contains(lesson.id))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateLessonProgress(phaseId: String, lessonId: String, isCompleted: Boolean): Boolean {
        val userId = authSessionProvider.currentUser()?.uid ?: return false

        return try {
            // 1. Persist individual lesson completion
            appStore.updateLessonCompletion(userId, phaseId, lessonId, isCompleted)

            // 2. Fetch all lessons and their completion to calculate phase progress
            val lessons = getLessonsForPhase(phaseId)
            val totalLessons = lessons.size
            if (totalLessons == 0) return false
            
            val completedCount = lessons.count { it.isCompleted }
            val phaseProgressPercent = (completedCount * 100) / totalLessons

            // 3. Update User object
            val user = appStore.getUser(userId) ?: return false
            val currentPhaseProgressMap = user.phaseProgress.toMutableMap()
            currentPhaseProgressMap[phaseId] = phaseProgressPercent

            val completedPhases = user.completedPhases.toMutableList()
            if (phaseProgressPercent == 100 && !completedPhases.contains(phaseId)) {
                completedPhases.add(phaseId)
                // Trigger affiliate conversion if Phase 1 is completed
                if (phaseId == PhaseCatalog.PHASE1 && user.referredBy != null) {
                    appStore.recordConversion(user.referredBy, userId)
                }
            } else if (phaseProgressPercent < 100 && completedPhases.contains(phaseId)) {
                completedPhases.remove(phaseId)
            }

            val totalProgress = PhaseCatalog.phaseIds.sumOf { catalogPhaseId ->
                currentPhaseProgressMap[catalogPhaseId] ?: 0
            }
            val overallProgress = totalProgress / PhaseCatalog.phaseIds.size
            val features = AdvancedFeatures(
                tradingBot = overallProgress >= 30,
                investment = overallProgress >= 60,
                affiliate = overallProgress >= 100
            )

            appStore.setUser(
                user.copy(
                    phaseProgress = currentPhaseProgressMap,
                    completedPhases = completedPhases,
                    progress = overallProgress,
                    unlockedFeatures = features
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun requestSeat(
        phase: Phase,
        phoneNumber: String,
        whatsappNumber: String
    ): BookingRequestResult {
        val userId = authSessionProvider.currentUser()?.uid
            ?: return BookingRequestResult(BookingRequestOutcome.FAILED)
        val sanitizedPhoneNumber = phoneNumber.trim()
        val sanitizedWhatsappNumber = whatsappNumber.trim()

        if (sanitizedPhoneNumber.isBlank() || sanitizedWhatsappNumber.isBlank()) {
            return BookingRequestResult(BookingRequestOutcome.INVALID_CONTACT_INFO)
        }

        val db = FirebaseFirestore.getInstance()
        return try {
            val bookingId = "${userId}_${phase.phaseId}"
            
            db.runTransaction { transaction ->
                val phaseRef = db.collection("phases").document(phase.phaseId)
                val phaseSnap = transaction.get(phaseRef)
                
                // If not in Firestore yet, use catalog defaults
                val total = if (phaseSnap.exists()) phaseSnap.getLong("totalSeats")?.toInt() ?: 100 else 100
                val booked = if (phaseSnap.exists()) phaseSnap.getLong("bookedSeats")?.toInt() ?: 0 else 0

                if (booked >= total) {
                    return@runTransaction BookingRequestResult(BookingRequestOutcome.NO_SEATS_AVAILABLE)
                }

                val bookingRef = db.collection("bookings").document(bookingId)
                val existingBookingSnap = transaction.get(bookingRef)
                
                if (existingBookingSnap.exists()) {
                    val status = existingBookingSnap.getString("status")
                    if (status == Booking.STATUS_PENDING) {
                        return@runTransaction BookingRequestResult(
                            outcome = BookingRequestOutcome.ALREADY_PENDING,
                            booking = existingBookingSnap.toObject(Booking::class.java)
                        )
                    }
                    if (status == Booking.STATUS_APPROVED) {
                        return@runTransaction BookingRequestResult(
                            outcome = BookingRequestOutcome.ALREADY_APPROVED,
                            booking = existingBookingSnap.toObject(Booking::class.java)
                        )
                    }
                }

                val now = System.currentTimeMillis()
                val booking = Booking(
                    bookingId = bookingId,
                    userId = userId,
                    phaseId = phase.phaseId,
                    phoneNumber = sanitizedPhoneNumber,
                    whatsappNumber = sanitizedWhatsappNumber,
                    createdAt = now,
                    expiresAt = now + Booking.EXPIRATION_WINDOW_MILLIS,
                    status = Booking.STATUS_PENDING
                )

                // Atomic Updates: Reserve Seat + Create Booking
                transaction.set(bookingRef, booking)
                
                // Use set with merge to ensure the phase document exists if it didn't before
                val phaseUpdates = mapOf(
                    "bookedSeats" to booked + 1,
                    "totalSeats" to total,
                    "phaseId" to phase.phaseId,
                    "title" to phase.title,
                    "level" to phase.level,
                    "order" to phase.order
                )
                transaction.set(phaseRef, phaseUpdates, com.google.firebase.firestore.SetOptions.merge())
                
                BookingRequestResult(
                    outcome = BookingRequestOutcome.REQUEST_CREATED,
                    booking = booking
                )
            }.await()
        } catch (e: Exception) {
            BookingRequestResult(BookingRequestOutcome.FAILED)
        }
    }

    suspend fun getCurrentUserBookings(phases: List<Phase>): Map<String, Booking> {
        val userId = authSessionProvider.currentUser()?.uid ?: return emptyMap()

        return try {
            buildMap {
                phases.forEach { phase ->
                    val bookingId = "${userId}_${phase.phaseId}"
                    val booking = appStore.getBooking(bookingId)?.normalizeBooking()
                    if (booking != null) {
                        put(phase.phaseId, booking)
                    }
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun approveBooking(bookingId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.runTransaction { transaction ->
                val bookingRef = db.collection("bookings").document(bookingId)
                val bookingSnap = transaction.get(bookingRef)
                
                if (!bookingSnap.exists() || bookingSnap.getString("status") != Booking.STATUS_PENDING) {
                    return@runTransaction false
                }

                val phaseId = bookingSnap.getString("phaseId") ?: return@runTransaction false
                val userId = bookingSnap.getString("userId") ?: return@runTransaction false
                
                // Seat was already incremented in requestSeat()
                // We just approve and unlock the phase here
                val userRef = db.collection("users").document(userId)
                val userSnap = transaction.get(userRef)
                val unlocked = (userSnap.get("unlockedPhases") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                transaction.update(bookingRef, "status", Booking.STATUS_APPROVED)
                if (!unlocked.contains(phaseId)) {
                    transaction.update(userRef, "unlockedPhases", unlocked + phaseId)
                }
                true
            }.await()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun rejectBooking(bookingId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.runTransaction { transaction ->
                val bookingRef = db.collection("bookings").document(bookingId)
                val bookingSnap = transaction.get(bookingRef)
                
                if (!bookingSnap.exists() || bookingSnap.getString("status") != Booking.STATUS_PENDING) {
                    return@runTransaction false
                }

                val phaseId = bookingSnap.getString("phaseId") ?: return@runTransaction false
                val phaseRef = db.collection("phases").document(phaseId)
                val phaseSnap = transaction.get(phaseRef)
                val booked = phaseSnap.getLong("bookedSeats") ?: 0L

                // Status -> Rejected, Seats -> Decrement (Release the reserved seat)
                transaction.update(bookingRef, "status", Booking.STATUS_REJECTED)
                if (booked > 0) {
                    transaction.update(phaseRef, "bookedSeats", booked - 1)
                }
                true
            }.await()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun cancelBooking(bookingId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.runTransaction { transaction ->
                val bookingRef = db.collection("bookings").document(bookingId)
                val bookingSnap = transaction.get(bookingRef)
                val status = bookingSnap.getString("status") ?: return@runTransaction false
                
                if (status == Booking.STATUS_CANCELLED) return@runTransaction true

                if (status == Booking.STATUS_APPROVED) {
                    val phaseId = bookingSnap.getString("phaseId")!!
                    val userId = bookingSnap.getString("userId")!!
                    
                    val phaseRef = db.collection("phases").document(phaseId)
                    val phaseSnap = transaction.get(phaseRef)
                    val booked = phaseSnap.getLong("bookedSeats") ?: 0
                    
                    if (booked > 0) transaction.update(phaseRef, "bookedSeats", booked - 1)
                    
                    val userRef = db.collection("users").document(userId)
                    val unlocked = (transaction.get(userRef).get("unlockedPhases") as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList()
                    
                    transaction.update(userRef, "unlockedPhases", unlocked - phaseId)
                }
                
                transaction.update(bookingRef, "status", Booking.STATUS_CANCELLED)
                true
            }.await()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun expireBooking(bookingId: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            db.runTransaction { transaction ->
                val bookingRef = db.collection("bookings").document(bookingId)
                val bookingSnap = transaction.get(bookingRef)
                
                if (!bookingSnap.exists() || bookingSnap.getString("status") != Booking.STATUS_PENDING) {
                    return@runTransaction false
                }

                val phaseId = bookingSnap.getString("phaseId") ?: return@runTransaction false
                val phaseRef = db.collection("phases").document(phaseId)
                val phaseSnap = transaction.get(phaseRef)
                val booked = phaseSnap.getLong("bookedSeats") ?: 0L

                // Status -> Expired, Seats -> Decrement
                transaction.update(bookingRef, "status", Booking.STATUS_EXPIRED)
                if (booked > 0) {
                    transaction.update(phaseRef, "bookedSeats", booked - 1)
                }
                true
            }.await()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun canAccessPhase(phaseId: String): Boolean {
        val userId = authSessionProvider.currentUser()?.uid ?: return false
        return try {
            appStore.getUser(userId)?.unlockedPhases?.contains(phaseId) == true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun Booking.normalizeBooking(now: Long = System.currentTimeMillis()): Booking {
        val normalizedStatus = when (status) {
            Booking.STATUS_BOOKED_LEGACY -> Booking.STATUS_APPROVED
            else -> status
        }
        val normalizedCreatedAt = createdAt.takeIf { it > 0L } ?: now
        val normalizedExpiresAt = when {
            normalizedStatus == Booking.STATUS_PENDING && expiresAt <= 0L ->
                normalizedCreatedAt + Booking.EXPIRATION_WINDOW_MILLIS
            normalizedStatus == Booking.STATUS_PENDING -> expiresAt
            else -> 0L
        }
        
        if (normalizedStatus == Booking.STATUS_PENDING && normalizedExpiresAt <= now) {
            // Trigger atomic expiration
            expireBooking(bookingId)
            return copy(status = Booking.STATUS_EXPIRED)
        }

        return this
    }

    private fun buildDefaultUser(
        userId: String,
        unlockedPhaseId: String? = null,
        email: String = "",
        name: String = "Student"
    ): User {
        return User(
            id = userId,
            email = email,
            name = name,
            phaseProgress = unlockedPhaseId?.let { mapOf(it to 0) } ?: emptyMap(),
            unlockedFeatures = AdvancedFeatures(),
            unlockedPhases = unlockedPhaseId?.let(::listOf) ?: emptyList(),
            completedPhases = emptyList()
        )
    }

    private fun User.withUnlockedPhase(phaseId: String): User {
        val unlocked = if (unlockedPhases.contains(phaseId)) unlockedPhases else unlockedPhases + phaseId
        val phaseProgressMap = if (phaseProgress.containsKey(phaseId)) {
            phaseProgress
        } else {
            phaseProgress + (phaseId to 0)
        }
        return copy(unlockedPhases = unlocked, phaseProgress = phaseProgressMap)
    }
}
