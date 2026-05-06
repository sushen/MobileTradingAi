# Project Overview

- Shapla Chottor Lab is a Kotlin Android learning app for AI, coding, and trading-oriented education with a gated classroom model.
- Target users are students moving through a fixed 6-phase curriculum and one internal admin who manually reviews access requests.
- Core purpose is to onboard users with Google sign-in, control access to limited-seat learning phases, track lesson progress, and create upgrade paths into higher-value advanced offerings.

# Architecture

- Tech stack: Kotlin, Android Views, ViewBinding, Material 3, Navigation Component with Safe Args, LiveData/ViewModel, Firebase Auth, Firestore, Google Sign-In, Glide, and Meta Android SDK.
- Build setup: Android Gradle Plugin `8.5.1`, Kotlin `2.1.0`, `compileSdk 35`, `targetSdk 35`, `minSdk 24`, app version `1.0.8`.
- App shell: `SplashActivity` routes to `LoginActivity` or `MainActivity`; `MainActivity` hosts a `NavHostFragment` with bottom navigation for Home, Phases, My Learning, Advanced, and Profile.
- Dependency wiring is manual through `AppGraph`, which exposes `FirebaseAuthSessionProvider` and `FirestoreAppStore`; there is no DI framework.
- Data access is Firestore-first: `FirestoreAppStore` handles `users`, `phases`, `bookings`, per-lesson progress subcollections, `referralEvents`, and `affiliateStats`.
- Core business logic lives in `UserRepository` and `PhaseRepository`; `PhaseRepository` owns phase seeding, lesson loading, seat requests, approval/rejection/cancellation/expiry transactions, access checks, and progress recalculation.
- Lesson architecture is now hybrid static content: `Phase1LessonProvider` supplies authored Phase 1 lessons with structured content blocks, while phases 2-6 still use placeholder lessons defined directly in `PhaseRepository`.
- Shared classroom state spans `ClassroomFragment` and `LessonDetailFragment` through an activity-scoped `ClassroomViewModel`.
- Utility module `ProgressCalculator` centralizes phase progress, overall progress, completion checks, and feature unlock math.
- View models: `HomeViewModel`, `PhaseViewModel`, `ClassroomViewModel`, and `MyLearningViewModel` drive the main journeys; several secondary screens are simple fragments with no real domain logic.
- Domain models: `User`, `Phase`, `Lesson`, `LessonContentBlock`, `Booking`, `AdvancedFeatures`, `BookingRequestResult`, and `BookingRequestOutcome`.
- Static curriculum source: `PhaseCatalog` defines the canonical 6 phases and acts as a fallback when Firestore phase reads fail.
- Non-runtime repo assets: `Ai_to_Ai_Development/` stores prompt templates and AI handoff files used for the team's AI-assisted development workflow.

# Key Features

- Google-first authentication: `LoginActivity` signs users in with Google and Firebase Auth, then creates or merges `users/{uid}` in Firestore.
- Splash routing: `SplashActivity` shows a 2-second splash, verifies Meta SDK state in debug builds, logs `fb_mobile_test_event`, and routes based on Firebase Auth session state.
- Fixed 6-phase curriculum: users browse Beginner, Intermediate, and Advanced tabs backed by `PhaseCatalog` and Firestore phase documents.
- Seat-request flow: locked phases create `bookings/{userId_phaseId}` with a 15-minute pending window; seats are reserved at request time by incrementing `phases/{phaseId}.bookedSeats`.
- Pending-state UX: phase cards show booking state, remaining approval time, expiry messaging, and retry messaging for rejected or expired requests.
- Manual admin workflow: the admin panel can review pending or all bookings, approve requests, reject pending requests, cancel approved seats, copy WhatsApp numbers, and open WhatsApp chat links.
- Classroom gating: `ClassroomFragment` only opens if the current phase exists in `users/{uid}.unlockedPhases`.
- Structured lesson reader: Phase 1 now exposes 4 authored lessons with typed content blocks (`CONCEPT`, `EXAMPLE`, `EXERCISE`, `REFLECTION`) rendered in a dedicated lesson detail screen.
- Sequential lesson UX: the classroom list visually locks later lessons until the previous lesson is completed, and unlocked lessons navigate into the detail reader.
- Lesson completion and progress: each completed lesson updates lesson docs, `phaseProgress`, `completedPhases`, overall `progress`, and `unlockedFeatures`; phases 2-6 still expose 3 placeholder lessons each.
- Progress dashboards: Home and My Learning show profile details, progress indicators, current phase, and completed phases using realtime user snapshots.
- Referral scaffolding: new users receive a derived `referralCode`, and the app store can write `referralEvents` and `affiliateStats` and record a conversion when a referred user completes Phase 1.
- Advanced section scaffolding: Install, Invest, and Affiliate screens are reachable from the Advanced tab, but they are still mostly static UI.
- Profile and privacy: Profile supports logout, conditional admin access, and opening a bundled privacy policy screen.
- Build metadata in login: the login screen displays app version and current git branch via Gradle-generated config values.
- Meta SDK integration: the app configures Meta App ID, client token, auto init, auto app events, advertiser ID collection, app activation, and debug-only key-hash logging.
- Local admin notifications: `AdminNotificationManager` listens to pending booking additions in Firestore and shows local notifications on the admin device.

# User Flow

- Install and open: the user lands on the splash screen while Meta SDK and Firebase initialization complete.
- Sign in: unauthenticated users sign in with Google; successful auth creates or updates their Firestore profile.
- Enter the main shell: the user reaches the 5-tab experience with Home, Phases, My Learning, Advanced, and Profile.
- Explore phases: the user browses the 6 learning phases and sees locked, pending, unlocked, and seat-availability states.
- Request access: for an available locked phase, the user submits a booking request from the phase card dialog using a WhatsApp number.
- Wait for approval: the request stays pending for 15 minutes while the admin can contact the user and approve or reject it.
- Learn inside the classroom: approved users open the classroom, tap the next available lesson, read the structured lesson detail view, mark lessons complete, and gradually unlock later lessons.
- Return for retention: Home and My Learning surface the last unlocked phase, total progress, and completed phases across sessions.
- Discover advanced offers: as progress grows, the user can navigate to Install, Invest, and Affiliate screens, although those flows are not yet fully functional.

# Data & State

- Authentication state comes from Firebase Auth via `FirebaseAuthSessionProvider`.
- `users/{userId}` stores identity, `progress`, `phaseProgress`, `unlockedPhases`, `completedPhases`, `unlockedFeatures`, `referralCode`, and `referredBy`.
- `phases/{phaseId}` stores phase metadata plus `totalSeats` and `bookedSeats`; `availableSeats` is computed in the client model as `totalSeats - bookedSeats`.
- `bookings/{bookingId}` stores `userId`, `phaseId`, `phoneNumber`, `whatsappNumber`, timestamps, and status.
- `users/{userId}/progress/{phaseId}/lessons/{lessonId}` stores lesson completion booleans.
- `referralEvents/{referrerId_referredUserId}` stores referral lifecycle state, and `affiliateStats/{userId}` stores aggregate invite/conversion counters.
- Lesson definitions are static app data, not Firestore content; `Lesson` now stores `phaseId`, `order`, `contentBlocks`, and `isCompleted`.
- `LessonContentBlock` models typed lesson sections for concept, example, exercise, and reflection content.
- UI state is held mostly in `LiveData`; realtime user updates come from `AppStore.getUserStream`.
- `ProgressCalculator` is the single place for progress math and advanced feature unlock thresholds.
- There is no Room database, local cache, offline sync layer, or backend-owned state machine.
- Advanced feature unlock state is recalculated from overall progress thresholds: trading bot at `30%`, investment at `60%`, and affiliate at `100%`.

# Integrations

- Firebase Auth: Google account authentication and session management.
- Firestore: primary backend for users, phases, bookings, lesson progress, referral events, and affiliate stats.
- Google Sign-In: drives the login entry point and supplies the ID token for Firebase Auth.
- Google Services config: `app/google-services.json` includes Android clients for both `com.shaplachottor.app` and `com.shaplachottor.lab`.
- Meta Android SDK: initialized in `TradingAIApplication` with auto app events, advertiser ID collection, app activation, debug logging, and runtime key-hash output in debug builds.
- WhatsApp deep links: the admin panel opens `https://api.whatsapp.com/send?phone=...` for booking follow-up.
- Glide: loads Google profile photos in Home and Profile.
- Navigation Component Safe Args: passes `phaseId` and `lessonId` into classroom and lesson-detail flows.
- Firebase Analytics, Storage, Messaging, and Functions dependencies are included, but there is no meaningful Analytics instrumentation, no Storage usage, no push implementation, and no backend Functions code in this repository.

# Current Implementation Status

- Complete: the Android shell, Google sign-in, Firestore-backed user creation, phase browsing UI, booking creation, admin approval/rejection/cancellation actions, classroom gating, lesson completion, progress recalculation, My Learning dashboard, profile/logout flow, privacy policy screen, and Meta SDK app-side initialization are implemented.
- Complete: a dedicated lesson-detail flow is implemented with typed content blocks, completion CTA, and shared classroom state between the list and detail screens.
- Complete: a debug build is currently reproducible; on May 5, 2026, `java -jar gradle\wrapper\gradle-wrapper.jar assembleDebug` succeeded and produced `app/build/outputs/apk/debug/app-debug.apk`.
- Partial: Phase 1 has real authored lesson content and 4 lessons, but phases 2-6 still rely on 3 placeholder lessons each with empty content blocks.
- Partial: Firestore rules are tighter than before and now explicitly support owner-driven expiry plus +/-1 counter updates, but the privilege model is still heavily client-driven and not fully safe.
- Partial: referral and affiliate persistence exists in the data layer, but there is no user-facing referral capture, sharing, reward, or stats UI.
- Partial: the Advanced, Install, Invest, and Affiliate areas are navigable but not connected to real gated actions or monetization workflows.
- Partial: admin notifications are local Firestore listeners rather than server-driven push, email, or queue-based workflows.
- Partial: automated test coverage now exists for `ProgressCalculator`; the generated test report on May 5, 2026 showed 4 passing unit tests.
- Partial: documentation exists for Firebase, Firestore, and Meta setup, but several files still reference old package names, missing backend folders, or outdated data contracts.
- Missing: the optional `functions/` backend referenced by setup docs is absent from the repo, so all approval and notification logic remains client-side only.
- Missing: broad repository, Firestore rule, fragment, and instrumentation test coverage.
- Missing: a trusted non-client enforcement layer for seats, affiliate counters, and privileged state transitions.

# Known Issues / Gaps

- Sensitive booking, seat, approval, and affiliate logic still runs from Android clients plus Firestore rules only.
- Firestore security around `phases.bookedSeats` is improved but still weak: any signed-in user can increment or decrement any phase's seat counter by 1.
- Firestore security around `affiliateStats` is improved but still weak: any signed-in user can increment another user's `totalInvites` or `conversions` by 1.
- Phase bootstrapping remains brittle. `ensurePhasesSeeded()` still depends on admin-only phase creation, and `requestSeat()` now uses `transaction.update()` on `phases/{phaseId}`, so bookings fail if phase documents were never pre-created in Firestore.
- Sequential lesson progression is enforced only in the UI. `PhaseRepository.updateLessonProgress()` and `LessonDetailFragment` do not independently block out-of-order completion if a user reaches later lesson IDs directly.
- Only Phase 1 has rich lesson content; phases 2-6 still return empty content blocks, so the new lesson-detail screen is mostly blank beyond the first phase.
- Prerequisite gating only checks the previous phase inside the currently selected level tab. As a result, the first Intermediate phase and the first Advanced phase can be requested without completing the previous global phase.
- The booking dialog captures only a WhatsApp number and passes it as both `phoneNumber` and `whatsappNumber`, even though the data model stores separate fields.
- Advanced unlock thresholds are computed and stored, but the UI does not actually block or unlock Install, Invest, and Affiliate experiences based on those values.
- `HomeFragment` displays `phase.focus`, but `PhaseCatalog` seeds `focus` as empty strings, so the current-course subtitle is blank.
- Android 13+ notification permission is requested in `MainActivity`, but `POST_NOTIFICATIONS` is not declared in the manifest.
- `gradlew.bat` clears `CLASSPATH` and is not a reliable standard wrapper entry point; direct wrapper-jar invocation works, but the Windows wrapper script itself needs repair.
- The privacy policy says the app does not use tracking or advertising technologies, but Meta auto app events and advertiser ID collection are enabled in code.
- Meta dashboard setup is still not fully production-ready until the correct Key Hash is registered and Events Manager test events are externally verified.
- The project builds with `compileSdk 35` on Android Gradle Plugin `8.5.1`, which currently emits an unsupported compile SDK warning.
- `RegisterActivity`, `ForgotPasswordActivity`, `EducationFragment`, and the Advanced sub-screens remain placeholders or thin static screens rather than complete flows.
- Admin authorization is hardcoded to `sushen.biswas.aga@gmail.com` in both UI and Firestore rules, which is brittle and not scalable.

# Growth & Monetization Opportunities

- Turn the current seat-request and manual-approval model into paid cohorts, mentorship access, or application-based premium classrooms.
- Use the new structured lesson/content-block format as a differentiated premium learning product, expanding the authored in-app curriculum from Phase 1 into phases 2-6.
- Activate the referral scaffolding with a real referral entry flow, share links, attribution, and rewards to create a measurable user-acquisition loop.
- Convert Install Bot, Invest, and Affiliate into monetized post-learning modules, paid tools, or subscription/community upgrades.
- Add funnel analytics around sign-in, booking request, approval, first lesson completion, phase completion, and advanced-feature unlocks to identify drop-off points.
- Use the existing admin WhatsApp follow-up as a retention and conversion channel into community groups, coaching, or recurring memberships.
- The Web3 template in `docs/web3_integration_template.md` can support future investment flows, but only after the core access and learning loop is more reliable.

# Next Best Actions (IMPORTANT)

- 1. Harden the client-side Firestore contract end to end: phase bootstrapping, seat counters, affiliate counters, and admin/student state transitions still need tighter rule alignment and safer client flows.
- 2. Finish the learning journey: expand the structured lesson-detail system beyond Phase 1 and enforce sequential plus cross-phase progression in repository logic, not just the UI.
- 3. Clean release readiness: add `POST_NOTIFICATIONS` to the manifest, repair `gradlew.bat`, align README/setup docs/privacy text with the current package and tracking behavior, and complete Meta Key Hash plus Events Manager verification.
