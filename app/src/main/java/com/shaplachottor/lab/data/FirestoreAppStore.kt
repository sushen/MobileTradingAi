package com.shaplachottor.lab.data

import com.google.firebase.firestore.FirebaseFirestore
import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

class FirestoreAppStore : AppStore {
    companion object {
        private const val TAG = "FirestoreAppStore"
    }

    private val db = FirebaseFirestore.getInstance().apply {
        val settings = com.google.firebase.firestore.firestoreSettings {
            setLocalCacheSettings(com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                .setSizeBytes(100 * 1024 * 1024) // 100MB
                .build())
        }
        firestoreSettings = settings
    }

    override suspend fun getPhaseCount(): Int {
        return try {
            val snapshot = db.collection("phases").get(com.google.firebase.firestore.Source.CACHE).await()
            if (snapshot.isEmpty) {
                db.collection("phases").get(com.google.firebase.firestore.Source.SERVER).await().size()
            } else {
                snapshot.size()
            }
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun setPhase(phase: Phase) {
        db.collection("phases").document(phase.phaseId).set(phase).await()
    }

    override suspend fun getPhases(): List<Phase> {
        return try {
            val snapshot = db.collection("phases").get(com.google.firebase.firestore.Source.CACHE).await()
            if (snapshot.isEmpty) {
                db.collection("phases").get(com.google.firebase.firestore.Source.SERVER).await().toObjects(Phase::class.java)
            } else {
                snapshot.toObjects(Phase::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPhase(phaseId: String): Phase? {
        return try {
            val doc = db.collection("phases").document(phaseId).get(com.google.firebase.firestore.Source.CACHE).await()
            if (!doc.exists()) {
                db.collection("phases").document(phaseId).get(com.google.firebase.firestore.Source.SERVER).await().toObject(Phase::class.java)
            } else {
                doc.toObject(Phase::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUser(userId: String): User? {
        return try {
            val doc = db.collection("users").document(userId).get(com.google.firebase.firestore.Source.CACHE).await()
            if (!doc.exists()) {
                db.collection("users").document(userId).get(com.google.firebase.firestore.Source.SERVER).await().toObject(User::class.java)
            } else {
                doc.toObject(User::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getUserStream(userId: String): kotlinx.coroutines.flow.Flow<User?> = kotlinx.coroutines.flow.callbackFlow {
        val registration = db.collection("users").document(userId)
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(User::class.java))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun setUser(user: User) {
        db.collection("users").document(user.id).set(user).await()
    }

    override suspend fun getBooking(bookingId: String): Booking? {
        return try {
            val doc = db.collection("bookings").document(bookingId).get(com.google.firebase.firestore.Source.CACHE).await()
            if (!doc.exists()) {
                db.collection("bookings").document(bookingId).get(com.google.firebase.firestore.Source.SERVER).await().toObject(Booking::class.java)
            } else {
                doc.toObject(Booking::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setBooking(booking: Booking) {
        db.collection("bookings").document(booking.bookingId).set(booking).await()
    }

    override suspend fun getPendingBookings(): List<Booking> {
        return try {
            val snapshot = db.collection("bookings")
                .whereEqualTo("status", Booking.STATUS_PENDING)
                .get(com.google.firebase.firestore.Source.CACHE).await()
            if (snapshot.isEmpty) {
                 db.collection("bookings")
                    .whereEqualTo("status", Booking.STATUS_PENDING)
                    .get(com.google.firebase.firestore.Source.SERVER).await().toObjects(Booking::class.java)
            } else {
                snapshot.toObjects(Booking::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getAllBookings(): List<Booking> {
        return try {
            val snapshot = db.collection("bookings").get(com.google.firebase.firestore.Source.CACHE).await()
             if (snapshot.isEmpty) {
                db.collection("bookings").get(com.google.firebase.firestore.Source.SERVER).await().toObjects(Booking::class.java)
            } else {
                snapshot.toObjects(Booking::class.java)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun updateLessonCompletion(
        userId: String,
        phaseId: String,
        lessonId: String,
        isCompleted: Boolean
    ) {
        val completionData = mapOf("isCompleted" to isCompleted)
        val lessonRef = db.collection("users")
            .document(userId)
            .collection("progress")
            .document(phaseId)
            .collection("lessons")
            .document(lessonId)
        android.util.Log.d(
            TAG,
            "Writing lesson completion: path=${lessonRef.path}, userId=$userId, phaseId=$phaseId, lessonId=$lessonId, isCompleted=$isCompleted"
        )
        lessonRef.set(completionData).await()
    }

    override suspend fun getCompletedLessonIds(userId: String, phaseId: String): List<String> {
        return try {
            // Use default Source (Server with Cache fallback) to ensure consistency.
            // Forcing Source.CACHE was causing partial results when the local cache was incomplete,
            // leading to "jumping" progression states where lessons appeared uncompleted.
            val snapshot = db.collection("users")
                .document(userId)
                .collection("progress")
                .document(phaseId)
                .collection("lessons")
                .whereEqualTo("isCompleted", true)
                .get() // Removed forced Source.CACHE
                .await()

            val completedLessonIds = snapshot.documents.map { it.id }
            android.util.Log.d(
                TAG,
                "Fetched completed lesson ids: userId=$userId, phaseId=$phaseId, path=users/$userId/progress/$phaseId/lessons, completedLessonIds=$completedLessonIds"
            )
            completedLessonIds
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching completed lessons for phaseId=$phaseId", e)
            emptyList()
        }
    }

    override suspend fun logReferralEvent(referrerId: String, referredUserId: String) {
        try {
            val eventId = "${referrerId}_${referredUserId}"
            val event = hashMapOf(
                "referrerId" to referrerId,
                "referredUserId" to referredUserId,
                "status" to "joined",
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("referralEvents").document(eventId).set(event).await()
            
            // Increment total invites count
            db.collection("affiliateStats").document(referrerId)
                .update("totalInvites", com.google.firebase.firestore.FieldValue.increment(1))
                .await()
        } catch (e: Exception) {
            // Document might not exist, create it
            val stats = hashMapOf("totalInvites" to 1, "conversions" to 0)
            db.collection("affiliateStats").document(referrerId).set(stats)
        }
    }

    override suspend fun recordConversion(referrerId: String, referredUserId: String) {
        try {
            val eventId = "${referrerId}_${referredUserId}"
            val eventRef = db.collection("referralEvents").document(eventId)
            val eventSnap = eventRef.get().await()
            
            if (eventSnap.exists() && eventSnap.getString("status") != "converted") {
                eventRef.update("status", "converted").await()
                
                db.collection("affiliateStats").document(referrerId)
                    .update("conversions", com.google.firebase.firestore.FieldValue.increment(1))
                    .await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreAppStore", "Error recording conversion for $referrerId: ${e.message}", e)
        }
    }

    override suspend fun getAffiliateStats(userId: String): Map<String, Any>? {
        return try {
            db.collection("affiliateStats").document(userId).get().await().data
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUserByReferralCode(code: String): User? {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("referralCode", code.uppercase())
                .limit(1)
                .get()
                .await()
            snapshot.toObjects(User::class.java).firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getReferralEvents(referrerId: String): List<com.shaplachottor.lab.models.ReferralEvent> {
        return try {
            val snapshot = db.collection("referralEvents")
                .whereEqualTo("referrerId", referrerId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.toObjects(com.shaplachottor.lab.models.ReferralEvent::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
