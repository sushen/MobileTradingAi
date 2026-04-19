# AI Context

## 1. Project Overview

* Android app name: `ShaplaChottor`; Gradle root project name: `TradingAI`.
* Package/application ID: `com.shaplachottor.app`.
* Core purpose from README, docs, resources, and code: a mobile gateway for AI trading education, progressive learning phases, manual seat booking/approval, and later gated access to bot setup, investment, and affiliate areas.
* The app does not contain trading execution logic. Existing docs describe it as a gateway ecosystem for a Windows Trading AI bot.
* Current repo state is incomplete and not buildable without restoring missing source/resources and Firebase configuration.

## 2. Tech Stack

* Language: Kotlin.
* Platform: Android native app.
* UI approach: XML layouts with ViewBinding, Material Components/Material 3 styling, RecyclerView, BottomNavigationView, Navigation Component.
* Architecture dependencies: AndroidX Lifecycle ViewModel, LiveData, `viewModelScope`.
* Backend dependencies: Firebase Auth, Firestore, Storage, Analytics through Firebase BoM.
* Auth-related dependency: Google Play Services Auth.
* Async support: Kotlin coroutines and `kotlinx-coroutines-play-services`.
* Image loading dependency: Glide.
* Build tools: Android Gradle Plugin `8.5.1`, Kotlin Android plugin `2.1.0`, Java 17 target, compileSdk/targetSdk `34`, minSdk `24`.
* Gradle wrapper metadata points to Gradle `9.0.0`.

## 3. Architecture

* Intended pattern: MVVM plus Repository pattern.
* UI layer:
  * `PhasesFragment` and `ClassroomFragment` render screens and observe ViewModel LiveData.
  * `PhaseAdapter` and `LessonAdapter` render RecyclerView items.
* ViewModel layer:
  * `PhaseViewModel` loads phases, filters by level, exposes booking state, and delegates seat requests.
  * `ClassroomViewModel` enforces phase access, loads phase lessons, fetches current user progress, and toggles lesson completion.
  * `MyLearningViewModel` listens to the current Firestore user document and resolves current/completed phases.
* Repository/data layer:
  * `PhaseRepository` owns phase seeding, phase lookup, hardcoded lesson lists, progress updates, booking requests, booking approval/expiration helpers, and phase-access checks.
  * `PhaseRepository` depends on `AppGraph`, `AppStore`, `AuthSessionProvider`, and `PhaseCatalog`, but those classes are not present in the current repo.
* Data flow currently intended by code:
  * Fragment -> ViewModel -> `PhaseRepository` -> missing `AppStore`/Firebase-backed store.
  * Some ViewModels bypass the repository and call `FirebaseAuth`/`FirebaseFirestore` directly, so data access is inconsistent.
* Navigation:
  * `activity_main.xml` hosts a `NavHostFragment` using `nav_graph.xml`.
  * `bottom_nav_menu.xml` defines Home, Courses/Phases, My Learning, Advanced, and Profile tabs.

## 4. Implemented Features

* Learning phase model
  * Completed: `Phase` model with title, description, focus, outcome, identity shift, level, order, seats, lock state, `availableSeats`, and `isAvailable`.
  * Partial: canonical phase source is referenced as `PhaseCatalog`, but `PhaseCatalog` is missing.

* Booking model and booking request logic
  * Completed: `Booking` model supports `pending`, `approved`, `expired`, legacy `booked`, 15-minute expiration, contact fields, and booking IDs.
  * Completed in repository logic: blank contact validation, duplicate pending/approved checks, seat availability checks, pending booking creation.
  * Partial: actual persistence depends on missing `AppStore`; admin approval UI/backend caller is not present.

* Phase list screen
  * Completed in Kotlin: `PhasesFragment`, `PhaseViewModel`, and `PhaseAdapter` implement level filtering, seat status display, booking status display, booking dialog flow, and navigation to classroom for unlocked phases.
  * Partial/blocking: required XML resources are missing: `fragment_phases.xml`, `dialog_booking_request.xml`, `item_phase.xml`, and lock drawables `ic_lock_open`/`ic_lock_closed`.
  * Partial/blocking: `User`, `UserRepository`, `BookingRequestOutcome`, and `BookingRequestResult` are referenced but missing.

* Classroom screen
  * Completed in Kotlin: `ClassroomFragment`, `ClassroomViewModel`, `ClassroomViewModelFactory`, and `LessonAdapter` implement access check, lesson list rendering, progress display, lesson completion toggles, and completion dialog.
  * Partial/blocking: required XML resources are missing: `fragment_classroom.xml` and `item_lesson.xml`.
  * Partial: lessons are hardcoded in `PhaseRepository.getLessonsForPhase`; lesson completion is not tracked per lesson ID, only by phase progress increments.

* Progress and feature gating
  * Completed in repository logic: phase progress changes in thirds based on 3 hardcoded lessons; completed phases list updates at 100%; overall progress averages all canonical phases; feature flags unlock at 30%, 60%, and 100%.
  * Partial/blocking: depends on missing `User`, `PhaseCatalog`, and `AppStore`.
  * Weakness: `lessonId` is accepted but not used to prevent duplicate progress increments.

* Static UI/resources
  * Completed as XML only: login, register, forgot password, main shell, home, education, install, invest, affiliate, profile, course item, investment plan item.
  * Partial/blocking: corresponding activities/fragments for most of these layouts are missing.

* Firebase and Firestore setup documentation
  * Completed: `README.md`, `docs/firebase_setup.md`, `docs/firestore_database_structure.md`, `docs/web3_integration_template.md`, and `firestore.rules` document Firebase setup, collections, rules, booking behavior, Web3 principles, and feature gating.
  * Partial: README references `functions/` and `docs/firebase_functions_setup.md`, but neither exists in the current repo.

* Authentication
  * Partial/documented: README and login layout indicate Google-first authentication.
  * Blocking: `SplashActivity`, `LoginActivity`, `RegisterActivity`, `ForgotPasswordActivity`, `TradingAIApplication`, and auth repositories/session implementation are missing.

* Web3/investment/affiliate/admin
  * Partial/documented/static only: docs and XML layouts describe or display these areas.
  * Not implemented in current Kotlin source: wallet connection, transaction submission, investment history, affiliate tracking logic, admin dashboard, course management, and investment enable/disable controls.

## 5. Key Files & Responsibilities

* `settings.gradle` -> declares root project `TradingAI` and includes `:app`.
* `build.gradle` -> root plugin versions for Android, Kotlin, Google Services, and Navigation Safe Args.
* `gradle.properties` -> AndroidX and Gradle JVM settings.
* `gradle/wrapper/gradle-wrapper.properties` -> Gradle distribution configuration.
* `app/build.gradle` -> Android app config, SDK levels, ViewBinding, Firebase, Navigation, Lifecycle, Material, Glide, and test dependencies.
* `app/src/main/AndroidManifest.xml` -> declares Internet permission, app class, and activity entry points.
* `app/src/main/res/navigation/nav_graph.xml` -> navigation graph for home, phases, classroom, education, my learning, advanced, profile, install, invest, and affiliate fragments.
* `app/src/main/res/menu/bottom_nav_menu.xml` -> bottom navigation tab definitions.
* `app/src/main/res/values/strings.xml` -> app labels, risk disclaimer, gating strings, booking hints, placeholder Google web client ID.
* `app/src/main/res/values/colors.xml` -> brand and UI color tokens.
* `app/src/main/res/values/themes.xml` and `values-night/themes.xml` -> Material 3 DayNight theme definitions.
* `app/src/main/res/layout/activity_main.xml` -> main NavHost plus bottom navigation shell.
* `app/src/main/res/layout/activity_login.xml` -> Google sign-in UI only.
* `app/src/main/res/layout/activity_register.xml` -> email/password registration UI only.
* `app/src/main/res/layout/activity_forgot_password.xml` -> password reset UI only.
* `app/src/main/res/layout/fragment_home.xml` -> static home/progress/advanced tools screen layout.
* `app/src/main/res/layout/fragment_education.xml` -> static courses list shell.
* `app/src/main/res/layout/fragment_install.xml` -> static Windows bot installation guide.
* `app/src/main/res/layout/fragment_invest.xml` -> static investment plans RecyclerView shell.
* `app/src/main/res/layout/fragment_affiliate.xml` -> static affiliate dashboard layout.
* `app/src/main/res/layout/fragment_profile.xml` -> static profile/logout layout.
* `app/src/main/res/layout/item_course.xml` -> static course card item layout.
* `app/src/main/res/layout/item_investment_plan.xml` -> static investment plan card item layout.
* `app/src/main/java/com/shaplachottor/app/models/AdvancedFeatures.kt` -> feature unlock flags.
* `app/src/main/java/com/shaplachottor/app/models/Booking.kt` -> booking request model and booking status constants.
* `app/src/main/java/com/shaplachottor/app/models/Lesson.kt` -> lesson model.
* `app/src/main/java/com/shaplachottor/app/models/Phase.kt` -> learning phase model and seat availability helpers.
* `app/src/main/java/com/shaplachottor/app/repositories/PhaseRepository.kt` -> phase, lesson, progress, booking, approval, expiration, and access-control business logic.
* `app/src/main/java/com/shaplachottor/app/viewmodels/PhaseViewModel.kt` -> LiveData state for phase listing/filtering and booking result.
* `app/src/main/java/com/shaplachottor/app/viewmodels/ClassroomViewModel.kt` -> LiveData state for classroom access, phase, lessons, and user progress.
* `app/src/main/java/com/shaplachottor/app/viewmodels/ClassroomViewModelFactory.kt` -> factory for injecting `PhaseRepository` into `ClassroomViewModel`.
* `app/src/main/java/com/shaplachottor/app/viewmodels/MyLearningViewModel.kt` -> Firestore-backed current/completed learning state.
* `app/src/main/java/com/shaplachottor/app/fragments/PhasesFragment.kt` -> phase tabs, phase list rendering, booking request dialog flow, and classroom navigation.
* `app/src/main/java/com/shaplachottor/app/fragments/ClassroomFragment.kt` -> classroom rendering, access-denied handling, progress display, and lesson completion.
* `app/src/main/java/com/shaplachottor/app/adapters/PhaseAdapter.kt` -> RecyclerView item binding for phases, seat counts, lock state, and booking state.
* `app/src/main/java/com/shaplachottor/app/adapters/LessonAdapter.kt` -> RecyclerView item binding for lessons and completion checkbox.
* `firestore.rules` -> Firestore access rules for users, phases, bookings, investments, courses, enrollments, affiliate stats, and settings.
* `docs/firebase_setup.md` -> Firebase setup and default user state instructions.
* `docs/firestore_database_structure.md` -> expected Firestore collections and fields.
* `docs/web3_integration_template.md` -> non-implemented Web3 flow template.
* `README.md` -> high-level product intent, setup, six-phase catalog, and feature-gating documentation.

## 6. Current System Behavior

* Verified runtime behavior: none. The app cannot currently be built/launched from this repo state.
* Build verification:
  * `.\gradlew.bat :app:assembleDebug` fails immediately with `Error: -classpath requires class path specification` because `gradlew.bat` currently sets `CLASSPATH=` instead of the wrapper jar path.
  * `java -jar gradle\wrapper\gradle-wrapper.jar :app:assembleDebug` reaches Gradle but fails at `:app:processDebugGoogleServices` because `app/google-services.json` is missing.
* Intended flow from manifest/navigation/layouts/code:
  * Launcher activity should be `SplashActivity`.
  * Auth should route through login/register/forgot password screens.
  * Main app should show bottom navigation with Home, Courses/Phases, My Learning, Advanced, and Profile.
  * Phases screen should show Beginner/Intermediate/Advanced tabs and phase cards.
  * Locked phases should show booking CTA; booking requires phone and WhatsApp numbers.
  * Pending booking should show approval expiry time.
  * Approved/unlocked phases should open classroom.
  * Classroom should show hardcoded lessons and update phase progress when lesson checkboxes change.
  * Progress should unlock advanced feature flags at 30%, 60%, and 100%.

## 7. Known Issues / Weaknesses

* Project is not buildable in current state.
* `gradlew.bat` is modified/broken: `CLASSPATH` is empty.
* `app/google-services.json` is missing, so Google Services processing fails.
* Manifest references missing classes:
  * `.TradingAIApplication`
  * `com.shaplachottor.app.activities.SplashActivity`
  * `MainActivity`
  * `LoginActivity`
  * `RegisterActivity`
  * `ForgotPasswordActivity`
* Navigation graph references missing fragment classes:
  * `HomeFragment`
  * `EducationFragment`
  * `MyLearningFragment`
  * `AdvancedFragment`
  * `ProfileFragment`
  * `InstallFragment`
  * `InvestFragment`
  * `AffiliateFragment`
* Navigation graph references missing layouts:
  * `fragment_phases`
  * `fragment_classroom`
  * `fragment_my_learning`
  * `fragment_advanced`
* Kotlin ViewBinding references missing layouts:
  * `FragmentPhasesBinding`
  * `DialogBookingRequestBinding`
  * `FragmentClassroomBinding`
  * `ItemPhaseBinding`
  * `ItemLessonBinding`
* Kotlin references missing source types:
  * `AppGraph`
  * `AppStore`
  * `AuthSessionProvider`
  * `PhaseCatalog`
  * `User`
  * `UserRepository`
  * `BookingRequestOutcome`
  * `BookingRequestResult`
* `PhaseAdapter` references missing drawables:
  * `ic_lock_open`
  * `ic_lock_closed`
* `README.md` references `functions/` and `docs/firebase_functions_setup.md`, but they are not present.
* `.gitignore` ignores `*.xml` and `*.json` globally, which can hide new Android resource XML files and Firebase JSON config from git unless explicitly force-added or exceptioned.
* Repository catches broad `Exception` and often returns false/empty fallback values, which can hide data-layer failures.
* `ClassroomViewModel` and `MyLearningViewModel` directly use Firebase while `PhaseRepository` uses an abstract `AppStore`, creating inconsistent data-access patterns.
* `updateLessonProgress()` ignores `lessonId`, so checking the same lesson repeatedly can incorrectly increment/decrement progress.
* Lesson data is hardcoded and identical for every phase.
* Booking expiration is normalized only when bookings are read through repository methods; no scheduler/backend expiration is present.
* Admin approval logic exists only as repository methods; no admin UI, secure callable backend, or Cloud Function implementation is present.
* Firestore rules rely on `request.auth.token.role == 'admin'`; no code in repo sets custom claims.
* `PhaseRepository.approveBooking()` updates booking, phase seat count, and user access as separate store calls; atomicity depends on missing `AppStore` implementation and may be unsafe without a Firestore transaction.
* Auth UI layouts exist, but auth activities and account creation/sign-in logic are missing.
* Web3, investment, affiliate, and admin features are documented or static-layout only.

## 8. Next Recommended Features (Top 3-5)

* Buildable app foundation
  * Why it matters: no feature can be verified until wrapper, Firebase config handling, missing classes, and missing resources are resolved.
  * Complexity: High.

* Firebase/auth/data layer completion
  * Why it matters: `PhaseRepository`, `MyLearningViewModel`, bookings, progress, and phase access all depend on user/session/store abstractions that are absent.
  * Complexity: High.

* Complete learning phases and classroom UI
  * Why it matters: phase booking and classroom progress are the most developed business flows in current Kotlin, but they cannot render because required layouts/resources/models are missing.
  * Complexity: Medium.

* Admin booking approval workflow
  * Why it matters: user access depends on manual approval, but there is no present admin surface or backend function to approve bookings safely.
  * Complexity: High.

* Replace hardcoded lessons with Firestore-backed course/lesson data
  * Why it matters: current classroom progress is based on three static lessons for every phase and does not track individual lesson completion.
  * Complexity: Medium.

## 9. Development State Summary

* Estimated completion: 20-30% of the documented product vision.
* Stage: early/pre-alpha.
* Buildable: no.
* Most complete area: phase/booking/classroom business logic skeleton.
* Least complete areas: app shell/auth implementation, data layer, missing resources/classes, admin approval flow, Web3/investment/affiliate implementations.
* Production readiness: not near production. Primary next milestone should be a clean debug build with Firebase config supplied locally and a working login-to-phases-to-classroom happy path.
