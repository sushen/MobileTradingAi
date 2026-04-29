You are an expert Android engineer and QA auditor.

Your task is to analyze the entire Android project and verify whether the Meta (Facebook) SDK is correctly implemented, configured, and ready for production.

Then generate a complete report and save it as `context.md`.

---

# 🎯 Objective

DO NOT assume anything. Verify everything from code, manifest, and runtime readiness.

---

# ✅ Step 1: Dependency Check

Scan `app/build.gradle` and confirm:

* `com.facebook.android:facebook-android-sdk` exists

Mark:

* ✅ Present
* ❌ Missing

---

# ✅ Step 2: App ID Configuration

Check:

### `res/values/strings.xml`

* `facebook_app_id` exists

### `AndroidManifest.xml`

* `com.facebook.sdk.ApplicationId` meta-data exists

---

# ✅ Step 3: Manifest Configuration

Verify:

* INTERNET permission exists
* `AutoInitEnabled` present
* `AutoLogAppEventsEnabled` present
* `AdvertiserIDCollectionEnabled` present

---

# ✅ Step 4: Package & Class Validation

Extract and confirm:

* Application ID (package name)
* Launcher Activity (MAIN + LAUNCHER)

---

# ✅ Step 5: SDK Initialization

Check if SDK is initialized via:

* Auto init (manifest), OR
* Manual initialization in Application class

---

# ✅ Step 6: Event Logging

Check if any event logging exists:

Examples:

* `AppEventsLogger`
* `logEvent(...)`

If not present:

* Mark as ❌ Missing

---

# ✅ Step 7: Debug Capability

Check if debugging is enabled:

* `FacebookSdk.setIsDebugEnabled(true)`
* LoggingBehavior.APP_EVENTS

---

# ✅ Step 8: SHA Key Awareness (IMPORTANT)

Check if project includes any reference or documentation for:

* SHA-1
* Key Hash

If not:

* Mark as ⚠️ Missing (required for production tracking)

---

# ✅ Step 9: Installation Source Awareness

Add note:

* Android Studio install → NOT counted
* Play Store install → REQUIRED for real tracking

---

# 📄 Step 10: Generate context.md

Create a structured file:

```md
# Meta SDK Integration Audit

## 1. Dependency
- Facebook SDK: ✅ / ❌

## 2. App ID Setup
- App ID in strings.xml: ✅ / ❌
- Manifest meta-data: ✅ / ❌

## 3. Manifest Config
- INTERNET Permission: ✅ / ❌
- Auto Init: ✅ / ❌
- Auto Log Events: ✅ / ❌
- Advertiser ID Collection: ✅ / ❌

## 4. App Identity
- Package Name: <value>
- Launcher Activity: <value>

## 5. SDK Initialization
- Method: Auto / Manual
- Status: ✅ / ❌

## 6. Event Logging
- Implemented: ✅ / ❌
- Example: <event name or location>

## 7. Debug Setup
- Debug Enabled: ✅ / ❌

## 8. SHA / Key Hash
- Configured: ❌ / ⚠️ / ✅
- Note: Required for production tracking

## 9. Installation Source
- Play Store Required: ✅

## 10. Final Verdict
- ❌ Not Ready
- ⚠️ Partially Ready
- ✅ Production Ready

## 11. Missing / Issues
- List all problems clearly

## 12. Next Actions
- Actionable checklist to fix issues
```

---

# ⚠️ Rules

* Do NOT assume success
* Be strict and honest
* If something is missing → mark clearly
* Output must be clean, readable, and professional

---

# 🎯 Expected Output

* Full audit completed
* `context.md` created
* Clear verdict + next steps
