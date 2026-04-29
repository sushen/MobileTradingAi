package com.shaplachottor.lab.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shaplachottor.lab.databinding.ActivityRegisterBinding

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
