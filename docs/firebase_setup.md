# Firebase Setup Guide

## 1) Create the Firebase project
1. Create a Firebase project in the Firebase Console.
2. Add an Android app with package name `com.shaplachottor.app`.
3. Download `google-services.json` and place it at `app/google-services.json`.

## 2) Enable authentication
- Enable the Google provider.
- Add the SHA-1 and SHA-256 fingerprints for your debug and release keystores.
- Copy the Google Web client ID into `app/src/main/res/values/strings.xml` as `default_web_client_id`.

## 3) Enable Firestore
- Create Firestore in production mode.
- Deploy `firestore.rules`.
- Sign in once with a valid Google account to let the app create the default user profile and seed the canonical 6-phase catalog when the `phases` collection is empty.

## 4) Collections used by the app
- `users`
- `phases`
- `bookings`

Booking requests now store `phoneNumber`, `whatsappNumber`, `createdAt`, `expiresAt`, and a `status` of `pending`, `approved`, or `expired`.
Users do not get phase access until an admin approves the request.

## 5) Booking notification backend

If you want the master admin to receive an email every time a new booking request is submitted, deploy the Cloud Function in `functions/`.

Setup steps:
- install backend dependencies with `npm --prefix functions install`
- create `functions/.env` from `functions/.env.example`
- set the SMTP password with `firebase functions:secrets:set SMTP_PASSWORD`
- deploy with `firebase deploy --only functions`

The default notification target is `sushen.biswas.aga@gmail.com`.

## 6) Default user state
New users are created with:
- `progress = 0`
- `phaseProgress = {}`
- `unlockedPhases = []`
- `completedPhases = []`
- `unlockedFeatures = { tradingBot: false, investment: false, affiliate: false }`

## 7) Feature gating
- `>= 30%` overall progress unlocks Bot Setup.
- `>= 60%` overall progress unlocks Investment Features.
- `100%` overall progress unlocks the Affiliate System.

Overall progress is calculated across all 6 phases, so unfinished phases count as `0`.
