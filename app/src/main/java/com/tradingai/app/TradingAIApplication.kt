package com.tradingai.app

import android.app.Application
import com.google.firebase.FirebaseApp

class TradingAIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(this)
    }

    companion object {
        lateinit var instance: TradingAIApplication
            private set
    }
}
