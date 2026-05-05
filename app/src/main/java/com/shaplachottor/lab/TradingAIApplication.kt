package com.shaplachottor.lab

import android.app.Application
import android.os.Build
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

        // Initialize Meta SDK before any event logging.
        FacebookSdk.setApplicationId(getString(R.string.facebook_app_id))
        FacebookSdk.setClientToken(getString(R.string.facebook_client_token))
        FacebookSdk.fullyInitialize()

        // Keep collection behavior enabled for production installs.
        FacebookSdk.setAutoLogAppEventsEnabled(true)
        FacebookSdk.setAdvertiserIDCollectionEnabled(true)
        AppEventsLogger.activateApp(this)

        if (BuildConfig.DEBUG) {
            // Keep verbose Meta diagnostics removable from release builds.
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.DEVELOPER_ERRORS)
            printKeyHash()
            Log.d("FB_SDK_STATUS", "SDK Initialized: ${FacebookSdk.isInitialized()}")
            Log.d("FB_SDK_STATUS", "App ID: ${FacebookSdk.getApplicationId()}")
        }

        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("FIREBASE_ERROR", "Initialization failed", e)
        }
    }

    private fun printKeyHash() {
        try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                info.signingInfo?.apkContentsSigners?.toList().orEmpty()
            } else {
                @Suppress("DEPRECATION")
                val info = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                info.signatures?.toList().orEmpty()
            }

            signatures.forEach { signature ->
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
