# ShaplaChottor Mobile App
**AI Research Lab - Learn. Build. Trade.**

A premium Android application focused on AI-driven trading education and progressive feature unlocks.

## Tech Stack
- **Language:** Kotlin
- **Architecture:** MVVM + Repository Pattern
- **UI:** Material 3, ViewBinding, Navigation Component
- **Backend:** Firebase (Google Auth, Firestore, Storage, Analytics)
- **Package Name:** `com.shaplachottor.app`

## Core Experience
1. **Google-First Authentication:** Single-click sign-in with your Google Account.
2. **Education-First Journey:** A fixed 6-phase learning path from Beginner to Advanced.
3. **Approval-Based Access:** A phase stays locked until the user submits a seat request and an admin approves it.
4. **Progress-Based Gating:** Advanced tools unlock only as overall progress increases.
5. **Professional UI:** Clean, branded Android experience under the ShaplaChottor identity.

## Six-Phase System
The app uses this canonical Firestore-backed phase catalog:

1. `phase1` - Foundations
2. `phase2` - Data Analysis
3. `phase3` - Object-Oriented Programming
4. `phase4` - System Design
5. `phase5` - Simulation & Data Systems
6. `phase6` - Production Engineering

Each phase document contains:
- `phaseId`
- `title`
- `description`
- `level`
- `order`
- `totalSeats`
- `bookedSeats`

If the `phases` collection is empty, the app seeds these 6 canonical phase documents after a signed-in user enters the app.

## Setup Instructions

### 1. Firebase Configuration
1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.shaplachottor.app`.
3. Add your SHA-1 and SHA-256 signing fingerprints.
4. Download `google-services.json` and place it in the `app/` directory.
5. Enable the **Google** sign-in provider.
6. Copy the Google Web client ID from Firebase Auth settings.

### 2. Local Configuration
Update `app/src/main/res/values/strings.xml`:

```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID_HERE</string>
```

### 3. Firestore Rules
Deploy the repository's `firestore.rules` file before testing bookings and progress updates.

### 4. Cloud Functions
Deploy the booking notification backend in `functions/` if you want the admin email alert flow.
See [docs/firebase_functions_setup.md](/C:/Users/user/StudioProjects/MobileTradingAi/docs/firebase_functions_setup.md:1) for SMTP and secret setup.

### 5. Firestore Collections
The app relies on these collections:

#### `users`
- `id`: String
- `name`: String
- `email`: String
- `role`: String
- `progress`: Number
- `phaseProgress`: Map<String, Number>
- `unlockedFeatures`: Map
- `unlockedPhases`: Array<String>
- `completedPhases`: Array<String>

Default user state:
- `progress = 0`
- `phaseProgress = {}`
- `unlockedPhases = []`
- `completedPhases = []`

#### `phases`
- `phaseId`: String
- `title`: String
- `description`: String
- `level`: String
- `order`: Number
- `totalSeats`: Number
- `bookedSeats`: Number

#### `bookings`
- `bookingId`: String (`{userId}_{phaseId}`)
- `userId`: String
- `phaseId`: String
- `phoneNumber`: String
- `whatsappNumber`: String
- `status`: String (`pending`, `approved`, `expired`)
- `createdAt`: Number
- `expiresAt`: Number

## Feature Gating Logic
- Overall progress is calculated across all 6 phases.
- **Bot Setup** unlocks at **30%** overall progress.
- **Investment Features** unlock at **60%** overall progress.
- **Affiliate System** unlocks at **100%** overall progress.

## Build & Run
1. Open the project in **Android Studio**.
2. Sync Gradle.
3. Deploy `firestore.rules`.
4. Deploy the Cloud Function in `functions/` if you need admin email notifications.
5. Run the app.
6. Sign in with Google and verify that booking requests are written to `bookings`, remain pending until admin approval, notify the admin by email, and only unlock phases after approval.

---
(c) 2026 ShaplaChottor AI Research Lab
