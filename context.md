# Project Overview

- This app is an Android learning platform for Shapla Chottor Lab / TradingAI that sells a 6-phase AI/coding journey, mixing free foundational content with paid cohort unlocks.
- Target users are students learning programming, data, and AI workflows, plus a hardcoded lead admin account that manually reviews bookings and unlocks premium access.
- The core purpose is to turn a learning catalog into a monetized progression system where lesson completion unlocks advanced surfaces and referrals help drive new enrollments.

# Architecture

- Frontend stack: Kotlin 2.1.0, Android Views/XML, ViewBinding, Material 3, Navigation Component with Safe Args, RecyclerView, LiveData, Coroutines, and Glide.
- App architecture: MVVM plus Repository pattern with a lightweight service locator (`AppGraph`) instead of Hilt/Dagger.
- Backend stack: Firebase Auth, Cloud Firestore, Google Sign-In, Firestore Security Rules, and Meta Android SDK. There is no custom backend service in this repo.
- Data layer: `AppStore` abstracts persistence, and `FirestoreAppStore` implements cache-first Firestore access, a real-time user stream, and a 100 MB persistent offline cache.
- Core business logic lives in `PhaseRepository`, which handles phase catalog fallback/seeding, lesson access, canonical sequential lesson-state resolution, progress reconciliation, seat booking, approval, cancellation, and expiration.
- User identity and referral attribution live in `UserRepository`, which creates or merges user profiles, generates referral codes, and logs referred-user attribution.
- UI shell: `SplashActivity` decides auth routing, `MainActivity` hosts bottom navigation, and fragments cover Home, Phases, My Learning, Advanced, Profile, Classroom, Lesson Detail, Affiliate, and Admin Panel.
- Content architecture: phase metadata can come from Firestore or local `PhaseCatalog`, but lesson content is fully local Kotlin data in `Phase1LessonProvider` through `Phase6LessonProvider`.
- Connectivity-aware refresh is built in through `NetworkMonitor`; the phase and classroom viewmodels reload catalog/progress when the device comes back online.
- Current UI implementation is XML/ViewBinding only. Older Compose references in surrounding docs are outdated.

# Key Features

- Premium cohort model: Phase 1 is free, while Phases 2 to 6 are premium cohorts with pricing, seat caps, and optional cohort start metadata.
- Manual admin approval: premium requests stay locked until an admin confirms payment and approves access from the Admin Panel.
- Seat reservation workflow: requesting a premium phase creates `bookings/{userId}_{phaseId}`, reserves a seat immediately in Firestore, and sets a 15-minute pending window.
- Sequential learning enforcement: users cannot book or access a later phase until the previous phase is completed, and lessons inside each phase must be completed in order.
- Structured classroom experience: each phase has static lessons with concept, example, exercise, and reflection blocks, and lessons can be opened individually in a detail screen.
- Self-healing classroom progression: `SequentialLessonProgressResolver` treats visible lesson completion as a contiguous prefix, rejects future-lesson completion, and repairs invalid Firestore completion states during reads and writes.
- Equal-phase progress model: overall journey progress is weighted evenly across the 6 phases, so finishing a full phase contributes roughly 16 percent regardless of lesson count.
- Feature gating: advanced tools unlock at defined progress thresholds - bot install at 30 percent, invest/community at 60 percent, and affiliate at 100 percent.
- Home and My Learning dashboards: both surfaces compute live progress from Firestore user updates and help the user resume the currently active phase.
- Referral system: new users can enter a referral code during Google sign-in or email registration, and referral events convert when the referred user completes Phase 1.
- Affiliate views: Profile always shows the user's referral code plus invite/conversion counts, while the gated Affiliate screen adds copy/share actions and referral history.
- Admin tooling: admins can review pending or all booking requests, open WhatsApp, copy numbers, approve after payment confirmation, reject, or cancel seats.
- Admin in-app notifications: when the hardcoded admin account is signed in and grants notification permission, the app listens for new pending bookings and posts local device notifications.
- Auth utilities: Google sign-in is implemented, and email/password account creation plus password reset are available.
- Privacy policy screen: a dedicated activity renders in-app policy text.

# User Flow

- Install and launch: `SplashActivity` initializes Firebase/AppGraph, optionally logs a Meta test event in debug, and routes to `LoginActivity` or `MainActivity`.
- Sign in or register: a user can sign in with Google or create an account with email/password, optionally supplying a referral code during either path.
- Profile creation: `UserRepository` creates or merges the user profile, assigns a 6-character referral code, and logs a referral event if the account was referred.
- Explore learning paths: after login, the user lands in the bottom-nav shell and can browse Home, Phases, My Learning, Advanced, and Profile.
- Start learning: Phase 1 is free and can be entered without booking; later phases require both prerequisite completion and access approval.
- Request premium access: from `PhasesFragment`, the user submits a seat request with WhatsApp contact info and enters a pending state with a 15-minute expiry window.
- Admin review: the admin receives an in-app notification while the app is active, opens the Admin Panel, validates payment for premium cohorts, and approves or rejects the request.
- Enter classroom: approved users open the classroom, complete lessons sequentially, and see lesson-based phase and overall progress recalculate; if Firestore lesson docs are inconsistent, the repository reconciles them back to the canonical sequential state.
- Unlock more value: overall progress unlocks the advanced areas, and referred users converting through Phase 1 increase affiliate stats.
- Retention loop: Home, My Learning, and Profile help the user resume the journey, review completed phases, and share referral codes.

# Data & State

- Firestore collections in active use are `users`, `phases`, `bookings`, `referralEvents`, and `affiliateStats`.
- `users/{userId}` maps to the `User` model: `id`, `email`, `name`, `photoUrl`, `progress`, `phaseProgress`, `unlockedPhases`, `completedPhases`, `unlockedFeatures`, `referralCode`, and `referredBy`.
- Lesson completion is stored in `users/{userId}/progress/{phaseId}/lessons/{lessonId}` with an `isCompleted` flag.
- Raw lesson completion docs are treated as input, not truth. `PhaseRepository` canonicalizes them by lesson order, keeps only the contiguous completed prefix, and writes repaired `isCompleted` values back when it detects invalid or out-of-order states.
- Denormalized user fields such as `phaseProgress`, `completedPhases`, `progress`, and `unlockedFeatures` are recomputed from canonical lesson completion rather than trusted blindly from stale user-document values.
- Overall progress is not a flat lesson-total percentage. `ProgressCalculator` gives each phase equal weight, then fills that phase's share based on completed lessons inside it.
- `phases/{phaseId}` stores catalog metadata such as title, level, type, pricing, start date, visibility, and seat counts. If Firestore is empty or stale, the app can fall back to local `PhaseCatalog`, and first booking transactions can bootstrap missing phase documents.
- `bookings/{userId}_{phaseId}` stores phone/WhatsApp details, timestamps, expiry, and booking status. Legacy `booked` status is normalized to `approved` on read.
- `referralEvents/{referrerId}_{referredUserId}` store `joined` vs `converted` states plus timestamp.
- `affiliateStats/{userId}` stores only `totalInvites` and `conversions`.
- Lesson bodies themselves are local app data, not remote content. Phase 1 has 4 lessons, and Phases 2 to 6 currently have 3 lessons each.
- `ClassroomFragment` and `LessonDetailFragment` share a classroom-scoped `ClassroomViewModel` through the classroom navigation back stack entry instead of using activity-wide lesson state.
- State flow is mostly client-driven: fragments talk to viewmodels, viewmodels call repositories, repositories run Firestore transactions or listeners, and UI updates are delivered through LiveData or Flow-backed streams.
- Firestore security rules carry a large share of enforcement: owner-only user writes, seat-counter constraints, booking status transitions, and admin-only global access.

# Integrations

- Firebase Auth: used for Google sign-in, email/password registration, session state, and password reset.
- Google Sign-In (`play-services-auth`): used to obtain the Google ID token for Firebase login.
- Cloud Firestore: used for user profiles, phase catalog, bookings, lesson progress, referral events, affiliate stats, and admin notification snapshots.
- Firestore offline cache: enabled in `FirestoreAppStore` for local-first reads and resilience.
- Meta Android SDK: initialized in `TradingAIApplication`; debug builds emit `fb_mobile_test_event`, and advertiser ID collection is enabled through code and manifest metadata.
- Material 3: used for app theming, dialogs, cards, progress indicators, tabs, and navigation presentation.
- Glide: used for user avatar loading.
- Local Android notifications: used by `AdminNotificationManager` for on-device admin alerts driven by Firestore listeners.
- Declared but not meaningfully used yet: Firebase Functions, Firebase Storage, Firebase Messaging, and Firebase Analytics instrumentation beyond dependency inclusion.

# Current Implementation Status

- Complete: the Android app shell, bottom navigation, splash/auth routing, user profile persistence, phase catalog fallback/seeding, classroom lesson flow, canonical sequential progression repair, progress calculation, advanced feature gating, referral capture, and admin booking review are all implemented in code.
- Complete: premium cohorts are modeled with prices and seat limits, and the manual "Payment Received" workflow for premium phases is implemented in the Admin Panel.
- Complete: Firestore rules and client transactions cover the intended contract for users, phases, bookings, progress, and referrals.
- Complete: `MainActivity` requests notification permission and can start admin-only in-app booking alerts.
- Complete: `RegisterActivity` and `ForgotPasswordActivity` exist and are integrated into the auth surface.
- Complete: as of May 9, 2026, `app:testDebugUnitTest` passes locally, covering progress calculation, sequential lesson resolution, and core prerequisite logic in `PhaseRepository`.
- Partial: email/password registration exists, but there is no matching email/password login path in `LoginActivity`; the login screen is still Google-only.
- Partial: the advanced gating shell is real, but `InstallFragment` is static guidance only and `InvestFragment` is an empty-list placeholder with no adapter or data source.
- Partial: `AffiliateFragment` is functional for viewing and sharing, but there is no reward logic, payout logic, or admin-side affiliate management.
- Partial: `EducationFragment` exists and can show free vs premium tabs, but the main bottom navigation uses `PhasesFragment`, so `EducationFragment` is effectively a secondary flow.
- Partial: automated coverage exists for utilities and prerequisite logic, but overall test coverage is still thin and there are no instrumentation or end-to-end flows in this repo.
- Missing: automated server-side booking lifecycle management, scheduled expiration cleanup, push notifications via FCM, payment gateway integration, and role/claims-based admin management.
- Missing: remote lesson/content management, a real trading bot install/download flow, a real investment/community product flow, and any backend logic for wallet/web3 execution despite the template docs in `docs/web3_integration_template.md`.

# Known Issues / Gaps

- Admin authorization is hardcoded to `sushen.biswas.aga@gmail.com` across app code, docs, and Firestore rules instead of using roles or custom claims.
- The booking dialog currently collects only a WhatsApp number, then passes that same value as both `phoneNumber` and `whatsappNumber`, so the data model, privacy policy, and UI are out of sync.
- Booking expiration is still client/lazy driven. A pending request is normalized to `expired` when the owning user reloads bookings, but there is no trusted backend scheduler to release seats globally if the user never returns.
- Cohort `startDate` is informational only. The UI may show an upcoming date, but repository logic does not block early booking or early classroom unlocks based on start time.
- Referral codes are derived from the last 6 characters of the Firebase UID, but uniqueness is not enforced even though the docs describe them as unique.
- The affiliate feature gate is inconsistent: the full Affiliate screen requires 100 percent progress, but Profile already exposes the user's referral code and top-line invite/conversion stats.
- Documentation is partially out of sync with the app: some docs still mention fields such as `role` and `createdAt`, README/setup copy is still Google-first, and the privacy policy text does not acknowledge email/password auth even though registration exists.
- Several Firebase dependencies are included but not wired into product behavior, which increases maintenance overhead and can mislead future contributors.
- `EducationFragment` appears underused relative to `PhasesFragment`, creating duplicate course-browsing concepts in the codebase.
- `InstallFragment` has a visible GitHub download button but no click handler, and `InvestFragment` has a RecyclerView layout with no adapter or loading logic.
- Lesson content is hardcoded in Kotlin providers, so non-developers cannot update curriculum, copy, or sequencing without shipping a new app version.
- Build/test setup still has some friction: the project builds with Java 17, but `compileSdk = 35` on Android Gradle Plugin 8.5.1 emits a compatibility warning during Gradle runs.
- Meta App ID and client token are stored directly in app resources, which is operationally convenient but not ideal for configuration hygiene.

# Growth & Monetization Opportunities

- The core premium cohort model is already in place, with tiered prices from Phase 2 to Phase 6 and built-in seat scarcity that can support waitlists, launch windows, and cohort countdown campaigns.
- Referral capture is already implemented at signup, so the next leverage point is turning referral codes into a real incentive system with discounts, bonus lessons, cash commissions, or mentorship perks.
- The free Phase 1 to paid cohort transition is a strong conversion funnel; adding instrumentation around signup, booking, approval, and first lesson completion would make it measurable.
- Advanced feature gating creates natural upsell points for paid add-ons such as bot setup support, private research groups, premium templates, or deeper cohort access.
- Manual WhatsApp outreach can become a high-touch sales channel for premium cohorts, especially if paired with follow-up scripts, reminders, and admin conversion metrics.
- Profile already exposes referral identity before the 100 percent gate, which could support an earlier ambassador program even before the full affiliate dashboard is unlocked.
- Static lesson content can evolve into premium downloadable assets, cohort handouts, or template packs once content management is externalized.
- Meta attribution plus referral loops create the basis for acquisition optimization, but the app still needs clearer event tracking to optimize spend and conversion.

# Next Best Actions (IMPORTANT)

- Replace hardcoded admin identity and client-only lifecycle logic with backend authority: add role-based admin control, Cloud Functions for booking expiration and approval side effects, and FCM for real push notifications.
- Finish the broken or incomplete product paths: add email/password login, collect separate phone and WhatsApp fields, enforce real cohort start rules, and either fully implement or remove the placeholder `Install`, `Invest`, and duplicate `Education` flows.
- Externalize and instrument the growth funnel: move frequently changing curriculum/config out of the app binary, sync docs and privacy copy with the real data contract, and add analytics around signup, referral, booking, approval, and lesson completion.
