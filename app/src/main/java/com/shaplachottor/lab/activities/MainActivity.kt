package com.shaplachottor.lab.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.shaplachottor.lab.R
import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.databinding.ActivityMainBinding
import com.shaplachottor.lab.services.AdminNotificationManager
import com.shaplachottor.lab.services.LearnerNotificationManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authProvider = AppGraph.authSessionProvider()
    private var adminNotificationManager: AdminNotificationManager? = null
    private var learnerNotificationManager: LearnerNotificationManager? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            checkAndStartAdminNotifications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        binding.bottomNav.setupWithNavController(navController)

        askNotificationPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        adminNotificationManager?.stopListening()
        learnerNotificationManager?.stopListening()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                checkAndStartAdminNotifications()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            checkAndStartAdminNotifications()
        }
    }

    private fun checkAndStartAdminNotifications() {
        val user = authProvider.currentUser()
        val email = user?.email
        val userId = user?.uid

        if (userId != null) {
            // Update FCM Token for push notifications
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    FirebaseFirestore.getInstance()
                        .collection("users").document(userId)
                        .update("fcmToken", token)
                }
            }

            if (learnerNotificationManager == null) {
                learnerNotificationManager = LearnerNotificationManager(this, userId)
            }
            learnerNotificationManager?.startListening()
        }

        if (email == "sushen.biswas.aga@gmail.com" || email == "sushen.biswas.aga@googlemail.com") {
            if (adminNotificationManager == null) {
                adminNotificationManager = AdminNotificationManager(this)
            }
            adminNotificationManager?.startListeningForRequests()
        }
    }
}
