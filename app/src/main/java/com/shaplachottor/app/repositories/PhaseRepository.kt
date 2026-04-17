package com.shaplachottor.app.repositories

import com.shaplachottor.app.data.AppGraph
import com.shaplachottor.app.data.AppStore
import com.shaplachottor.app.data.AuthSessionProvider
import com.shaplachottor.app.data.PhaseCatalog
import com.shaplachottor.app.models.AdvancedFeatures
import com.shaplachottor.app.models.Booking
import com.shaplachottor.app.models.BookingRequestOutcome
import com.shaplachottor.app.models.BookingRequestResult
import com.shaplachottor.app.models.Lesson
import com.shaplachottor.app.models.Phase
import com.shaplachottor.app.models.User

open class PhaseRepository(
    private val authSessionProvider: AuthSessionProvider = AppGraph.authSessionProvider(),
    private val appStore: AppStore = AppGraph.appStore()
) {

    suspend fun ensurePhasesSeeded(): Boolean {
        return try {
            if (appStore.getPhaseCount() == 0) {
                PhaseCatalog.allPhases.forEach { phase ->
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
        return try {
            listOf(
                Lesson("L1", "Introduction to AI Trading", "Basics of how AI works in markets", false, "video"),
                Lesson("L2", "Setting up Environment", "Installing Python and libraries", false, "text"),
                Lesson("L3", "First Strategy", "Building a simple moving average bot", false, "quiz")
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateLessonProgress(phaseId: String, lessonId: String, isCompleted: Boolean): Boolean {
        val userId = authSessionProvider.currentUser()?.uid ?: return false

        return try {
            val user = appStore.getUser(userId) ?: return false
            val totalLessons = 3
            val currentPhaseProgressMap = user.phaseProgress.toMutableMap()
            val currentPhaseProgress = currentPhaseProgressMap[phaseId] ?: 0
            val increment = 100 / totalLessons
            var newProgress = if (isCompleted) {
                (currentPhaseProgress + increment).coerceAtMost(100)
            } else {
                (currentPhaseProgress - increment).coerceAtLeast(0)
            }

            if (isCompleted && newProgress >= 90) {
                newProgress = 100
            }

            currentPhaseProgressMap[phaseId] = newProgress

            val completedPhases = user.completedPhases.toMutableList()
            if (newProgress == 100 && !completedPhases.contains(phaseId)) {
                completedPhases.add(phaseId)
            } else if (newProgress < 100 && completedPhases.contains(phaseId)) {
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

        return try {
            val bookingId = "${userId}_${phase.phaseId}"
            val currentPhase = appStore.getPhase(phase.phaseId)
                ?: PhaseCatalog.findById(phase.phaseId)
                ?: throw IllegalStateException("Phase not found")
            val existingBooking = appStore.getBooking(bookingId)?.normalizeBooking()

            when (existingBooking?.status) {
                Booking.STATUS_PENDING -> {
                    return BookingRequestResult(
                        outcome = BookingRequestOutcome.ALREADY_PENDING,
                        booking = existingBooking
                    )
                }

                Booking.STATUS_APPROVED -> {
                    return BookingRequestResult(
                        outcome = BookingRequestOutcome.ALREADY_APPROVED,
                        booking = existingBooking
                    )
                }
            }

            if (currentPhase.bookedSeats >= currentPhase.totalSeats) {
                return BookingRequestResult(BookingRequestOutcome.NO_SEATS_AVAILABLE)
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

            appStore.setBooking(booking)
            BookingRequestResult(
                outcome = BookingRequestOutcome.REQUEST_CREATED,
                booking = booking
            )
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
        return try {
            val booking = appStore.getBooking(bookingId)?.normalizeBooking() ?: return false

            when (booking.status) {
                Booking.STATUS_APPROVED -> true
                Booking.STATUS_EXPIRED -> false
                Booking.STATUS_PENDING -> {
                    val phase = appStore.getPhase(booking.phaseId)
                        ?: PhaseCatalog.findById(booking.phaseId)
                        ?: return false
                    if (phase.bookedSeats >= phase.totalSeats) {
                        appStore.setBooking(booking.copy(status = Booking.STATUS_EXPIRED))
                        return false
                    }

                    val user = appStore.getUser(booking.userId)
                    appStore.setBooking(booking.copy(status = Booking.STATUS_APPROVED))
                    appStore.setPhase(phase.copy(bookedSeats = phase.bookedSeats + 1))
                    appStore.setUser(
                        user?.withUnlockedPhase(booking.phaseId)
                            ?: buildDefaultUser(booking.userId, booking.phaseId)
                    )
                    true
                }

                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun expireBooking(bookingId: String): Boolean {
        return try {
            val booking = appStore.getBooking(bookingId)?.normalizeBooking() ?: return false
            if (booking.status == Booking.STATUS_APPROVED) {
                return false
            }

            if (booking.status != Booking.STATUS_EXPIRED) {
                appStore.setBooking(booking.copy(status = Booking.STATUS_EXPIRED))
            }
            true
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
        val expiredStatus = if (
            normalizedStatus == Booking.STATUS_PENDING &&
            normalizedExpiresAt <= now
        ) {
            Booking.STATUS_EXPIRED
        } else {
            normalizedStatus
        }
        val normalizedBooking = copy(
            createdAt = normalizedCreatedAt,
            expiresAt = normalizedExpiresAt,
            status = expiredStatus
        )

        if (normalizedBooking != this) {
            appStore.setBooking(normalizedBooking)
        }

        return normalizedBooking
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
