package com.shaplachottor.lab.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shaplachottor.lab.R

class AdminForegroundService : Service() {
    private var adminNotificationManager: AdminNotificationManager? = null
    private val channelId = "admin_foreground_service"
    private val notificationId = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        adminNotificationManager = AdminNotificationManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, notification)
        }
        
        adminNotificationManager?.startListeningForRequests()
        
        return START_STICKY
    }

    override fun onDestroy() {
        adminNotificationManager?.stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Admin Monitor Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the app active to receive booking requests"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Admin Monitor Active")
            .setContentText("Listening for new booking requests...")
            .setSmallIcon(R.drawable.lotus_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
