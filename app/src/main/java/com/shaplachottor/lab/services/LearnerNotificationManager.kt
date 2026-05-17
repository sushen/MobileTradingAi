package com.shaplachottor.lab.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shaplachottor.lab.R
import com.shaplachottor.lab.activities.MainActivity
import com.shaplachottor.lab.data.PhaseCatalog
import com.shaplachottor.lab.models.Booking

class LearnerNotificationManager(
    private val context: Context,
    private val userId: String
) {
    private val db = FirebaseFirestore.getInstance()
    private val channelId = "learner_progress_updates"
    private var listenerRegistration: ListenerRegistration? = null

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Learning Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Teacher review and classroom unlock updates"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startListening() {
        if (listenerRegistration != null) return

        listenerRegistration = db.collection("bookings")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    android.util.Log.e("LearnerNotificationMgr", "Error listening for learner updates: ${e.message}", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    if (dc.type != DocumentChange.Type.MODIFIED || dc.document.metadata.hasPendingWrites()) {
                        continue
                    }

                    val booking = dc.document.toObject(Booking::class.java)
                    if (booking.status == Booking.STATUS_REVIEWING ||
                        booking.status == Booking.STATUS_APPROVED ||
                        booking.status == Booking.STATUS_REJECTED
                    ) {
                        showNotification(booking)
                    }
                }
            }
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    private fun showNotification(booking: Booking) {
        val phaseTitle = PhaseCatalog.findById(booking.phaseId)?.title ?: booking.phaseId
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            booking.bookingId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = when (booking.status) {
            Booking.STATUS_REVIEWING -> "Teacher will WhatsApp"
            Booking.STATUS_APPROVED -> "$phaseTitle is approved. You can enter the classroom now."
            Booking.STATUS_REJECTED -> "Your request for $phaseTitle was rejected. Continue practicing and try again."
            else -> return
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.lotus_logo)
            .setContentTitle("Classroom Update")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(booking.bookingId.hashCode(), builder.build())
            }
        }
    }
}
