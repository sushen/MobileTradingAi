package com.shaplachottor.lab

import android.app.Application
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.FirebaseApp
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class TradingAIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Mandatory Meta SDK Initialization
        FacebookSdk.setApplicationId(getString(R.string.facebook_app_id))
        FacebookSdk.setClientToken(getString(R.string.facebook_client_token))
        FacebookSdk.sdkInitialize(this)
        FacebookSdk.fullyInitialize()

        // 2. Enable Verbose Debugging & Collection Flags
        FacebookSdk.setIsDebugEnabled(true)
        FacebookSdk.setAutoLogAppEventsEnabled(true)
        FacebookSdk.setAdvertiserIDCollectionEnabled(true)
        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        FacebookSdk.addLoggingBehavior(LoggingBehavior.DEVELOPER_ERRORS)
        
        // 3. Force Activate App (Essential for Install Tracking)
        AppEventsLogger.activateApp(this)
        
        // 4. Print Runtime Key Hash (Confirm this matches Meta Dashboard)
        printKeyHash()

        Log.d("FB_SDK_STATUS", "SDK Initialized: ${FacebookSdk.isInitialized()}")
        Log.d("FB_SDK_STATUS", "App ID: ${FacebookSdk.getApplicationId()}")

        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("FIREBASE_ERROR", "Initialization failed", e)
        }
    }

    private fun printKeyHash() {
        try {
            val info = packageManager.getPackageInfo(
                "com.shaplachottor.lab",
                PackageManager.GET_SIGNATURES
            )
            info.signatures?.forEach { signature ->
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hash = Base64.encodeToString(md.digest(), Base64.DEFAULT)
                Log.d("META_DEBUG_KEY_HASH", "KEY HASH: $hash")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("KEY_HASH_ERROR", "Package not found", e)
        } catch (e: NoSuchAlgorithmException) {
            Log.e("KEY_HASH_ERROR", "Algorithm error", e)
        }
    }
}
