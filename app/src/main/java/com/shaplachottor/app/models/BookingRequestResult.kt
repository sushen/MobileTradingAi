package com.shaplachottor.app.models

data class BookingRequestResult(
    val outcome: BookingRequestOutcome,
    val booking: Booking? = null
)
