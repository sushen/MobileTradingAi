# AI Context

## 1. Project Overview

- `MobileTradingAi` is an Android app branded as `ShaplaChottor`.
- The implemented product is an education-first access shell for AI/trading learning, not a mobile trading executor.
- The core working flow is:
  - sign in with Google
  - create or sync the Firestore user document
  - seed a fixed six-phase curriculum
  - request access to a phase by submitting phone and WhatsApp numbers
  - wait for manual admin approval
  - enter the classroom only after approval
  - track progress and unlock advanced sections as learning progress increases
- The repo also contains a Firebase Cloud Function that emails the master admin when a booking request becomes a fresh `pending` request.
- Investment, affiliate, install, and education screens exist, but they are mostly informational or placeholder UX compared with the phase/classroom flow.

## 2. Tech Stack

- Languages:
  - Kotlin for the Android app
  - JavaScript for Firebase Cloud Functions
- Android stack:
  - Android SDK
  - XML layouts with ViewBinding
  - Material 3
  - Navigation Component with Safe Args
  - `ViewModel` + `LiveData`
- Async:
  - Kotlin coroutines
  - `kotlinx-coroutines-play-services`
- Firebase:
  - Firebase Authentication
  - Cloud Firestore
  - Firebase Storage
  - Firebase Analytics
  - Firebase Cloud Functions v2 Firestore trigger
- Key libraries:
  - Google Play Services Auth (`GoogleSignIn`)
  - `firebase-admin`
  - `firebase-functions`
  - `nodemailer`
  - Glide
  - JUnit4
  - MockK
  - Espresso
  - Fragment testing
- Build/runtime versions:
  - AGP `8.5.1`
  - Kotlin plugin `2.1.0`
  - `compileSdk 34`
  - `minSdk 24`
  - Java/Kotlin target `17`
  - Functions runtime `nodejs20`

## 3. Architecture

- Pattern:
  - MVVM-style Android app with repositories, but not strict clean architecture.
- Core data access pattern:
  - `AppGraph` acts as a small service locator.
  - `AuthSessionProvider` abstracts the signed-in Firebase user.
  - `AppStore` abstracts Firestore reads/writes for `users`, `phases`, and `bookings`.
  - `UserRepository` and `PhaseRepository` use those abstractions.
- Actual architecture is mixed:
  - `PhaseRepository` and `UserRepository` use abstractions and are testable.
  - `HomeViewModel`, `MyLearningViewModel`, `AdvancedViewModel`, and part of `ClassroomViewModel` still talk directly to Firebase SDKs.
- Main app flow:
  - `TradingAIApplication` initializes Firebase.
  - `SplashActivity` checks auth, syncs the user doc, seeds phases, then opens `MainActivity`.
  - `LoginActivity` performs Google sign-in, then syncs user data and seeds phases.
  - `MainActivity` hosts a `NavHostFragment` and bottom navigation.
  - `PhasesFragment` + `PhaseViewModel` + `PhaseRepository` drive phase listing and booking requests.
  - `ClassroomFragment` + `ClassroomViewModel` + `PhaseRepository` drive classroom access and progress updates.
- Backend flow:
  - `functions/index.js` registers `notifyAdminOfBookingRequest`.
  - The function listens to `bookings/{bookingId}` writes.
  - It sends an SMTP email to the admin when a booking becomes a new `pending` request.
  - It writes `adminNotification` metadata back into the booking document after successful delivery.

## 4. Implemented Features

### Google Sign-In and startup bootstrap

- Completed:
  - Google sign-in is implemented in `LoginActivity`.
  - Startup auth gating is implemented in `SplashActivity`.
  - Signed-in users have their Firestore profile synced and canonical phases seeded.
- Partial:
  - Login uses deprecated `GoogleSignIn` APIs.
  - If Firestore profile sync fails after Firebase auth succeeds, the user can remain authenticated but stay on the login screen.
  - `SplashActivity` does not stop navigation if user sync or phase seeding fails.

### Firestore user profile sync

- Completed:
  - `UserRepository.ensureCurrentUserDocument()` merges `id`, `name`, and `email` into `users/{uid}`.
  - `UserRepository.fetchCurrentUser()` exposes the current user via `LiveData`.
- Partial:
  - Only minimal identity fields are actively synced.
  - Profile screen UI does not render this data.

### Six-phase curriculum

- Completed:
  - `PhaseCatalog` defines six canonical phases.
  - `PhaseRepository.ensurePhasesSeeded()` inserts them when `phases` is empty.
  - `PhasesFragment` shows phases in `Beginner`, `Intermediate`, and `Advanced` tabs.
- Partial:
  - The repo only seeds when the collection is empty; it does not repair partial or corrupted catalogs.
  - Optional phase fields such as `focus`, `outcome`, and `identityShift` exist in the model/UI but the seeded catalog does not populate them.

### Booking request workflow

- Completed:
  - Locked phases open a booking dialog that requires phone and WhatsApp numbers.
  - `PhaseRepository.requestSeat()` writes `bookings/{userId_phaseId}` with `status = pending`.
  - Bookings include `createdAt` and `expiresAt` with a 15-minute window.
  - Pending and expired states are surfaced in the phase list UI.
  - Classroom access checks only unlocked phases.
- Partial:
  - There is no in-app admin approval UI.
  - Approval and expiry are repository/backend concepts, not a surfaced admin workflow inside the Android app.
  - Expiry is normalized lazily when bookings are loaded/requested/approved; there is no scheduled server-side expiry job.

### Admin notification backend

- Completed:
  - A Firebase Cloud Function emails the master admin when a booking becomes a fresh `pending` request.
  - The default admin email is `sushen.biswas.aga@gmail.com`.
  - Function setup docs and env template files exist.
- Partial:
  - Delivery depends on external deployment plus SMTP configuration and the `SMTP_PASSWORD` secret.
  - There are no automated tests for the function.
  - There is no retry dashboard or alternate notification channel.

### Manual approval semantics

- Completed:
  - `PhaseRepository.approveBooking()` marks bookings `approved`, increments `bookedSeats`, and unlocks the phase in `users/{uid}.unlockedPhases`.
  - `PhaseRepository.expireBooking()` can expire non-approved bookings.
- Partial:
  - These approval methods are not wired to any shipped admin UI.
  - Approval is not implemented as a Firestore transaction.

### Classroom and progress tracking

- Completed:
  - `ClassroomFragment` renders a phase classroom and lesson list.
  - `PhaseRepository.updateLessonProgress()` updates `phaseProgress`, `completedPhases`, overall `progress`, and advanced feature flags.
  - Advanced unlock thresholds are enforced at `30%`, `60%`, and `100%`.
- Partial:
  - All phases reuse the same three hardcoded lessons.
  - Lesson completion is not stored per lesson, only as phase-level percentages.
  - The checkbox state is therefore not durable or trustworthy across sessions.

### Home, My Learning, and Advanced screens

- Completed:
  - Home shows the user name, overall progress, and the latest unlocked phase.
  - My Learning shows the current unlocked phase and completed phases.
  - Advanced shows gated cards for bot install, investments, and affiliate tools.
- Partial:
  - Home's three advanced buttons are only enabled/disabled; they do not navigate.
  - My Learning resume state is phase-level only.
  - Advanced destinations are mostly placeholder content.

### Legacy email/password auth screens

- Completed:
  - Register and forgot-password activities are implemented and declared in the manifest.
- Partial:
  - They are not connected to the Google-first primary UX.
  - Validation is minimal.

### Education, investment, install, and affiliate sections

- Completed:
  - Screens and adapters exist.
  - Static content, links, and placeholder cards render.
- Partial:
  - Education content is hardcoded and separate from the phase/classroom system.
  - Investment plans are static and do not create `investments`.
  - Affiliate data is static placeholder text.
  - Install only opens an external GitHub URL.

### Automated testing

- Completed:
  - Unit tests cover `UserRepository`, `PhaseRepository`, and `PhaseViewModel`.
  - Instrumentation tests cover core `PhasesFragment` behavior.
  - Fake auth/store implementations support repository and fragment tests.
- Partial:
  - There is no automated coverage for most fragments, auth activities, advanced flows, or the Cloud Function backend.

## 5. Key Files & Responsibilities

- `app/src/main/java/com/shaplachottor/app/TradingAIApplication.kt`
  - Initializes Firebase at app startup.
- `app/src/main/java/com/shaplachottor/app/firebase/FirebaseManager.kt`
  - Central singleton access to Firebase Auth, Firestore, Storage, and Analytics.
- `app/src/main/java/com/shaplachottor/app/data/AppGraph.kt`
  - Service locator plus test-overridable auth/store abstractions.
- `app/src/main/java/com/shaplachottor/app/data/PhaseCatalog.kt`
  - Canonical six-phase curriculum.
- `app/src/main/java/com/shaplachottor/app/repository/UserRepository.kt`
  - Syncs/fetches the Firestore user profile.
- `app/src/main/java/com/shaplachottor/app/repositories/PhaseRepository.kt`
  - Seeds phases, reads phase data, handles booking requests, approval/expiry logic, classroom access checks, and progress updates.
- `app/src/main/java/com/shaplachottor/app/activities/SplashActivity.kt`
  - Startup auth gate and bootstrap flow.
- `app/src/main/java/com/shaplachottor/app/activities/LoginActivity.kt`
  - Google sign-in and post-auth bootstrap.
- `app/src/main/java/com/shaplachottor/app/activities/MainActivity.kt`
  - Main navigation shell with bottom navigation.
- `app/src/main/java/com/shaplachottor/app/fragments/PhasesFragment.kt`
  - Phase tabs, booking dialog, pending-state handling, classroom navigation.
- `app/src/main/java/com/shaplachottor/app/viewmodels/PhaseViewModel.kt`
  - Loads phases, filters by level, loads booking states, and submits booking requests.
- `app/src/main/java/com/shaplachottor/app/adapters/PhaseAdapter.kt`
  - Renders phase cards, booking button states, and pending/expired/approved messages.
- `app/src/main/java/com/shaplachottor/app/fragments/ClassroomFragment.kt`
  - Classroom screen and lesson interactions.
- `app/src/main/java/com/shaplachottor/app/viewmodels/ClassroomViewModel.kt`
  - Access checks, phase/lesson loading, progress refresh after toggles.
- `app/src/main/java/com/shaplachottor/app/fragments/HomeFragment.kt`
  - Home dashboard UI.
- `app/src/main/java/com/shaplachottor/app/viewmodels/HomeViewModel.kt`
  - Direct Firestore listener for user progress and current phase.
- `app/src/main/java/com/shaplachottor/app/fragments/MyLearningFragment.kt`
  - Current learning state and completed phases UI.
- `app/src/main/java/com/shaplachottor/app/viewmodels/MyLearningViewModel.kt`
  - Direct Firebase listeners for current phase and completed phases.
- `app/src/main/java/com/shaplachottor/app/fragments/AdvancedFragment.kt`
  - Gated advanced tools screen.
- `app/src/main/java/com/shaplachottor/app/viewmodels/AdvancedViewModel.kt`
  - Direct Firestore listener for overall progress and unlock checks.
- `app/src/main/res/navigation/nav_graph.xml`
  - Declares destinations and classroom `phaseId` argument.
- `firestore.rules`
  - Firestore security rules for users, phases, bookings, and placeholder collections.
- `functions/index.js`
  - Cloud Function that emails the admin on new pending booking requests.
- `functions/package.json`
  - Functions runtime dependencies and verification script.
- `docs/firebase_setup.md`
  - Firebase/Auth/Firestore setup steps.
- `docs/firebase_functions_setup.md`
  - SMTP secret/env setup for the admin notification backend.
- `app/src/debug/java/com/shaplachottor/app/testing/FakeAppStore.kt`
  - In-memory fake Firestore-like store for tests.
- `app/src/debug/java/com/shaplachottor/app/testing/FakeAuthSessionProvider.kt`
  - Fake signed-in user provider for tests.
- `app/src/test/...` and `app/src/androidTest/...`
  - Current automated coverage.
- `prompt` and `Ai_to_Ai_Devlopment/Master_Prompt/*`
  - AI workflow artifacts describing a broader product vision; not runtime code.

## 6. Current System Behavior

1. App launch starts at `SplashActivity`.
2. If no Firebase user is signed in, the app opens `LoginActivity`.
3. If a Firebase user exists, startup attempts to:
   - sync the Firestore user document
   - seed the six canonical phases if `phases` is empty
   - open `MainActivity`
4. `LoginActivity` supports Google sign-in. On success it signs into Firebase, syncs the Firestore user, seeds phases, and navigates to `MainActivity` if profile sync succeeds.
5. `MainActivity` hosts bottom navigation with `Home`, `Courses` (phases), `My Learning`, `Advanced`, and `Profile`.
6. `Home` shows welcome text, overall progress, latest unlocked phase, and three advanced buttons that only change enabled state.
7. `Courses` opens `PhasesFragment`, defaulting to the `Beginner` tab.
8. Locked phases show a booking CTA. Tapping it opens a dialog that requires:
   - phone number
   - WhatsApp number
9. Submitting the form writes a `pending` booking request with a 15-minute expiry window.
10. A deployed Cloud Function can email the master admin when that booking becomes a fresh `pending` request.
11. The app does not deduct a seat or unlock the phase at request time.
12. If the booking document later becomes `approved`, the user's phase becomes unlocked and the classroom becomes accessible.
13. `ClassroomFragment` blocks access if the phase is not in `users/{uid}.unlockedPhases`.
14. Inside the classroom, three hardcoded lessons are shown. Checkbox toggles update phase and overall progress.
15. `My Learning` shows the latest unlocked phase and completed phases if the user has any unlocked phases.
16. `Advanced` unlocks sub-screens at:
   - `30%` for install/bot setup
   - `60%` for investment
   - `100%` for affiliate
17. `Profile` currently only supports logout.
18. Register and forgot-password screens exist but are not part of the primary Google-first journey.

## 7. Known Issues / Weaknesses

- There is no shipped admin UI for approving or rejecting bookings.
- Booking approval is not transactional. Booking status, phase seats, and unlocked user state are written separately.
- Pending booking requests do not reserve seats. Multiple users can remain pending for the same remaining seat count.
- Booking expiry is lazy, not truly automatic. Expiration happens when code reads or rewrites bookings, not from a scheduled server-side expiry job.
- `PhaseRepository.getLessonsForPhase()` returns the same three hardcoded lessons for every phase.
- Lesson completion is not stored per lesson, so repeated toggling can distort progress.
- Architecture is inconsistent. Several ViewModels bypass repositories and use Firebase SDKs directly.
- `HomeViewModel`, `MyLearningViewModel`, and `AdvancedViewModel` add snapshot listeners but do not keep/remove registrations in `onCleared()`.
- `SplashActivity` ignores bootstrap failures and proceeds to `MainActivity` as long as a Firebase user exists.
- `LoginActivity` can leave a user signed in even when Firestore profile sync fails.
- `RegisterActivity` and `ForgotPasswordActivity` have minimal validation and are not integrated into the primary UX.
- Home's advanced buttons do not navigate anywhere.
- Profile fields are static placeholders and are not bound to the actual current user.
- `EducationFragment`, `InvestFragment`, `AffiliateFragment`, and `InstallFragment` are mostly placeholder or static content.
- The manual approval backend requires deployed Functions plus SMTP configuration; the repo alone does not make notifications work.
- There are no automated tests for the Cloud Function.
- The POSIX `gradlew` wrapper is malformed (`CLASSPATH="\\\"\\\""`), while Windows `gradlew.bat` is the working wrapper.
- The repo still shows scope drift:
  - project name: `TradingAI`
  - app brand: `ShaplaChottor`
  - prompt artifacts describe a broader Web3/investment/admin product than the implemented app delivers

## 8. Next Recommended Features (Top 5)

### 1. Admin approval interface

- Why it matters:
  - The current manual approval workflow is incomplete without a real admin surface to review, approve, reject, and expire bookings.
- Complexity:
  - High

### 2. Server-side automatic booking expiry

- Why it matters:
  - The current 15-minute expiry is not truly automatic; it is only normalized when app code touches the booking again.
- Complexity:
  - Medium

### 3. Transaction-safe approval flow

- Why it matters:
  - Approval currently updates booking, phase, and user state separately and can leave data inconsistent under failure or concurrency.
- Complexity:
  - Medium

### 4. Persisted lesson completion model

- Why it matters:
  - Progress is currently easy to corrupt because lesson state is hardcoded and not stored durably.
- Complexity:
  - Medium

### 5. Unify Firebase access behind repositories

- Why it matters:
  - Consolidating direct SDK usage behind repositories would improve testability, listener lifecycle management, and consistency.
- Complexity:
  - High

## 9. Development State Summary

- Estimated completion:
  - About `65%` of an MVP for the sign-in, phase access, booking-request, and progress-tracking flow.
  - Much lower for the broader investment, affiliate, education, and admin product suggested by the prompt artifacts.
- Current stage:
  - Mid-stage prototype.
- Verification state in this analysis session:
  - `.\gradlew.bat testDebugUnitTest` passed.
  - `functions/npm run verify` passed and loaded the Functions module successfully.
  - Instrumentation tests exist, but they were not executed in this analysis session.
- Practical summary:
  - The Android learning backbone is real and usable.
  - Manual booking requests, approval semantics, and admin email notification code now exist.
  - The product still lacks a complete admin approval experience, durable lesson state, and finished advanced/product-facing features.
