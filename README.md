# ShaplaChottor Mobile App
**AI Research Lab - Learn. Build. Trade.**

A premium Android application focused on AI-driven trading education and advanced algorithmic tools.

## Tech Stack
- **Language:** Kotlin
- **Architecture:** MVVM + Repository Pattern
- **UI:** Material 3, ViewBinding, Navigation Component
- **Backend:** Firebase (Google Auth, Firestore, Storage, Analytics)
- **Package Name:** `com.shaplachottor.app`

## Core Experience
1. **Google-First Authentication:** Single-click sign-in with your Google Account for seamless access.
2. **Education-First:** A structured learning path from Beginner to Advanced AI trading.
3. **Feature Gating:** Advanced tools (Bot Install, Investments, Affiliates) unlock as you progress through the courses.
4. **Professional UI:** Clean, modern design under the ShaplaChottor brand.
5. **Adaptive Branding:** High-resolution vector logos and adaptive icons for all Android devices.

## Setup Instructions

### 1. Firebase Configuration
1. Create a new project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.shaplachottor.app`.
3. **IMPORTANT:** Provide your SHA-1 certificate for Google Sign-In.
4. Download the `google-services.json` file and place it in the `app/` directory.
5. Enable **Google** sign-in provider (Email/Password is no longer used).
6. Copy your **Web client ID** from the Google provider settings.

### 2. Local Configuration
Update `app/src/main/res/values/strings.xml` with your Web Client ID:
```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID_HERE</string>
```

### 3. Signing Fingerprints
Ensure your debug and release SHA-1 fingerprints are added to the Firebase project settings to enable Google Authentication.

### 4. Firestore Structure
The app expects a `users` collection with the following document structure:
- `name`: String
- `progress`: Number (0-100)
- `role`: String ("student")
- `enrolledCourses`: Array

### 4. Build & Run
1. Open in **Android Studio**.
2. **Sync Project with Gradle Files**.
3. Build and Run.

## Feature Gating Logic
- **Bot Setup**: Unlocks at 30% course progress.
- **Investments**: Unlocks at 60% course progress.
- **Affiliate System**: Unlocks at 100% course progress.

---
© 2026 ShaplaChottor AI Research Lab
