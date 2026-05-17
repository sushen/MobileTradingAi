package com.shaplachottor.lab.repositories

import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.data.AppStore
import com.shaplachottor.lab.data.AuthSessionProvider
import com.shaplachottor.lab.data.PhaseCatalog
import com.shaplachottor.lab.models.AdvancedFeatures
import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.BookingRequestOutcome
import com.shaplachottor.lab.models.BookingRequestResult
import com.shaplachottor.lab.models.CurrentPhaseProgress
import com.shaplachottor.lab.models.Lesson
import com.shaplachottor.lab.models.LearningJourneyProgress
import com.shaplachottor.lab.models.OverallLearningProgress
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.PhaseLearningProgress
import com.shaplachottor.lab.models.PhaseProgressionSnapshot
import com.shaplachottor.lab.models.User
import com.shaplachottor.lab.util.PhaseProgressionResolver
import com.shaplachottor.lab.util.ProgressCalculator
import com.shaplachottor.lab.util.SequentialLessonProgressResolver
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

open class PhaseRepository(
    private val authSessionProvider: AuthSessionProvider = AppGraph.authSessionProvider(),
    private val appStore: AppStore = AppGraph.appStore()
) {
    companion object {
        private const val TAG = "PhaseRepository"
    }

    private data class PhaseLessonSnapshot(
        val phaseId: String,
        val state: SequentialLessonProgressResolver.SequentialLessonState,
        val lessons: List<Lesson>,
        val phaseProgressPercent: Int
    ) {
        val completedCount: Int
            get() = state.completedCount

        val totalCount: Int
            get() = state.totalCount

        val repairLessonIds: Set<String>
            get() = linkedSetOf<String>().apply {
                addAll(state.invalidCompletedLessonIds)
                addAll(state.unknownCompletedLessonIds)
            }

        fun toPhaseLearningProgress(): PhaseLearningProgress {
            return PhaseLearningProgress(
                phaseId = phaseId,
                completedLessons = completedCount,
                totalLessons = totalCount,
                percent = phaseProgressPercent
            )
        }
    }

    private data class UserProgressSnapshot(
        val phaseSnapshots: List<PhaseLessonSnapshot>,
        val phaseProgress: Map<String, Int>,
        val completedPhases: List<String>,
        val overallProgress: Int,
        val unlockedFeatures: AdvancedFeatures
    )

    suspend fun ensurePhasesSeeded(): Boolean {
        // Only the admin can seed the entire catalog to avoid permission errors for students
        val email = authSessionProvider.currentUser()?.email
        if (email != "sushen.biswas.aga@gmail.com" && email != "sushen.biswas.aga@googlemail.com") return false
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
        return try {
            getPhaseLessonSnapshot(phaseId).lessons
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to load lessons for phaseId=$phaseId", e)
            emptyList()
        }
    }

    suspend fun getLearningJourneyProgress(user: User? = null): LearningJourneyProgress? {
        val userId = authSessionProvider.currentUser()?.uid ?: return null
        val currentUser = user ?: appStore.getUser(userId) ?: return null

        return try {
            val phases = getPhases().sortedBy { it.order }
            val userProgressSnapshot = buildUserProgressSnapshot(userId)
            buildLearningJourneyProgress(
                currentUser = currentUser,
                userProgressSnapshot = userProgressSnapshot,
                phases = phases
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to build learning journey progress for userId=$userId", e)
            null
        }
    }

    fun observeCurrentUserBookings(): Flow<Map<String, Booking>> = callbackFlow {
        val userId = authSessionProvider.currentUser()?.uid
        if (userId == null) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        val registration = FirebaseFirestore.getInstance()
            .collection("bookings")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                launch {
                    val bookings = snapshot?.documents
                        .orEmpty()
                        .mapNotNull { it.toObject(Booking::class.java) }
                        .map { it.normalizeBooking() }
                        .associateBy { it.phaseId }

                    android.util.Log.d(
                        TAG,
                        "Observed booking sync: userId=$userId, states=${bookings.mapValues { it.value.status }}"
                    )
                    trySend(bookings)
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun updateLessonProgress(phaseId: String, lessonId: String, isCompleted: Boolean): Boolean {
        val userId = authSessionProvider.currentUser()?.uid ?: return false

        return try {
            val currentSnapshot = getPhaseLessonSnapshot(phaseId, userId)
            val lessonIndex = currentSnapshot.state.indexOfLesson(lessonId)
            if (lessonIndex == -1) {
                android.util.Log.w(TAG, "Blocked progress update for unknown lessonId=$lessonId in phaseId=$phaseId")
                return false
            }

            android.util.Log.d(
                TAG,
                "Progress update requested: phaseId=$phaseId, lessonId=$lessonId, lessonIndex=$lessonIndex, isCompleted=$isCompleted, rawCompleted=${currentSnapshot.state.rawCompletedLessonIds.sorted()}, canonicalCompleted=${currentSnapshot.state.completedLessonIds.sorted()}, unlocked=${currentSnapshot.state.unlockedLessonIds.sorted()}"
            )

            val completionChange = SequentialLessonProgressResolver.applyCompletionChange(
                state = currentSnapshot.state,
                lessonId = lessonId,
                isCompleted = isCompleted
            )

            val updatedCompletedIds = when (completionChange) {
                is SequentialLessonProgressResolver.CompletionChange.Success -> completionChange.completedLessonIds
                is SequentialLessonProgressResolver.CompletionChange.Rejected -> {
                    android.util.Log.w(
                        TAG,
                        "Blocked progress update for phaseId=$phaseId, lessonId=$lessonId, isCompleted=$isCompleted: ${completionChange.reason}"
                    )
                    return false
                }
            }

            val updatedState = SequentialLessonProgressResolver.resolve(
                lessons = currentSnapshot.state.orderedLessons,
                rawCompletedLessonIds = updatedCompletedIds
            )
            val updatedPhaseSnapshot = PhaseLessonSnapshot(
                phaseId = phaseId,
                state = updatedState,
                lessons = updatedState.toRenderedLessons(),
                phaseProgressPercent = ProgressCalculator.calculatePhaseProgress(
                    completedCount = updatedState.completedCount,
                    totalCount = updatedState.totalCount
                )
            )
            val userProgressSnapshot = buildUserProgressSnapshot(
                userId = userId,
                phaseOverrides = mapOf(phaseId to updatedPhaseSnapshot)
            )

            android.util.Log.d(
                TAG,
                "Progress update resolved: phaseId=$phaseId, lessonId=$lessonId, updatedCompleted=${updatedState.completedLessonIds.sorted()}, updatedUnlocked=${updatedState.unlockedLessonIds.sorted()}, phaseProgress=${updatedPhaseSnapshot.phaseProgressPercent}, overallProgress=${userProgressSnapshot.overallProgress}"
            )

            if (updatedPhaseSnapshot.phaseProgressPercent == 100) {
                android.util.Log.d(
                    TAG,
                    "Phase completion reached: userId=$userId, phaseId=$phaseId, completedLessons=${updatedPhaseSnapshot.completedCount}/${updatedPhaseSnapshot.totalCount}"
                )
            }

            val db = FirebaseFirestore.getInstance()
            db.runTransaction { transaction ->
                val userRef = db.collection("users").document(userId)
                val userSnap = transaction.get(userRef)
                if (!userSnap.exists()) {
                    throw Exception("User profile not found. Please complete registration.")
                }

                applyLessonProgressWrites(
                    transaction = transaction,
                    userId = userId,
                    phaseId = phaseId,
                    desiredCompletedLessonIds = updatedState.completedLessonIds,
                    rawCompletedLessonIds = currentSnapshot.state.rawCompletedLessonIds,
                    repairLessonIds = currentSnapshot.repairLessonIds
                )

                userProgressSnapshot.phaseSnapshots
                    .filter { it.phaseId != phaseId && it.repairLessonIds.isNotEmpty() }
                    .forEach { snapshot ->
                        applyLessonProgressWrites(
                            transaction = transaction,
                            userId = userId,
                            phaseId = snapshot.phaseId,
                            desiredCompletedLessonIds = snapshot.state.completedLessonIds,
                            rawCompletedLessonIds = snapshot.state.rawCompletedLessonIds,
                            repairLessonIds = snapshot.repairLessonIds
                        )
                    }

                android.util.Log.d(
                    TAG,
                    "Updating user progress document: userId=$userId, phaseProgress=${userProgressSnapshot.phaseProgress}, completedPhases=${userProgressSnapshot.completedPhases}, overallProgress=${userProgressSnapshot.overallProgress}"
                )
                transaction.update(userRef, buildUserProgressUpdateMap(userProgressSnapshot))
                true
            }.await()

            if (phaseId == PhaseCatalog.PHASE1 && updatedPhaseSnapshot.phaseProgressPercent == 100) {
                val user = appStore.getUser(userId)
                if (user?.referredBy != null) {
                    appStore.recordConversion(user.referredBy, userId)
                }
            }

            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Progress update failed for phaseId=$phaseId, lessonId=$lessonId", e)
            false
        }
    }

    suspend fun requestSeat(
        phase: Phase,
        whatsappNumber: String
    ): BookingRequestResult {
        val userId = authSessionProvider.currentUser()?.uid
            ?: return BookingRequestResult(BookingRequestOutcome.FAILED)
        val sanitizedWhatsappNumber = whatsappNumber.trim()

        android.util.Log.d(TAG, "Starting seat request: userId=$userId, phaseId=${phase.phaseId}, whatsapp=$sanitizedWhatsappNumber")

        if (sanitizedWhatsappNumber.isBlank()) {
            return BookingRequestResult(BookingRequestOutcome.INVALID_CONTACT_INFO)
        }

        return try {
            val bookingId = "${userId}_${phase.phaseId}"
            val currentUser = appStore.getUser(userId)

            if (currentUser?.unlockedPhases.orEmpty().contains(phase.phaseId)) {
                android.util.Log.d(
                    TAG,
                    "Phase request ignored because access already unlocked: userId=$userId, phaseId=${phase.phaseId}"
                )
                return BookingRequestResult(BookingRequestOutcome.ALREADY_APPROVED)
            }
            
            // Phase Prerequisite Validation
            val allPhases = PhaseCatalog.allPhases
            val currentIndex = allPhases.indexOfFirst { it.phaseId == phase.phaseId }
            var completedPhaseId: String? = null
            if (currentIndex > 0) {
                val previousPhaseId = allPhases[currentIndex - 1].phaseId
                completedPhaseId = previousPhaseId
                val previousPhaseSnapshot = getPhaseLessonSnapshot(previousPhaseId, userId)
                val previousPhaseCompleted = ProgressCalculator.shouldMarkPhaseAsCompleted(
                    completedCount = previousPhaseSnapshot.completedCount,
                    totalCount = previousPhaseSnapshot.totalCount
                )
                if (previousPhaseSnapshot.totalCount > 0 && !previousPhaseCompleted) {
                    android.util.Log.d(
                        TAG,
                        "Seat request blocked: phaseId=${phase.phaseId}, previousPhaseId=$previousPhaseId, completedLessons=${previousPhaseSnapshot.completedCount}/${previousPhaseSnapshot.totalCount}"
                    )
                    return BookingRequestResult(BookingRequestOutcome.PREREQUISITE_NOT_MET)
                }
            }

            val db = FirebaseFirestore.getInstance()
            db.runTransaction { transaction ->
                android.util.Log.d(TAG, "Inside transaction for bookingId=$bookingId")
                val userRef = db.collection("users").document(userId)
                
                // We still read the phase to check for "soft" availability, 
                // but we won't try to write to it to avoid permission errors.
                val phaseRef = db.collection("phases").document(phase.phaseId)
                val phaseSnap = transaction.get(phaseRef)
                
                val total = if (phaseSnap.exists()) phaseSnap.getLong("totalSeats")?.toInt() ?: phase.totalSeats else phase.totalSeats
                val booked = if (phaseSnap.exists()) phaseSnap.getLong("bookedSeats")?.toInt() ?: 0 else 0

                android.util.Log.d(TAG, "Phase stats (read-only check): total=$total, booked=$booked")

                if (booked >= total) {
                    return@runTransaction BookingRequestResult(BookingRequestOutcome.NO_SEATS_AVAILABLE)
                }

                val bookingRef = db.collection("bookings").document(bookingId)
                val existingBookingSnap = transaction.get(bookingRef)
                
                if (existingBookingSnap.exists()) {
                    val status = existingBookingSnap.getString("status")
                    android.util.Log.d(TAG, "Existing booking found with status=$status")
                    if (status == Booking.STATUS_PENDING || status == Booking.STATUS_REVIEWING) {
                        return@runTransaction BookingRequestResult(
                            outcome = BookingRequestOutcome.ALREADY_PENDING,
                            booking = existingBookingSnap.toObject(Booking::class.java)
                        )
                    }
                    if (status == Booking.STATUS_APPROVED || status == Booking.STATUS_BOOKED_LEGACY) {
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
                    completedPhaseId = completedPhaseId,
                    whatsappNumber = sanitizedWhatsappNumber,
                    createdAt = now,
                    expiresAt = now + Booking.EXPIRATION_WINDOW_MILLIS,
                    status = Booking.STATUS_PENDING,
                    lastUpdatedAt = now
                )

                // Atomic Updates: Create Booking + Update User Profile
                // Note: We REMOVED the phase update here because learners don't have write permission to 'phases'
                android.util.Log.d(TAG, "Setting booking and updating user profile")
                
                val userSnap = transaction.get(userRef)
                transaction.set(bookingRef, booking)
                
                if (userSnap.exists()) {
                    transaction.update(userRef, "whatsappNumber", sanitizedWhatsappNumber)
                } else {
                    val newUser = User(
                        id = userId,
                        email = authSessionProvider.currentUser()?.email ?: "",
                        whatsappNumber = sanitizedWhatsappNumber
                    )
                    transaction.set(userRef, newUser)
                }
                
                BookingRequestResult(
                    outcome = BookingRequestOutcome.REQUEST_CREATED,
                    booking = booking
                )
            }.await()

                .also { result ->
                    if (result.outcome == BookingRequestOutcome.REQUEST_CREATED) {
                        android.util.Log.d(
                            TAG,
                            "Booking request created: userId=$userId, phaseId=${phase.phaseId}, completedPhaseId=$completedPhaseId, expiresAt=${result.booking?.expiresAt}"
                        )
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Request seat failed for phaseId=${phase.phaseId}", e)
            BookingRequestResult(BookingRequestOutcome.FAILED, error = e.message)
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

    suspend fun getPhaseProgressionSnapshots(
        phases: List<Phase>,
        currentUser: User?,
        bookingStates: Map<String, Booking>,
        learningJourneyProgress: LearningJourneyProgress?
    ): List<PhaseProgressionSnapshot> {
        val completedPhaseIds = learningJourneyProgress?.completedPhaseIds.orEmpty()
        val phaseProgressById = learningJourneyProgress?.phaseProgressById.orEmpty()
        return phases.sortedBy { it.order }.map { phase ->
            PhaseProgressionResolver.resolve(
                phase = phase,
                allPhases = phases,
                user = currentUser,
                booking = bookingStates[phase.phaseId],
                phaseProgress = phaseProgressById[phase.phaseId],
                completedPhaseIds = completedPhaseIds
            )
        }.also { snapshots ->
            android.util.Log.d(
                TAG,
                "Resolved phase states: ${snapshots.joinToString { "${it.phase.phaseId}=${it.state}/${it.booking?.status ?: "none"}" }}"
            )
        }
    }

    suspend fun markBookingReviewing(bookingId: String): Boolean {
        val currentUserEmail = authSessionProvider.currentUser()?.email
        if (currentUserEmail != "sushen.biswas.aga@gmail.com" && currentUserEmail != "sushen.biswas.aga@googlemail.com") {
            android.util.Log.w(TAG, "Unauthorized attempt to mark booking as reviewing by $currentUserEmail")
            return false
        }
        if (bookingId.isBlank()) {
            android.util.Log.e(TAG, "Mark reviewing failed: bookingId is blank")
            return false
        }
        val db = FirebaseFirestore.getInstance()
        return try {
            db.runTransaction { transaction ->
                val bookingRef = db.collection("bookings").document(bookingId)
                val bookingSnap = transaction.get(bookingRef)
                
                if (!bookingSnap.exists()) {
                    throw Exception("Booking $bookingId does not exist")
                }
                
                val status = bookingSnap.getString("status") ?: throw Exception("Status is null for booking $bookingId")

                if (status == Booking.STATUS_REVIEWING) {
                    return@runTransaction true
                }
                if (status != Booking.STATUS_PENDING) {
                    throw Exception("Booking $bookingId has invalid status '$status' to start review")
                }

                val now = System.currentTimeMillis()
                val adminEmail = currentUserEmail.orEmpty()
                transaction.update(
                    bookingRef,
                    mapOf(
                        "status" to Booking.STATUS_REVIEWING,
                        "reviewedAt" to now,
                        "lastUpdatedAt" to now,
                        "reviewedByEmail" to adminEmail
                    )
                )
                android.util.Log.d(
                    TAG,
                    "Booking review started: bookingId=$bookingId, reviewedBy=$adminEmail"
                )
                true
            }.await()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to mark booking reviewing: bookingId=$bookingId. Error: ${e.message}", e)
            false
        }
    }

    suspend fun approveBooking(bookingId: String): Boolean {
        val currentUserEmail = authSessionProvider.currentUser()?.email
        if (currentUserEmail != "sushen.biswas.aga@gmail.com" && currentUserEmail != "sushen.biswas.aga@googlemail.com") {
            android.util.Log.w(TAG, "Unauthorized attempt to approve booking by $currentUserEmail")
            return false
        }
        if (bookingId.isBlank()) {
            android.util.Log.e(TAG, "Approve failed: bookingId is blank")
            return false
        }
        val db = FirebaseFirestore.getInstance()
        return try {
            db.runTransaction { transaction ->
                val bookingRef = db.collection("bookings").document(bookingId)
                val bookingSnap = transaction.get(bookingRef)
                
                if (!bookingSnap.exists()) {
                    throw Exception("Booking $bookingId does not exist")
                }
                
                val status = bookingSnap.getString("status")
                if (status != Booking.STATUS_PENDING && status != Booking.STATUS_REVIEWING) {
                    throw Exception("Booking $bookingId has invalid status '$status' for approval")
                }

                val phaseId = bookingSnap.getString("phaseId") ?: throw Exception("phaseId is null for booking $bookingId")
                val userId = bookingSnap.getString("userId") ?: throw Exception("userId is null for booking $bookingId")
                
                // All Reads must happen before any Writes in a transaction
                val phaseRef = db.collection("phases").document(phaseId)
                val phaseSnap = transaction.get(phaseRef)
                
                val userRef = db.collection("users").document(userId)
                val userSnap = transaction.get(userRef)

                // 1. Seat increment check
                if (phaseSnap.exists()) {
                    val booked = phaseSnap.getLong("bookedSeats") ?: 0L
                    val total = phaseSnap.getLong("totalSeats") ?: 0L
                    android.util.Log.d(TAG, "Approving phase $phaseId: booked=$booked, total=$total")
                    if (booked >= total && total > 0) {
                        throw Exception("Phase $phaseId is full (booked=$booked, total=$total)")
                    }
                    transaction.update(phaseRef, "bookedSeats", booked + 1)
                } else {
                    android.util.Log.d(TAG, "Phase $phaseId document missing, bootstrapping from catalog")
                    val catalogPhase = PhaseCatalog.findById(phaseId)
                    if (catalogPhase != null) {
                        transaction.set(phaseRef, catalogPhase.copy(bookedSeats = 1))
                    } else {
                        throw Exception("Phase $phaseId not found in catalog")
                    }
                }

                // 2. Booking status update
                val now = System.currentTimeMillis()
                val adminEmail = currentUserEmail.orEmpty()
                transaction.update(
                    bookingRef,
                    mapOf(
                        "status" to Booking.STATUS_APPROVED,
                        "reviewedAt" to now,
                        "approvedAt" to now,
                        "lastUpdatedAt" to now,
                        "reviewedByEmail" to adminEmail
                    )
                )

                // 3. User phase unlock
                if (userSnap.exists()) {
                    val unlocked = (userSnap.get("unlockedPhases") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    if (!unlocked.contains(phaseId)) {
                        transaction.update(userRef, "unlockedPhases", unlocked + phaseId)
                    }
                } else {
                    // Fallback for missing user profile
                    val newUser = User(
                        id = userId,
                        email = "", // We don't have user email in booking doc
                        unlockedPhases = listOf(phaseId)
                    )
                    transaction.set(userRef, newUser)
                }

                android.util.Log.d(
                    TAG,
                    "Booking approved and phase unlocked successfully: bookingId=$bookingId, userId=$userId, phaseId=$phaseId, reviewedBy=$adminEmail"
                )
                true
            }.await()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to approve booking: bookingId=$bookingId. Error: ${e.message}", e)
            false
        }
    }

    suspend fun rejectBooking(bookingId: String): Boolean {
        val currentUserEmail = authSessionProvider.currentUser()?.email
        if (currentUserEmail != "sushen.biswas.aga@gmail.com" && currentUserEmail != "sushen.biswas.aga@googlemail.com") {
            android.util.Log.w(TAG, "Unauthorized attempt to reject booking by $currentUserEmail")
            return false
        }
        if (bookingId.isBlank()) {
            android.util.Log.e(TAG, "Reject failed: bookingId is blank")
            return false
        }
        val db = FirebaseFirestore.getInstance()
        return try {
            db.runTransaction { transaction ->
                val bookingRef = db.collection("bookings").document(bookingId)
                val bookingSnap = transaction.get(bookingRef)
                
                if (!bookingSnap.exists()) {
                    throw Exception("Booking $bookingId does not exist")
                }
                
                val status = bookingSnap.getString("status")
                if (status != Booking.STATUS_PENDING && status != Booking.STATUS_REVIEWING) {
                    throw Exception("Booking $bookingId has invalid status '$status' for rejection")
                }

                val now = System.currentTimeMillis()
                val adminEmail = currentUserEmail.orEmpty()

                transaction.update(
                    bookingRef,
                    mapOf(
                        "status" to Booking.STATUS_REJECTED,
                        "reviewedAt" to now,
                        "lastUpdatedAt" to now,
                        "reviewedByEmail" to adminEmail
                    )
                )
                
                android.util.Log.d(
                    TAG,
                    "Booking rejected: bookingId=$bookingId, reviewedBy=$adminEmail"
                )
                true
            }.await()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to reject booking: bookingId=$bookingId. Error: ${e.message}", e)
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

                // Release seat if the booking was active (Pending, Reviewing or Approved)
                if (status == Booking.STATUS_APPROVED || status == Booking.STATUS_PENDING || status == Booking.STATUS_REVIEWING) {
                    val phaseId = bookingSnap.getString("phaseId") ?: return@runTransaction false
                    val userId = bookingSnap.getString("userId") ?: return@runTransaction false
                    
                    val phaseRef = db.collection("phases").document(phaseId)
                    val userRef = db.collection("users").document(userId)
                    
                    val phaseSnap = transaction.get(phaseRef)
                    val userSnap = transaction.get(userRef)
                    
                    val booked = phaseSnap.getLong("bookedSeats") ?: 0
                    
                    if (booked > 0) transaction.update(phaseRef, "bookedSeats", booked - 1)
                    
                    // Revoke classroom access if it was already approved
                    if (status == Booking.STATUS_APPROVED) {
                        val unlocked = (userSnap.get("unlockedPhases") as? List<*>)
                            ?.mapNotNull { it as? String } ?: emptyList()
                        
                        if (unlocked.contains(phaseId)) {
                            transaction.update(userRef, "unlockedPhases", unlocked - phaseId)
                            android.util.Log.d(
                                TAG,
                                "Approved phase access revoked: bookingId=$bookingId, userId=$userId, phaseId=$phaseId"
                            )
                        }
                    }
                }
                
                transaction.update(
                    bookingRef,
                    mapOf(
                        "status" to Booking.STATUS_CANCELLED,
                        "lastUpdatedAt" to System.currentTimeMillis()
                    )
                )
                android.util.Log.d(TAG, "Booking cancelled: bookingId=$bookingId, statusBefore=$status")
                true
            }.await()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to cancel booking: bookingId=$bookingId", e)
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

                // Status -> Expired
                // Note: We NO LONGER decrement seats here because they are only incremented on approval
                transaction.update(
                    bookingRef,
                    mapOf(
                        "status" to Booking.STATUS_EXPIRED,
                        "lastUpdatedAt" to System.currentTimeMillis()
                    )
                )
                
                android.util.Log.d(TAG, "Booking expired: bookingId=$bookingId")
                true
            }.await()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to expire booking: bookingId=$bookingId", e)
            false
        }
    }

    suspend fun canAccessPhase(phaseId: String): Boolean {
        val userId = authSessionProvider.currentUser()?.uid ?: return false
        return try {
            val user = appStore.getUser(userId) ?: return false

            // Rule: Phase access requires explicit presence in unlockedPhases via admin approval,
            // OR a valid approved booking as a fallback.
            val isUnlocked = user.unlockedPhases.contains(phaseId)

            if (!isUnlocked) {
                // If not in unlockedPhases, check for a valid approved booking as a fallback
                val bookingId = "${userId}_${phaseId}"
                val booking = appStore.getBooking(bookingId)
                if (booking?.status != Booking.STATUS_APPROVED) {
                    android.util.Log.d(TAG, "Access denied to phaseId=$phaseId because the phase is not unlocked and no approved booking found for userId=$userId")
                    return false
                }
            }

            // Rule 2: Sequential Completion Check
            val allPhases = PhaseCatalog.allPhases
            val currentIndex = allPhases.indexOfFirst { it.phaseId == phaseId }
            
            if (currentIndex > 0) {
                // Check all previous phases to ensure no gaps
                for (i in 0 until currentIndex) {
                    val prevId = allPhases[i].phaseId
                    val previousPhaseSnapshot = getPhaseLessonSnapshot(prevId, userId)
                    
                    // A phase blocks progress IF it has lessons AND it's not marked as completed
                      val previousPhaseCompleted = ProgressCalculator.shouldMarkPhaseAsCompleted(
                          completedCount = previousPhaseSnapshot.completedCount,
                          totalCount = previousPhaseSnapshot.totalCount
                      )
                      if (previousPhaseSnapshot.totalCount > 0 && !previousPhaseCompleted) {
                          android.util.Log.d(
                              TAG,
                              "Access denied to phaseId=$phaseId because previous phaseId=$prevId is not complete. CompletedLessons=${previousPhaseSnapshot.completedCount}/${previousPhaseSnapshot.totalCount}"
                          )
                        return false
                    }
                }
            }
            
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "canAccessPhase failed for phaseId=$phaseId", e)
            false
        }
    }

    suspend fun reconcileProgressState(): User? {
        val userId = authSessionProvider.currentUser()?.uid ?: return null
        val currentUser = appStore.getUser(userId) ?: return null

        return try {
            val userProgressSnapshot = buildUserProgressSnapshot(userId)
            val hasLessonRepairs = userProgressSnapshot.phaseSnapshots.any { it.repairLessonIds.isNotEmpty() }
            val hasUserRepairs = shouldRepairUserProgress(currentUser, userProgressSnapshot)

            if (hasLessonRepairs || hasUserRepairs) {
                val db = FirebaseFirestore.getInstance()
                db.runTransaction { transaction ->
                    val userRef = db.collection("users").document(userId)
                    val userSnap = transaction.get(userRef)
                    if (!userSnap.exists()) {
                        throw Exception("User profile not found. Please complete registration.")
                    }

                    userProgressSnapshot.phaseSnapshots
                        .filter { it.repairLessonIds.isNotEmpty() }
                        .forEach { snapshot ->
                            applyLessonProgressWrites(
                                transaction = transaction,
                                userId = userId,
                                phaseId = snapshot.phaseId,
                                desiredCompletedLessonIds = snapshot.state.completedLessonIds,
                                rawCompletedLessonIds = snapshot.state.rawCompletedLessonIds,
                                repairLessonIds = snapshot.repairLessonIds
                            )
                        }

                    android.util.Log.d(
                        TAG,
                        "Reconciling user progress: userId=$userId, hasLessonRepairs=$hasLessonRepairs, hasUserRepairs=$hasUserRepairs, phaseProgress=${userProgressSnapshot.phaseProgress}, completedPhases=${userProgressSnapshot.completedPhases}, overallProgress=${userProgressSnapshot.overallProgress}"
                    )
                    transaction.update(userRef, buildUserProgressUpdateMap(userProgressSnapshot))
                    true
                }.await()
            }

            currentUser.copy(
                phaseProgress = userProgressSnapshot.phaseProgress,
                completedPhases = userProgressSnapshot.completedPhases,
                progress = userProgressSnapshot.overallProgress,
                unlockedFeatures = userProgressSnapshot.unlockedFeatures
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to reconcile user progress for userId=$userId", e)
            currentUser
        }
    }

    private suspend fun getPhaseLessonSnapshot(
        phaseId: String,
        userId: String? = authSessionProvider.currentUser()?.uid
    ): PhaseLessonSnapshot {
        val baseLessons = getBaseLessonsForPhase(phaseId)
        val rawCompletedLessonIds = if (userId == null) {
            emptyList()
        } else {
            appStore.getCompletedLessonIds(userId, phaseId)
        }
        val state = SequentialLessonProgressResolver.resolve(baseLessons, rawCompletedLessonIds)
        val renderedLessons = state.toRenderedLessons()
        val phaseProgressPercent = ProgressCalculator.calculatePhaseProgress(
            completedCount = state.completedCount,
            totalCount = state.totalCount
        )
        val snapshot = PhaseLessonSnapshot(
            phaseId = phaseId,
            state = state,
            lessons = renderedLessons,
            phaseProgressPercent = phaseProgressPercent
        )
        logPhaseSnapshot(snapshot)
        return snapshot
    }

    private fun getBaseLessonsForPhase(phaseId: String): List<Lesson> {
        return when (phaseId) {
            PhaseCatalog.PHASE1 -> com.shaplachottor.lab.data.Phase1LessonProvider.getLessons()
            PhaseCatalog.PHASE2 -> com.shaplachottor.lab.data.Phase2LessonProvider.getLessons()
            PhaseCatalog.PHASE3 -> com.shaplachottor.lab.data.Phase3LessonProvider.getLessons()
            PhaseCatalog.PHASE4 -> com.shaplachottor.lab.data.Phase4LessonProvider.getLessons()
            PhaseCatalog.PHASE5 -> com.shaplachottor.lab.data.Phase5LessonProvider.getLessons()
            PhaseCatalog.PHASE6 -> com.shaplachottor.lab.data.Phase6LessonProvider.getLessons()
            else -> emptyList()
        }.sortedBy { it.order }
    }

    private suspend fun buildUserProgressSnapshot(
        userId: String,
        phaseOverrides: Map<String, PhaseLessonSnapshot> = emptyMap()
    ): UserProgressSnapshot {
        val phaseSnapshots = coroutineScope {
            PhaseCatalog.phaseIds.map { phaseId ->
                async {
                    phaseOverrides[phaseId] ?: getPhaseLessonSnapshot(phaseId, userId)
                }
            }.awaitAll()
        }

        val phaseProgress = linkedMapOf<String, Int>().apply {
            phaseSnapshots.forEach { snapshot ->
                put(snapshot.phaseId, snapshot.phaseProgressPercent)
            }
        }
        val completedPhases = phaseSnapshots
            .filter {
                ProgressCalculator.shouldMarkPhaseAsCompleted(
                    completedCount = it.completedCount,
                    totalCount = it.totalCount
                )
            }
            .map { it.phaseId }
        val overallProgress = ProgressCalculator.calculateOverallProgress(
            phaseSnapshots.map { snapshot ->
                ProgressCalculator.PhaseStats(
                    phaseId = snapshot.phaseId,
                    completedCount = snapshot.completedCount,
                    totalCount = snapshot.totalCount
                )
            }
        )
        val unlockedFeatures = ProgressCalculator.calculateUnlockedFeatures(overallProgress)

        return UserProgressSnapshot(
            phaseSnapshots = phaseSnapshots,
            phaseProgress = phaseProgress,
            completedPhases = completedPhases,
            overallProgress = overallProgress,
            unlockedFeatures = unlockedFeatures
        )
    }

    private fun buildLearningJourneyProgress(
        currentUser: User,
        userProgressSnapshot: UserProgressSnapshot,
        phases: List<Phase>
    ): LearningJourneyProgress {
        val phaseProgressById = userProgressSnapshot.phaseSnapshots
            .associate { snapshot -> snapshot.phaseId to snapshot.toPhaseLearningProgress() }
        val currentPhaseProgress = phases
            .firstOrNull { phase ->
                val isAccessible = isPhaseAccessible(phase, currentUser)
                val phaseProgress = phaseProgressById[phase.phaseId]
                isAccessible && phaseProgress?.isCompleted == false
            }
            ?.let { activePhase ->
                CurrentPhaseProgress(
                    phase = activePhase,
                    progress = requireNotNull(phaseProgressById[activePhase.phaseId])
                )
            }

        val overallLearningProgress = OverallLearningProgress(
            percent = userProgressSnapshot.overallProgress,
            completedPhases = userProgressSnapshot.completedPhases.size,
            totalPhases = userProgressSnapshot.phaseSnapshots.size
        )

        return LearningJourneyProgress(
            currentPhaseProgress = currentPhaseProgress,
            overallLearningProgress = overallLearningProgress,
            phaseProgressById = phaseProgressById,
            completedPhaseIds = userProgressSnapshot.completedPhases,
            unlockedPhaseIds = currentUser.unlockedPhases
        ).also { journeyProgress ->
            logLearningJourneyProgress(journeyProgress)
        }
    }

    private fun isPhaseAccessible(phase: Phase, user: User): Boolean {
        // Allow access if unlockedPhases contains the ID.
        // The booking status fallback is handled in canAccessPhase(String) for lesson-level access.
        return user.unlockedPhases.contains(phase.phaseId)
    }

    private fun applyLessonProgressWrites(
        transaction: com.google.firebase.firestore.Transaction,
        userId: String,
        phaseId: String,
        desiredCompletedLessonIds: Set<String>,
        rawCompletedLessonIds: Set<String>,
        repairLessonIds: Set<String>
    ) {
        val lessonIdsToWrite = linkedSetOf<String>().apply {
            addAll(rawCompletedLessonIds)
            addAll(desiredCompletedLessonIds)
            addAll(repairLessonIds)
        }

        lessonIdsToWrite.forEach { persistedLessonId ->
            val shouldBeCompleted = desiredCompletedLessonIds.contains(persistedLessonId)
            val lessonRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("progress")
                .document(phaseId)
                .collection("lessons")
                .document(persistedLessonId)

            android.util.Log.d(
                TAG,
                "Persisting lesson progress: path=${lessonRef.path}, phaseId=$phaseId, lessonId=$persistedLessonId, isCompleted=$shouldBeCompleted"
            )
            transaction.set(lessonRef, mapOf("isCompleted" to shouldBeCompleted))
        }
    }

    private fun buildUserProgressUpdateMap(userProgressSnapshot: UserProgressSnapshot): Map<String, Any> {
        return mapOf(
            "phaseProgress" to userProgressSnapshot.phaseProgress,
            "completedPhases" to userProgressSnapshot.completedPhases,
            "progress" to userProgressSnapshot.overallProgress,
            "unlockedFeatures" to userProgressSnapshot.unlockedFeatures
        )
    }

    private fun shouldRepairUserProgress(
        user: User,
        userProgressSnapshot: UserProgressSnapshot
    ): Boolean {
        return user.phaseProgress != userProgressSnapshot.phaseProgress ||
            user.completedPhases != userProgressSnapshot.completedPhases ||
            user.progress != userProgressSnapshot.overallProgress ||
            user.unlockedFeatures != userProgressSnapshot.unlockedFeatures
    }

    private fun logPhaseSnapshot(snapshot: PhaseLessonSnapshot) {
        val lessonIndexes = snapshot.state.orderedLessons
            .mapIndexed { index, lesson -> "${lesson.id}@index=$index/order=${lesson.order}" }
            .joinToString()
        android.util.Log.d(
            TAG,
            "Phase snapshot: phaseId=${snapshot.phaseId}, lessonIndexes=[$lessonIndexes], rawCompleted=${snapshot.state.rawCompletedLessonIds.sorted()}, canonicalCompleted=${snapshot.state.completedLessonIds.sorted()}, invalidCompleted=${snapshot.state.invalidCompletedLessonIds.sorted()}, unknownCompleted=${snapshot.state.unknownCompletedLessonIds.sorted()}, unlocked=${snapshot.state.unlockedLessonIds.sorted()}, progress=${snapshot.phaseProgressPercent}"
        )
    }

    private fun logLearningJourneyProgress(journeyProgress: LearningJourneyProgress) {
        val activePhase = journeyProgress.currentPhaseProgress
        val currentPhaseSummary = if (activePhase == null) {
            "none"
        } else {
            "${activePhase.phase.phaseId}:${activePhase.progress.completedLessons}/${activePhase.progress.totalLessons} (${activePhase.progress.percent}%)"
        }
        val phaseSummaries = journeyProgress.phaseProgressById.values
            .sortedBy { PhaseCatalog.phaseIds.indexOf(it.phaseId) }
            .joinToString { progress ->
                "${progress.phaseId}=${progress.completedLessons}/${progress.totalLessons} (${progress.percent}%)"
            }

        android.util.Log.d(
            TAG,
            "Learning journey progress: activePhase=$currentPhaseSummary, completedPhases=${journeyProgress.completedPhaseIds}, unlockedPhases=${journeyProgress.unlockedPhaseIds}, overallPercent=${journeyProgress.overallLearningProgress.percent}, totalPhases=${journeyProgress.overallLearningProgress.totalPhases}, phaseSummaries=[$phaseSummaries]"
        )
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
            android.util.Log.d(TAG, "Normalized booking to expired: bookingId=$bookingId, phaseId=$phaseId")
            return copy(
                status = Booking.STATUS_EXPIRED,
                expiresAt = 0L,
                createdAt = normalizedCreatedAt,
                lastUpdatedAt = now
            )
        }

        return copy(
            status = normalizedStatus,
            createdAt = normalizedCreatedAt,
            expiresAt = normalizedExpiresAt
        )
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
