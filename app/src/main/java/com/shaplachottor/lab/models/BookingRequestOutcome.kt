package com.shaplachottor.lab.models

enum class BookingRequestOutcome {
    REQUEST_CREATED,
    ALREADY_PENDING,
    ALREADY_APPROVED,
    NO_SEATS_AVAILABLE,
    INVALID_CONTACT_INFO,
    FAILED
}
