package com.shaplachottor.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.shaplachottor.app.models.Booking
import com.shaplachottor.app.models.Phase
import com.shaplachottor.app.models.User
import kotlinx.coroutines.tasks.await

class FirestoreAppStore : AppStore {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun getPhaseCount(): Int {
        return try {
            val snapshot = db.collection("phases").get().await()
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun setPhase(phase: Phase) {
        db.collection("phases").document(phase.phaseId).set(phase).await()
    }

    override suspend fun getPhases(): List<Phase> {
        return try {
            val snapshot = db.collection("phases").get().await()
            snapshot.toObjects(Phase::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPhase(phaseId: String): Phase? {
        return try {
            val doc = db.collection("phases").document(phaseId).get().await()
            doc.toObject(Phase::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUser(userId: String): User? {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setUser(user: User) {
        db.collection("users").document(user.id).set(user).await()
    }

    override suspend fun getBooking(bookingId: String): Booking? {
        return try {
            val doc = db.collection("bookings").document(bookingId).get().await()
            doc.toObject(Booking::class.java)
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
                .get().await()
            snapshot.toObjects(Booking::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getAllBookings(): List<Booking> {
        return try {
            val snapshot = db.collection("bookings").get().await()
            snapshot.toObjects(Booking::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
