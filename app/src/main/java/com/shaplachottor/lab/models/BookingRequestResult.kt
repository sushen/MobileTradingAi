package com.shaplachottor.lab.models

data class BookingRequestResult(
    val outcome: BookingRequestOutcome,
    val booking: Booking? = null
)
