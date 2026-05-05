# Project Overview

- Shapla Chottor Lab is a Kotlin Android app for AI, coding, and trading-oriented education. Users sign in with Google, browse a fixed 6-phase curriculum, request a limited seat for each phase, and access classroom content only after manual approval.
- Target users are students or research-community members progressing through a guided learning path, plus one internal admin who reviews seat requests and manages classroom access.
- Core purpose is to gate educational access, track learning progress, and eventually unlock higher-value experiences such as bot installation, investment/community access, and affiliate growth loops.

# Architecture

- Tech stack: Kotlin, Android Views with ViewBinding, Material 3, Navigation Component with Safe Args, MVVM-style viewmodels, Firebase Auth, Firestore, Functions dependency, Analytics dependency, Storage dependency, Messaging dependency, Google Sign-In, Glide, and Meta Android SDK.
- App shell: `SplashActivity` routes to `LoginActivity` or `MainActivity`; `MainActivity` hosts a `NavHostFragment` with bottom navigation for Home, Phases, My Learning, Advanced, and Profile.
- Dependency wiring is manual: `AppGraph` exposes `FirebaseAuthSessionProvider` and `FirestoreAppStore`; repositories are instantiated directly by screens or viewmodels rather than through a DI framework.
- Data access is Firestore-centric: `FirestoreAppStore` reads and writes `users`, `phases`, `bookings`, lesson progress subcollections, `referralEvents`, and `affiliateStats`.
- Core domain modules are `UserRepository` for profile creation/merge and referral setup, and `PhaseRepository` for phase seeding, lesson loading, booking requests, approval hooks, access checks, and progress recalculation.
- View models: `HomeViewModel`, `PhaseViewModel`, `ClassroomViewModel`, and `MyLearningViewModel` drive the main user flows; several secondary screens are fragment-only placeholders with no backing viewmodel logic.
- Domain models: `User`, `Phase`, `Lesson`, `Booking`, `AdvancedFeatures`, `BookingRequestResult`, and `BookingRequestOutcome`.
- Static curriculum source: `PhaseCatalog` defines the canonical 6 phases and seeds Firestore when the remote `phases` collection is empty.

# Key Features

- Google-first authentication: `LoginActivity` uses Google Sign-In and Firebase Auth, then creates or merges a `users/{uid}` profile in Firestore.
- Splash routing: `SplashActivity` shows a short splash delay, logs a Meta test event in debug builds for validation, and routes the user into auth or the main app shell.
- 6-phase learning journey: users browse Beginner, Intermediate, and Advanced tabs backed by a fixed phase catalog with descriptions, ordering, and seat counts.
- Manual seat request flow: locked phases can create `bookings/{userId_phaseId}` with WhatsApp contact info and a 15-minute approval window.
- Pending-state UX: phase cards show pending status, a countdown timer, expiry behavior, and re-request messaging for rejected or expired requests.
- Classroom gating: users can only enter a classroom if the phase is present in `users/{uid}.unlockedPhases`.
- Lesson completion and progress: each phase currently exposes 3 hardcoded lessons; completion updates per-lesson state, per-phase progress, overall progress, and `AdvancedFeatures`.
- My Learning dashboard: shows overall progress, current phase, and completed phases using realtime user snapshots.
- Home dashboard: shows welcome state, profile image, total progress, and a resume CTA for the most recently unlocked phase.
- Admin panel: the hardcoded admin email can inspect pending or all requests, approve or reject requests, cancel seats, copy WhatsApp numbers, and open WhatsApp chat links.
- Admin local notifications: `AdminNotificationManager` watches Firestore for new pending bookings and shows local notifications on the admin device.
- Referral scaffolding: new users receive a derived referral code, and the app store supports `referralEvents` and `affiliateStats` collections.
- Advanced section scaffolding: Install, Invest, and Affiliate screens exist in navigation and layouts, but most of their behavior is still static UI.
- Privacy policy screen: `PrivacyPolicyActivity` renders a bundled privacy policy and links users to the website for contact and deletion requests.
- Build metadata in login: the login screen displays app version and current git branch from Gradle-generated config values.
- Meta SDK instrumentation: the app configures Meta App ID, client token, auto init, auto app events, advertiser ID collection, app activation, and debug-only diagnostic logging at app startup.

# User Flow

- Install and open: the user lands on the splash screen, which initializes app services and checks Firebase Auth session state.
- Sign in: unauthenticated users sign in with Google; successful auth creates or updates a Firestore profile document.
- Enter the main shell: the user sees a 5-tab bottom-navigation experience with Home, Phases, My Learning, Advanced, and Profile.
- Explore the curriculum: the user browses the 6 learning phases by level and sees lock state, pending state, unlocked state, and seat availability.
- Request access: for an available locked phase, the user submits a booking request with WhatsApp contact details and waits for manual approval.
- Get approved: the admin reviews the request in the admin panel and marks it approved, which is intended to unlock the classroom for that user.
- Learn inside the classroom: approved users open the phase classroom, check off lessons, and increase phase and overall progress.
- Return for retention: Home and My Learning surface progress, current phase, and completed phases so the user continues the path across sessions.
- Discover advanced offers: as progress rises, the app is intended to unlock bot installation, investment/community, and affiliate features, although those gates are not fully enforced yet.

# Data & State

- Authentication state comes from Firebase Auth through `FirebaseAuthSessionProvider`.
- `users/{userId}` stores identity, `progress`, `phaseProgress`, `unlockedPhases`, `completedPhases`, `unlockedFeatures`, `referralCode`, and `referredBy`.
- `phases/{phaseId}` stores the canonical phase metadata plus `totalSeats` and `bookedSeats`.
- `bookings/{bookingId}` stores request ownership, contact info, timestamps, expiry time, and booking status.
- `users/{userId}/progress/{phaseId}/lessons/{lessonId}` stores per-lesson completion booleans.
- `referralEvents` and `affiliateStats/{userId}` exist for future affiliate and conversion tracking.
- UI state is mostly held in `LiveData` inside viewmodels; there is no Room database, local cache, or offline-first sync layer.
- `HomeViewModel` and `MyLearningViewModel` subscribe to realtime user document updates through `AppStore.getUserStream`.
- `PhaseRepository` is the main orchestration layer for catalog seeding, lesson generation, booking logic, access checks, and progress recalculation.
- Advanced feature unlock state is computed from overall progress thresholds: trading bot at 30%, investment at 60%, and affiliate at 100%.

# Integrations

- Firebase Auth: handles Google account authentication and session state.
- Firestore: serves as the app's primary backend for users, phases, bookings, lesson progress, referral events, and affiliate stats.
- Firebase Functions: the Android app includes a callable `approveBooking` path in `PhaseRepository`, but the backing backend code is not present in this repository.
- Firebase Analytics: the dependency is included, but there is no meaningful app-specific analytics instrumentation yet.
- Firebase Storage: the dependency is included but unused in the current Android code.
- Firebase Messaging: the dependency is included but push messaging is not implemented; admin notifications currently rely on a local Firestore listener instead.
- Google Services: `app/google-services.json` is present and includes clients for both `com.shaplachottor.app` and `com.shaplachottor.lab`.
- Meta Android SDK: initialized in `TradingAIApplication` with auto event logging, advertiser ID collection, app activation, runtime key-hash logging in debug builds, and debug-only diagnostic behaviors.
- WhatsApp deep links: the admin panel opens `https://api.whatsapp.com/send?phone=...` to contact requesters.
- Glide: loads Google profile photos in Home and Profile.
- Navigation Component Safe Args: routes classroom navigation with `phaseId` arguments.

# Current Implementation Status

- Complete: the Android shell, Google sign-in, Firestore-backed user creation, phase catalog seeding, phase listing, booking creation, classroom gating, lesson completion UI, My Learning dashboard, profile/logout flow, privacy policy screen, admin panel UI, and Meta SDK initialization are implemented.
- Complete: a debug build is currently producible; on May 5, 2026, `java -jar gradle\wrapper\gradle-wrapper.jar assembleDebug` produced `app/build/outputs/apk/debug/app-debug.apk`.
- Partial: approval and seat-management logic exists in both client UI and a repository callable path, but the implementation is split and inconsistent.
- Partial: the advanced monetization surfaces exist as screens and data model fields, but Install, Invest, Affiliate, and referral UX are mostly static and not wired end to end.
- Partial: admin notifications work as local Firestore listeners, not as robust server-driven push or email workflows.
- Partial: documentation exists for Firebase, Firestore, and Meta setup, but parts of it are outdated relative to the current package name, rules, and repo contents.
- Missing: the `functions/` backend referenced by docs and `PhaseRepository.approveBooking()` is absent from the repository.
- Missing: automated test coverage is effectively absent; `app/src/test` and `app/src/androidTest` contain no meaningful tests.
- Missing: true server-side enforcement for seat counts, approval transactions, referral payout logic, and analytics funnels.

# Known Issues / Gaps

- The provided Firestore rules do not match the Android client behavior. Normal users are blocked from writing `progress`, `phaseProgress`, `unlockedPhases`, `completedPhases`, `unlockedFeatures`, and lesson completion, while the app currently tries to write those fields directly from the client.
- Admin authorization is inconsistent. UI access is based on a hardcoded email (`sushen.biswas.aga@gmail.com`), but Firestore rules require a custom auth claim `request.auth.token.role == 'admin'`.
- The approval backend is incomplete. Docs and `PhaseRepository.approveBooking()` expect a Cloud Function named `approveBooking`, but the repo has no `functions/` directory, and `AdminPanelFragment` bypasses that path with direct Firestore writes.
- Seat counts are not updated anywhere in the current client admin flow. `bookedSeats` and `availableSeats` can drift from reality because approval and cancellation do not adjust phase seat totals.
- Prerequisite gating is only checked against the previous item in the currently filtered level tab. This means the first phase in Intermediate and Advanced tabs can be requested without necessarily completing the previous global phase.
- The booking dialog only captures a WhatsApp number, but the model, repository, and docs describe separate phone and WhatsApp fields. The current UI passes the same value for both.
- Advanced unlock thresholds are computed and stored, but the Advanced, Install, Invest, and Affiliate UI does not enforce those gates.
- Home current-course copy is incomplete because `HomeFragment` displays `phase.focus`, while seeded phases do not populate `focus`.
- Android 13+ notification permission is requested in `MainActivity`, but `POST_NOTIFICATIONS` is not declared in the manifest, so admin notifications are not release-ready on modern devices.
- `gradlew.bat` is malformed: it clears `CLASSPATH` instead of pointing to `gradle-wrapper.jar`. The project can build through direct wrapper-jar invocation, but the standard Windows wrapper command currently fails.
- The privacy policy says the app does not use tracking or advertising technologies, but Meta auto event logging and advertiser ID collection are enabled in code.
- Meta dashboard setup is still incomplete until the Google Play signing SHA-1 is converted to a Key Hash and added to the Meta app settings. External Events Manager verification is also still pending.
- `RegisterActivity`, `ForgotPasswordActivity`, `EducationFragment`, and several layout controls exist as placeholders or dead-end UI rather than finished flows.

# Growth & Monetization Opportunities

- Turn the existing phase-gating system into paid or application-based cohort access. The booking and approval model already fits premium seats, mentorship tiers, or limited research cohorts.
- Activate the referral scaffolding. The data layer already supports `referralCode`, `referralEvents`, and `affiliateStats`; a shareable referral link, attribution capture, and reward system could create a strong growth loop.
- Convert the Advanced area into monetized modules. Install Bot, Invest, and Affiliate screens are already positioned as post-learning upsells and can become premium tools, community memberships, or managed-service offers.
- Add funnel analytics around login, booking request, approval, first lesson completion, phase completion, and advanced feature unlocks to identify where users drop before monetization.
- Use the admin booking workflow as a retention lever. WhatsApp outreach during pending approval can transition users into community groups, coaching, or recurring memberships.
- The Invest screen and `docs/web3_integration_template.md` suggest a future wallet-connected investment flow, but that should come only after the core learning and approval loop is stable and secure.

# Next Best Actions (IMPORTANT)

- 1. Unify the backend contract: implement the missing Cloud Functions or admin backend, move approval, progress, and seat mutations server-side, and align Firestore rules with the actual client flow.
- 2. Finish the real product paths: fix phase prerequisite logic, collect separate phone and WhatsApp fields, enforce advanced unlock gates, and connect Install, Invest, Affiliate, and referral screens to live data and actions.
- 3. Clean release readiness: complete Meta Key Hash and Events Manager verification, add manifest support for notification permission, repair `gradlew.bat`, and update README, setup docs, and privacy text so they match the shipped package, data usage, and backend architecture.

# Meta SDK Integration Audit

## SDK Setup

- Dependency: `com.facebook.android:facebook-android-sdk:latest.release` is present in `app/build.gradle`.
- App ID: Configured in `app/src/main/res/values/strings.xml` as `1603183510975171`.
- Client Token: Configured in `app/src/main/res/values/strings.xml` as `fcc2c7f25cd0f8cb65b3cb7db674ef7a`.

## Manifest

- Auto Init: Enabled with `com.facebook.sdk.AutoInitEnabled=true`.
- Auto Log Events: Enabled with `com.facebook.sdk.AutoLogAppEventsEnabled=true`.
- Advertiser ID: Enabled with `com.facebook.sdk.AdvertiserIDCollectionEnabled=true`.
- Permissions: `android.permission.INTERNET` and `com.google.android.gms.permission.AD_ID` are present.

## App Identity

- Package: `com.shaplachottor.lab`
- Launcher: `com.shaplachottor.lab.activities.SplashActivity`

## Event Logging

- Implemented: `SplashActivity` logs `fb_mobile_test_event` in debug builds and forces a flush for validation.

## Debug

- Enabled: Meta debug logging and runtime Key Hash output are enabled only in debug builds.

## SHA / Key Hash

- Configured: Not verifiable from the repository. The app prints runtime Key Hash values in debug builds, but the Meta dashboard entry still requires a manual Google Play SHA-1 to Key Hash conversion.

## Manual Workflow

- Meta Developer:
  - Create or open the Meta app for this Android package.
  - Confirm package name is `com.shaplachottor.lab`.
  - Confirm launcher activity is `com.shaplachottor.lab.activities.SplashActivity`.
  - Add the final Key Hash in the Meta Android app settings.
- Google Play Console:
  - Open App Integrity for the production app.
  - Copy the Google Play app signing SHA-1 certificate.
  - Convert the SHA-1 to a Base64 Key Hash with:
    `echo SHA1 | tr -d ':' | xxd -r -p | openssl base64`
  - Paste the generated Key Hash into Meta Developer settings.
- External validation:
  - Open Meta Events Manager and go to Test Events.
  - Run a debug build on a device.
  - Confirm `fb_mobile_test_event` appears.

## Installation

- Play Store Required: Play installs are still required for real install attribution in Meta Ads.

## Final Status

- Status: Implementation is complete in the Android app. Manual Meta dashboard Key Hash setup and external Events Manager verification are still pending before calling the integration production-ready.

## Next Steps

- Convert the Google Play signing SHA-1 into a Base64 Key Hash and add it to the Meta app settings.
- Install from a Play-managed build when validating real install attribution.
- Open Meta Events Manager Test Events and confirm `fb_mobile_test_event` is received from a debug build.
