# AI Context

## 0. Change Log

- **Initial Context Generation**: Synthesized codebase state after package migration (`com.shaplachottor.lab` -> `com.shaplachottor.app`).
- **Architecture Mapping**: Identified MVVM-ish structure using `AppGraph` for dependency injection.
- **Feature Audit**: Documented Classroom, Phases, Admin Panel, and Meta SDK integration status.
- **Package Refactoring**: Verified all files moved to `.app` package and build is stable.

---

## 1. Project Overview

- **Name**: Shapla Chottor (Mobile Trading AI Version Three)
- **Core Purpose**: An educational platform focused on AI and Trading. It delivers structured learning content (Phases/Lessons) with a manual seat booking/approval mechanism.
- **Target Audience**: Students/Learners interested in AI-driven coding and trading.

---

## 2. Tech Stack

- **Languages**: Kotlin (100%)
- **UI Framework**: Android XML with ViewBinding.
- **Backend/Services**: 
    - **Firebase**: Authentication (Google Sign-In), Firestore (Database), Storage, Analytics, Messaging.
    - **Meta SDK**: App Events, Install Tracking, Advertiser ID collection.
- **Libraries**:
    - **Glide**: Image loading.
    - **Navigation Component**: In-app routing via `nav_graph.xml`.
    - **Coroutines/Lifecycle**: Asynchronous operations and scope management.

---

## 3. Architecture

- **Pattern**: MVVM (Model-View-ViewModel) / Repository pattern.
- **Dependency Management**: Centralized `AppGraph.kt` acting as a Service Locator for providers and stores.
- **Data Flow**: UI (Fragments) ↔ ViewModels ↔ Repositories ↔ Firestore/Auth.
- **Initialization**: `TradingAIApplication` handles Firebase and Meta SDK setup.

---

## 4. Implemented Features

### 4.1 Authentication
- ✅ **Google Sign-In**: Primary login method in `LoginActivity`.
- ✅ **Session Management**: Handled via `AuthSessionProvider`.

### 4.2 Learning Management System (LMS)
- ✅ **Phases**: Grouped lessons (Beginner, Intermediate, Advanced) in `PhasesFragment`.
- ✅ **Classroom**: Interactive lesson list for unlocked phases in `ClassroomFragment`.
- ✅ **Lesson Tracking**: Marking lessons as complete with progress persistence in Firestore.

### 4.3 Booking & Approval System
- ✅ **Seat Requests**: Users must request access to phases (manual approval flow).
- ✅ **Expiration**: Requests expire (mentioned 15-minute window in UI logic).
- ✅ **Admin Panel**: Restricted fragment for approving/rejecting booking requests.

### 4.4 Meta SDK Integration
- ✅ **Auto-Init**: Enabled in `TradingAIApplication`.
- ✅ **Event Logging**: `SplashActivity` logs `TEST_EVENT_NOW`.
- ⚠️ **Key Hash**: Runtime printing utility implemented to confirm Meta Dashboard configuration.

---

## 5. Key Files & Responsibilities

- `TradingAIApplication.kt`: App-level initialization (Firebase, Meta SDK) and Key Hash logging.
- `AppGraph.kt`: Centralized access to `AuthSessionProvider` and `AppStore`.
- `SplashActivity.kt`: Entry point, logs Meta test events, and routes to Main or Login.
- `PhasesFragment.kt`: Main course catalog; handles tab filtering and booking dialogs.
- `ClassroomFragment.kt`: Content delivery; tracks lesson completion progress.
- `AdminPanelFragment.kt`: Manual approval interface (Hardcoded admin: `sushen.biswas.aga@gmail.com`).
- `UserRepository.kt` / `PhaseRepository.kt`: Data abstraction layers for Firestore operations.

---

## 6. Current System Behavior (User Flow)

1. **Launch**: `SplashActivity` starts → Logs `TEST_EVENT_NOW` to Meta → Checks Auth.
2. **Auth**: If not logged in, `LoginActivity` handles Google Sign-In.
3. **Browse**: User views `PhasesFragment`. Phases are locked by default unless `unlockedPhases` contains the ID.
4. **Booking**: User clicks a locked phase → Submits Phone/WhatsApp via `DialogBookingRequest`.
5. **Approval**: Admin sees request in `AdminPanelFragment` → Approves → Phase ID added to User's `unlockedPhases`.
6. **Learn**: User enters `ClassroomFragment` → Toggles lessons → Progress bar updates.

---

## 7. Integrations & External Dependencies

- **Firebase Auth**: Google provider configured.
- **Firestore**: Collections for `users`, `phases`, `bookings`.
- **Meta (Facebook) SDK**:
    - Status: **Partial**. Initialization and test logging work.
    - Pending: Real production event mapping (beyond test events).

---

## 8. Known Issues / Weaknesses

### Functional Issues
- **Hardcoded Admin**: Admin access is hardcoded to a specific email in `AdminPanelFragment`.
- **Manual Flow**: The 15-minute approval window might be tight for manual operations.

### Structural Issues
- **Naming Inconsistency**: Repositories are split between `repository` and `repositories` packages.
- **Navigation Coupling**: Some navigation calls are direct (R.id) while others use SafeArgs.

### Analytics / Tracking Issues
- **Test Artifacts**: `TEST_EVENT_NOW` is logged on every splash, which will pollute production analytics.
- **Debug Mode**: Meta SDK debug mode is explicitly enabled in production code.

---

## 9. Gaps & Missing Capabilities

- **Video Player**: Lessons currently just show a Toast when clicked; no media player integrated.
- **Notification Sync**: Admin notifications are initiated but full FCM flow for user approval alerts is unclear.
- **Automated Expiry**: Booking expiration logic appears to be UI-text based; need to verify background cleanup.

---

## 10. Next Recommended Features

1. **Centralized Analytics Manager**: Abstract Meta/Firebase event logging into a single service.
2. **Dynamic Admin Roles**: Move admin identification from hardcoded email to a Firestore role/flag.
3. **Lesson Content Viewer**: Implement a Fragment or Activity to actually display lesson content (Video/Text).
4. **Automated Booking Approval**: (Optional) Rule-based approval for beginner phases to reduce admin load.

---

## 11. Development State Summary

- **Completion Estimate**: 75% (Core infra stable, content delivery pending).
- **Stage**: Pre-production / Beta.
- **System Readiness**: 
    - **Architecture**: Stable (MVVM).
    - **Stability**: High (Builds and runs).
    - **Tracking**: Functional but needs cleanup.
