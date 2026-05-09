package com.shaplachottor.lab.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.shaplachottor.lab.databinding.ActivityRegisterBinding
import com.shaplachottor.lab.models.User
import com.shaplachottor.lab.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            performRegistration()
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun performRegistration() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val referralCode = binding.etReferralCode.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                
                if (firebaseUser != null) {
                    val user = User(
                        id = firebaseUser.uid,
                        email = email,
                        name = name
                    )

                    if (referralCode.isNotEmpty()) {
                        val referrer = userRepository.findUserByReferralCode(referralCode)
                        if (referrer != null && referrer.id != user.id) {
                            userRepository.saveUser(user.copy(referredBy = referrer.id))
                        } else {
                            userRepository.saveUser(user)
                            Toast.makeText(this@RegisterActivity, "Invalid referral code", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        userRepository.saveUser(user)
                    }

                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finishAffinity()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
    }
}
