# Shapla Chottor Mobile App
**AI Research Lab - Learn. Build. Code.**

A premium Android application focused on AI-driven coding education, structured learning cohorts, and progressive feature unlocks.

## Tech Stack
- **Language:** Kotlin (2.1.0)
- **Architecture:** MVVM + Repository Pattern
- **UI:** Material 3, ViewBinding, Navigation Component
- **Backend:** Firebase (Google Auth, Firestore, Analytics)
- **Integrations:** Meta Android SDK (App Events & Attribution)
- **Package Name:** `com.shaplachottor.lab`

## Core Experience
1. **Google-First Authentication:** Single-click sign-in with your Google Account, supporting referral attribution.
2. **Tiered Learning Journey:** A fixed 6-phase path. Free foundational "Phases" transition into premium "Cohorts" for advanced modules.
3. **Monetized Enrollment:** Premium cohorts include pricing metadata and scheduled start dates.
4. **Approval-Based Access:** Access stays locked until the user submits a seat request. For premium cohorts, admins verify payment before manual approval.
5. **Sequential Gating:** Prerequisite enforcement ensures students master fundamentals (Phase 1) before progressing.
6. **Real-time Seat Management:** Limited enrollment slots with atomic seat reservation logic.
7. **Progress-Based Feature Unlocks:** Advanced tools like the AI Bot and Affiliate system unlock as course progress increases.
8. **Affiliate Growth Loop:** Users earn a referral code and track invites and conversions via a dedicated dashboard.

## Learning Journey (Synced with Web)
The curriculum follows a structured 6-phase catalog:

### Beginner Level
- **Phase 1: Foundations (FREE)** - Core programming fundamentals and logical thinking. The entry point for all students.
- **Phase 2: Data Analysis (COHORT)** - Master practical data analysis for AI and trading workflows.

### Intermediate Level
- **Phase 3: Object-Oriented Programming (COHORT)** - Build reusable systems and strong architecture.
- **Phase 4: System Design (COHORT)** - Design scalable services and robust backend flows.

### Advanced Level
- **Phase 5: Simulation & Data Systems (COHORT)** - Build simulation pipelines for model-backed decisions.
- **Phase 6: Production Engineering (COHORT)** - Ship production-grade AI workflows with reliability.

## Enrollment Workflow
- **LOCKED**: Initial state. User must meet prerequisites to request access.
- **PENDING**: Seat is reserved. For premium cohorts, the student awaits admin outreach for payment verification.
- **UNLOCKED**: Access granted. The classroom and all its lessons are now available.
- **TIMER**: Pending requests expire after 15 minutes if not acted upon, releasing the seat back to the pool.

## Admin Features
Access the Admin Panel via the Profile screen (authorized admins only).

- **Booking Review**: Filter by pending or all requests with user details and contact info.
- **Payment Verification**: Dedicated confirmation dialog for premium requests to ensure funds are received.
- **Manual Approval**: One-tap approval to unlock content and trigger progress tracking.
- **Communication**: Integrated WhatsApp and clipboard actions for student follow-up.

## Setup Instructions

### 1. Firebase Configuration
1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.shaplachottor.lab`.
3. Add your SHA-1 and SHA-256 fingerprints.
4. Download `google-services.json` and place it in the `app/` directory.
5. Enable **Google** sign-in and **Firestore** (with local persistence).

### 2. Meta SDK Integration
1. Configure Meta App ID and Client Token in `AndroidManifest.xml` (or via Gradle properties).
2. Ensure `fb_mobile_test_event` logs successfully on splash to verify attribution.

### 3. Firestore Seed
The app automatically seeds the canonical phase catalog when the lead admin (`sushen.biswas.aga@gmail.com`) first opens the app.

## Feature Unlock Thresholds
- **AI Bot Setup**: Unlocks at **30%** overall progress.
- **Investment Dashboard**: Unlocks at **60%** overall progress.
- **Affiliate Dashboard**: Unlocks at **100%** overall progress.

---
(c) 2026 ShaplaChottor AI Research Lab
