# Firestore Database Structure

## users/{userId}
- `id`: string
- `name`: string
- `email`: string
- `role`: string, default `student`
- `progress`: number from `0` to `100`
- `phaseProgress`: map of phase progress keyed by `phaseId`
- `unlockedFeatures`: object (`AdvancedFeatures`)
- `unlockedFeatures.tradingBot`: boolean
- `unlockedFeatures.investment`: boolean
- `unlockedFeatures.affiliate`: boolean
- `unlockedPhases`: array of phase IDs
- `completedPhases`: array of phase IDs
- `referralCode`: string (6-char unique code)
- `referredBy`: string (uid of referrer)
- `createdAt`: timestamp stored as epoch millis

Default user state:
- `progress = 0`
- `phaseProgress = {}`
- `unlockedPhases = []`
- `completedPhases = []`

## phases/{phaseId} (Cohorts)
Required fields:
- `phaseId`: string
- `title`: string
- `description`: string
- `level`: string, one of `Beginner`, `Intermediate`, `Advanced`
- `type`: string, `free` or `premium`
- `price`: number (for premium)
- `currency`: string (e.g., `USD`)
- `startDate`: number (epoch millis)
- `order`: number
- `totalSeats`: number
- `bookedSeats`: number
- `isVisible`: boolean

Canonical phase documents:

### phases/phase1 (Foundations - Free)
- `type`: `free`
- `order`: 1
- `totalSeats`: 100

### phases/phase2-6 (Premium Cohorts)
- `type`: `premium`
- `price`: Tiered ($49.99 to $249.99)
- `totalSeats`: Decreasing per tier (50 down to 10)

## bookings/{bookingId}
- `bookingId`: string, format `${userId}_${phaseId}`
- `userId`: string
- `phaseId`: string
- `completedPhaseId`: string, the prerequisite phase the learner completed before requesting this one
- `whatsappNumber`: string
- `createdAt`: timestamp (epoch millis)
- `expiresAt`: timestamp (epoch millis)
- `status`: string, one of `pending`, `reviewing`, `approved`, `expired`, `rejected`, `cancelled`
- `reviewedAt`: timestamp (epoch millis)
- `approvedAt`: timestamp (epoch millis)
- `lastUpdatedAt`: timestamp (epoch millis)
- `reviewedByEmail`: string

Behavior:
- All phases after Phase 1 require a teacher-reviewed booking request.
- Completing a phase does not unlock the next one. It only makes the next phase requestable.
- Booking creates a `pending` request and reserves a seat immediately (`bookedSeats` incremented).
- Admin can move a request to `reviewing` while checking practical readiness and external assignments.
- If request is `expired`, `rejected`, or `cancelled`, the seat is released (`bookedSeats` decremented).
- Admin approval adds `phaseId` to `users/{userId}.unlockedPhases`.
- For `premium` phases, Admin must manually verify payment before approval in the Admin Panel.

## referralEvents/{eventId}
- `eventId`: `${referrerId}_${referredUserId}`
- `referrerId`: string
- `referredUserId`: string
- `status`: `joined` | `converted`
- `timestamp`: epoch millis

## affiliateStats/{userId}
- `totalInvites`: number
- `conversions`: number (incremented when referred user completes Phase 1)

## Progress and feature unlocks
- Overall progress is calculated based on completed lessons across all phases.
- Feature unlocks are tied to overall progress:
    - `>= 30%` -> Trading Bot
    - `>= 60%` -> Investment
    - `100%` -> Affiliate (Can view stats and share code)
