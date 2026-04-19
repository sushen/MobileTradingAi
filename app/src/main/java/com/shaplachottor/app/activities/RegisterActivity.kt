package com.shaplachottor.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shaplachottor.app.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            // Placeholder register logic
            finish()
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
}
