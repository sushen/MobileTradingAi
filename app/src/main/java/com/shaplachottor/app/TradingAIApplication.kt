package com.shaplachottor.app

import android.app.Application
import com.google.firebase.FirebaseApp

class TradingAIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase if the google-services.json was provided and plugin applied
        // Even if not, FirebaseApp.initializeApp(this) is usually safe to call if dependencies are there
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Handle cases where Firebase cannot initialize
        }
    }
}
