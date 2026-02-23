# Firebase Setup Guide

## 1) Create project
1. Open Firebase Console and create a project named `Trading AI`.
2. Add Android app package: `com.tradingai.mobile`.
3. Download `google-services.json` and place it in `app/google-services.json`.

## 2) Enable Authentication
- Enable Email/Password provider.
- Enable Google provider and configure SHA-1/SHA-256 from your keystore.

## 3) Enable Firestore
- Create Firestore in production mode.
- Deploy the included `firestore.rules` then iterate safely.

## 4) Collections to provision
- `users`
- `investments`
- `courses`
- `enrollments`
- `affiliateStats`
- `settings`

## 5) Roles
Use Firebase custom claims for role handling:
- `role=user`
- `role=admin`

## 6) Security checklist
- Never store private keys.
- Validate write payloads client + rules.
- Keep API keys in remote config / secured env where possible.
