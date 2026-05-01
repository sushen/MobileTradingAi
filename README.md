# ShaplaChottor Mobile App
**AI Research Lab - Learn. Build. Code.**

A premium Android application focused on AI-driven coding education and progressive feature unlocks.

## Tech Stack
- **Language:** Kotlin
- **Architecture:** MVVM + Repository Pattern
- **UI:** Material 3, ViewBinding, Navigation Component
- **Backend:** Firebase (Google Auth, Firestore, Storage, Analytics)
- **Package Name:** `com.shaplachottor.app`

## Core Experience
1. **Google-First Authentication:** Single-click sign-in with your Google Account.
2. **Education-First Journey:** A fixed 6-phase learning path from Beginner to Advanced, synced with the web platform.
3. **Approval-Based Access:** A phase stays locked until the user submits a seat request and an admin approves it.
4. **Prerequisite Gating:** Phases must be completed in order (e.g., Phase 1 must be unlocked to request Phase 2).
5. **Real-time Seat Management:** Limited seats (100 per phase) with real-time availability tracking.
6. **Progress-Based Feature Unlocks:** Advanced tools unlock only as overall journey progress increases.
7. **Admin Panel:** Built-in management tool for the lead admin (`sushen.biswas.aga@gmail.com`) to approve, reject, or cancel seat bookings.

## Learning Journey (Synced with Web)
The app uses the canonical 6-phase catalog defined in the AI Research Lab ecosystem:

### Beginner Level
- **Phase 1: Foundations** - Learn core programming fundamentals required for all future phases. Focus on building basic coding ability and logical thinking.
- **Phase 2: Data Analysis** - Master practical data analysis techniques for AI and trading workflows.

### Intermediate Level
- **Phase 3: Object-Oriented Programming** - Build reusable systems and strong architecture using OOP principles.
- **Phase 4: System Design** - Design scalable services and robust backend flows for production systems.

### Advanced Level
- **Phase 5: Simulation & Data Systems** - Build simulation pipelines and data systems for model-backed decisions.
- **Phase 6: Production Engineering** - Ship production-grade AI workflows with reliability and monitoring.

## Phase State Logic
- **LOCKED**: Initial state. User must request access (if prerequisites are met).
- **PENDING**: User has requested a seat. Wait for a WhatsApp call from our team.
- **UNLOCKED**: Admin approved. User has full access to the classroom content.
- **TIMER**: Pending requests include a live 15-minute countdown. If no call/approval occurs, the request expires.

## Admin Features
Access the Admin Panel by tapping the **Admin** chip in the Profile screen (visible only to authorized emails).

- **Pending Tab**: Review new requests with contact details (Phone/WhatsApp) and expiration timers.
- **Approval**: One-tap approval that instantly unlocks the classroom and updates the user's progress.
- **Rejection**: Deny requests that don't meet criteria.
- **Seat Cancellation**: Revoke access to free up seats and re-lock the phase.

## Setup Instructions

### 1. Firebase Configuration
1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.shaplachottor.app`.
3. Add your SHA-1 and SHA-256 signing fingerprints.
4. Download `google-services.json` and place it in the `app/` directory.
5. Enable the **Google** sign-in provider.

### 2. Local Configuration
The app automatically uses the Web Client ID found in `google-services.json`. Ensure `app/src/main/res/values/strings.xml` contains:
```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID_FROM_JSON</string>
```

### 3. Firestore Collections

#### `users`
Tracks user progress (`overallProgress`), unlocked phases, and profile details.

#### `phases`
Stores phase metadata, seat counts (`totalSeats`, `availableSeats`), and level descriptions.

#### `bookings`
- `status`: `pending`, `approved`, `rejected`, `cancelled`, `expired`
- `expiresAt`: 15-minute window for manual approval.

## Feature Gating Logic
- **AI Bot Setup**: Unlocks at **30%** progress.
- **Investment Dashboard**: Unlocks at **60%** progress.
- **Affiliate System**: Unlocks at **100%** progress.

---
(c) 2026 ShaplaChottor AI Research Lab
