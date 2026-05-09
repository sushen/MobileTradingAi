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
import com.shaplachottor.lab.BuildConfig
import com.shaplachottor.lab.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            try {
                if (BuildConfig.DEBUG) {
                    Log.d("FB_SDK_CHECK", "Initialized: ${FacebookSdk.isInitialized()}")
                    val logger = AppEventsLogger.newLogger(this@SplashActivity)
                    val params = Bundle().apply {
                        putString("source", "SplashActivity")
                    }
                    logger.logEvent("fb_mobile_test_event", params)
                }
            } catch (e: Exception) {
                Log.e("SPLASH_ERROR", "SDK logging failed", e)
            }

            delay(2000)
            
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                } else {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                }
            } catch (e: Exception) {
                Log.e("SPLASH_ERROR", "Navigation failed", e)
                // Fallback to Login in case of Auth errors
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}
