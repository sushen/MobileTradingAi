package com.shaplachottor.lab.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Runtime Verification & Immediate Flush
        Log.d("FB_SDK_CHECK", "Initialized: ${FacebookSdk.isInitialized()}")
        val logger = AppEventsLogger.newLogger(this)
        
        // Log guaranteed test event
        val params = Bundle()
        params.putString("source", "SplashActivity")
        logger.logEvent("TEST_EVENT_NOW", params)
        
        // CRITICAL: Force flush to bypass buffering for real-time testing
        AppEventsLogger.onContextStop()
        Log.d("FB_SDK_CHECK", "Event TEST_EVENT_NOW logged and onContextStop() called for flush.")

        // No layout for splash, just a theme-based splash screen or a simple one if needed
        // For now, simple delay and route
        lifecycleScope.launch {
            delay(2000)
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}
