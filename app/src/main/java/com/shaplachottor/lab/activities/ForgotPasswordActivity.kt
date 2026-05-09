package com.shaplachottor.lab.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.shaplachottor.lab.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnResetPassword.setOnClickListener {
            performPasswordReset()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun performPasswordReset() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnResetPassword.isEnabled = false
        lifecycleScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                Toast.makeText(this@ForgotPasswordActivity, "Reset link sent to your email", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@ForgotPasswordActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnResetPassword.isEnabled = true
            }
        }
    }
}
