package com.shaplachottor.lab.data

import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User

interface AppStore {
    suspend fun getPhaseCount(): Int
    suspend fun setPhase(phase: Phase)
    suspend fun getPhases(): List<Phase>
    suspend fun getPhase(phaseId: String): Phase?
    suspend fun getUser(userId: String): User?
    suspend fun setUser(user: User)
    suspend fun getBooking(bookingId: String): Booking?
    suspend fun setBooking(booking: Booking)
    suspend fun getPendingBookings(): List<Booking>
    suspend fun getAllBookings(): List<Booking>
}
