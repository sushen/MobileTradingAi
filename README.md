# Trading AI Mobile App

A complete Android application for managing a trading AI ecosystem.

## Tech Stack
- **Language:** Kotlin
- **Architecture:** MVVM + Repository Pattern
- **UI:** Material 3, ViewBinding, Navigation Component
- **Backend:** Firebase (Auth, Firestore, Storage, Analytics)

## Features
- **Authentication:** Email/Password & Google Sign-in.
- **Home:** Introduction to the ecosystem.
- **Install Bot:** Step-by-step guide to installing the trading bot.
- **Investor Dashboard:** Browse and select investment plans.
- **Education:** Tiered courses (Beginner to Advanced) for algorithmic trading.
- **Affiliate System:** Referral links and earnings tracking.
- **Profile:** Account management and community links.

## Setup Instructions

### 1. Firebase Configuration
1. Create a new project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.tradingai.app`.
3. Download the `google-services.json` file and place it in the `app/` directory of this project.
4. Enable **Email/Password** and **Google** sign-in providers in the Firebase Authentication section.
5. Create a **Cloud Firestore** database in test mode (or configure rules).
6. Enable **Firebase Storage**.

### 2. Firestore Structure
Create the following collections:
- `users`: Stores user profiles.
- `investments`: Stores user investment data.
- `courses`: Stores educational content.
- `affiliates`: Stores referral data.

### 3. Build & Run
1. Open the project in **Android Studio**.
2. Sync the project with Gradle files.
3. Build the project: `Build > Make Project`.
4. Run the app on an emulator or physical device.

## Security Rules (Firestore)
```
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    match /investments/{investmentId} {
      allow read, write: if request.auth != null && resource.data.userId == request.auth.uid;
    }
    match /courses/{courseId} {
      allow read: if request.auth != null;
    }
  }
}
```

## Deployment
To generate a release APK:
1. Go to `Build > Generate Signed Bundle / APK...`
2. Follow the wizard to create a keystore and sign your app.
