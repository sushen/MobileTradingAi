package com.shaplachottor.lab

import android.app.Application
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.FirebaseApp

class TradingAIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Meta SDK Debugging
        FacebookSdk.setIsDebugEnabled(true)
        FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS)
        
        Log.d("FB_EVENTS_CHECK", "SDK Initialized: ${FacebookSdk.isInitialized()}")

        // Initialize Firebase if the google-services.json was provided and plugin applied
        // Even if not, FirebaseApp.initializeApp(this) is usually safe to call if dependencies are there
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Handle cases where Firebase cannot initialize
        }
    }
}
