# Firestore Database Structure

## users/{userId}
- `id`: string
- `name`: string
- `email`: string
- `role`: string, default `student`
- `progress`: number from `0` to `100`
- `phaseProgress`: map of phase progress keyed by `phaseId`
- `unlockedFeatures`: map
- `unlockedFeatures.tradingBot`: boolean
- `unlockedFeatures.investment`: boolean
- `unlockedFeatures.affiliate`: boolean
- `unlockedPhases`: array of phase IDs
- `completedPhases`: array of phase IDs
- `createdAt`: timestamp stored as epoch millis

Default user state:
- `progress = 0`
- `phaseProgress = {}`
- `unlockedPhases = []`
- `completedPhases = []`

## phases/{phaseId}
Required fields:
- `phaseId`: string
- `title`: string
- `description`: string
- `level`: string, one of `Beginner`, `Intermediate`, `Advanced`
- `order`: number
- `totalSeats`: number
- `bookedSeats`: number

Canonical phase documents:

### phases/phase1
- `phaseId`: `phase1`
- `title`: `Foundations`
- `description`: `Learn core programming fundamentals required for all future phases. Focus on building basic coding ability and logical thinking.`
- `level`: `Beginner`
- `order`: `1`
- `totalSeats`: `100`
- `bookedSeats`: `0`

### phases/phase2
- `phaseId`: `phase2`
- `title`: `Data Analysis`
- `description`: `Learn how to work with data using standard Python libraries. Focus on handling structured datasets and time-series data.`
- `level`: `Beginner`
- `order`: `2`
- `totalSeats`: `100`
- `bookedSeats`: `0`

### phases/phase3
- `phaseId`: `phase3`
- `title`: `Object-Oriented Programming`
- `description`: `Learn how to structure code using classes and reusable components. Focus on writing maintainable and scalable code.`
- `level`: `Intermediate`
- `order`: `3`
- `totalSeats`: `100`
- `bookedSeats`: `0`

### phases/phase4
- `phaseId`: `phase4`
- `title`: `System Design`
- `description`: `Learn how to design modular systems and organize large codebases. Focus on architecture, patterns, and scalability.`
- `level`: `Intermediate`
- `order`: `4`
- `totalSeats`: `100`
- `bookedSeats`: `0`

### phases/phase5
- `phaseId`: `phase5`
- `title`: `Simulation & Data Systems`
- `description`: `Learn how to build real-world systems using APIs and streaming data. Focus on simulations and data-driven workflows.`
- `level`: `Advanced`
- `order`: `5`
- `totalSeats`: `100`
- `bookedSeats`: `0`

### phases/phase6
- `phaseId`: `phase6`
- `title`: `Production Engineering`
- `description`: `Learn how to deploy, debug, and maintain production systems. Focus on testing, logging, and performance optimization.`
- `level`: `Advanced`
- `order`: `6`
- `totalSeats`: `100`
- `bookedSeats`: `0`

## bookings/{bookingId}
- `bookingId`: string, format `${userId}_${phaseId}`
- `userId`: string
- `phaseId`: string
- `phoneNumber`: string
- `whatsappNumber`: string
- `createdAt`: timestamp stored as epoch millis
- `expiresAt`: timestamp stored as epoch millis
- `status`: string, one of `pending`, `approved`, `expired`

Behavior:
- All phases start locked.
- Booking creates a pending request and does not deduct a seat immediately.
- Admin approval increments `phases/{phaseId}.bookedSeats`.
- Admin approval adds the `phaseId` to `users/{userId}.unlockedPhases`.
- Expired or rejected requests keep the phase locked and do not deduct a seat.
- Locked phases show the booking CTA.
- Only approved phases open learning content.
- Backend-managed notification metadata may also be merged into the booking document after the email is sent.

## Progress and feature unlocks
- Overall progress is the average across all 6 phases.
- `>= 30%` unlocks Bot Setup.
- `>= 60%` unlocks Investment Features.
- `100%` unlocks the Affiliate System.
