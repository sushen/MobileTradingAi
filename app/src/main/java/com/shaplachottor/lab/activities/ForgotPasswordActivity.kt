package com.shaplachottor.lab.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shaplachottor.lab.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnResetPassword.setOnClickListener {
            // Placeholder reset logic
            finish()
        }
    }
}
