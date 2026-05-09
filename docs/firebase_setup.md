# Firebase Setup Guide

## 1) Create the Firebase project
1. Create a Firebase project in the Firebase Console.
2. Add an Android app with package name `com.shaplachottor.lab`.
3. Download `google-services.json` and place it at `app/google-services.json`.

## 2) Enable authentication
- Enable **Email/Password** provider.
- Enable **Google** provider.
- Add SHA-1 and SHA-256 fingerprints.
- Update `default_web_client_id` in `strings.xml`.

## 3) Enable Firestore
- Create Firestore in production mode.
- Use the following collections: `users`, `phases`, `bookings`, `referralEvents`, `affiliateStats`.
- Admin (`sushen.biswas.aga@gmail.com`) must log in to bootstrap the `phases` catalog.

## 4) Rules
Ensure Firestore rules allow:
- Users to read/write their own `users/{userId}` document.
- Users to read all `phases`.
- Users to create/read their own `bookings`.
- Admins (identified by email) to read/write all collections.

## 5) Admin Notifications
The app includes an `AdminNotificationManager` that listens for `pending` bookings in real-time when the admin is logged in. Ensure the admin user has the correct email hardcoded in `MainActivity.kt` and `AdminPanelFragment.kt`.

## 6) Monetization Configuration
Premium Cohorts (Phases 2-6) require payment verification.
The admin uses the "Confirm Payment" dialog in the Admin Panel to verify manual payments (e.g., via bank transfer or mobile money) before granting access.
