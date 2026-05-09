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

        // 1. Initialize Firebase as early as possible
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("FIREBASE_ERROR", "Initialization failed", e)
        }

        // 2. Initialize AppGraph
        try {
            com.shaplachottor.lab.data.AppGraph.init(this)
        } catch (e: Exception) {
            Log.e("APP_GRAPH_ERROR", "Initialization failed", e)
        }

        // 3. Defer Meta SDK initialization to prevent blocking the main thread
        Thread {
            try {
                FacebookSdk.fullyInitialize()
                FacebookSdk.setAutoLogAppEventsEnabled(true)
                FacebookSdk.setAdvertiserIDCollectionEnabled(true)
                if (BuildConfig.DEBUG) {
                    FacebookSdk.setIsDebugEnabled(true)
                    FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
                    printKeyHash()
                }
            } catch (e: Exception) {
                Log.e("FB_SDK_ERROR", "Deferred initialization failed", e)
            }
        }.start()
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
