# Trading AI (Android App)

Android gateway application for the Trading AI Windows bot ecosystem.

## Important scope
- The Android app **does not execute trades**.
- The app provides account access, education, Web3 investment entry, and affiliate tooling.
- All wallet signing happens in external wallets (MetaMask/WalletConnect), never in-app private key storage.

## Tech stack
- Java + Android SDK
- Firebase Authentication + Firestore
- Material Design 3
- Web3 deep-link / WalletConnect-ready template
- MVVM architecture

## Feature map
1. **Authentication**: Email/Password + Google Sign-In, Firestore user profile.
2. **Home Dashboard**: 4 actions: Windows version, Invest via Web3, Education, Affiliate.
3. **Windows Version**: app overview, download link, setup guide, FAQ.
4. **Web3 Investment**: wallet connect, tx submit in wallet, tx hash/history/status.
5. **Education**: courses, paid unlock flow, enrollment and progress tracking.
6. **Affiliate**: referral code, referred users, commission metrics, withdrawal request.
7. **Admin**: user/investment totals, course + affiliate settings, investment on/off.

## Project structure
- `app/src/main/java/com/tradingai/mobile/data` : models + repository stubs.
- `app/src/main/java/com/tradingai/mobile/viewmodel` : MVVM state holders.
- `app/src/main/java/com/tradingai/mobile/ui` : entry UI modules.
- `docs/firebase_setup.md` : Firebase onboarding steps.
- `docs/web3_integration_template.md` : Web3 integration flow template.
- `docs/firestore_database_structure.md` : Firestore schema reference.
- `firestore.rules` : sample secure Firestore rules.

## Security and compliance notes
- Input validation should run both client-side and in backend/rules.
- Prevent duplicate investment submissions with idempotency keys.
- Display transparent risk disclaimers in app UI and docs.
- Do not claim guaranteed profits.

## Gradle dependencies (high level)
Included in `app/build.gradle`:
- Firebase BoM + Auth + Firestore
- Google Sign-In
- Material3
- AndroidX Lifecycle + Navigation
- Web3J Android core

## Next implementation steps
1. Add full screen flow (auth + bottom navigation).
2. Implement Firestore transactions for investment idempotency.
3. Add WalletConnect SDK and callback handling.
4. Add admin-restricted screens and rule tests.
5. Add instrumentation tests for key flows.
