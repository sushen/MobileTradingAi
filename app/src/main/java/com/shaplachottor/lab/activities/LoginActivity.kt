package com.shaplachottor.lab.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shaplachottor.lab.BuildConfig
import com.shaplachottor.lab.R
import com.shaplachottor.lab.data.PhaseCatalog
import com.shaplachottor.lab.databinding.ActivityLoginBinding
import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.BookingRequestOutcome
import com.shaplachottor.lab.models.User
import com.shaplachottor.lab.repository.UserRepository
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.util.BookingRequestDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val userRepository = UserRepository()
    private val phaseRepository = PhaseRepository()

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showLoading(false)
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            showLoading(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        displayVersionInfo()

        binding.btnGoogleLogin.setOnClickListener {
            showLoading(true)
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        lifecycleScope.launch {
            try {
                val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    val referralCodeInput = binding.etReferralCode.text.toString().trim()
                    
                    val user = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString() ?: ""
                    )
                    
                    // Validate referral code if provided
                    if (referralCodeInput.isNotEmpty()) {
                        val referrer = userRepository.findUserByReferralCode(referralCodeInput)
                        if (referrer != null && referrer.id != user.id) {
                            userRepository.saveUser(user.copy(referredBy = referrer.id))
                        } else {
                            // Proceed without referral if invalid or self-referral
                            userRepository.saveUser(user)
                            if (referralCodeInput.isNotEmpty()) {
                                Toast.makeText(this@LoginActivity, "Invalid referral code", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        userRepository.saveUser(user)
                    }

                    val savedUser = userRepository.getCurrentUserOrNull() ?: user
                    routeAfterGoogleLogin(savedUser)
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Authentication Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun displayVersionInfo() {
        val versionName = BuildConfig.VERSION_NAME
        val buildType = BuildConfig.BUILD_TYPE

        // Try to get branch from BuildConfig, then from generated resValue
        var branch = BuildConfig.CURRENT_GIT_BRANCH
        if (branch.isNullOrEmpty()) {
            val resId = resources.getIdentifier("git_branch_res", "string", packageName)
            if (resId != 0) {
                branch = getString(resId)
            }
        }

        binding.tvVersionInfo.text = getString(R.string.version_label, versionName)

        if (!branch.isNullOrEmpty()) {
            val branchInfo = if (buildType != "release") "$branch-$buildType" else branch
            binding.tvBranchInfo.text = getString(R.string.branch_label, branchInfo)
            binding.tvBranchInfo.visibility = View.VISIBLE
        } else {
            binding.tvBranchInfo.visibility = View.GONE
        }
    }

    private fun routeAfterGoogleLogin(user: User) {
        if (user.whatsappNumber.isNullOrBlank()) {
            showPhaseOneBookingPrompt(user)
        } else {
            continueToMain()
        }
    }

    private fun showPhaseOneBookingPrompt(user: User) {
        val phaseOne = PhaseCatalog.findById(PhaseCatalog.PHASE1)
        if (phaseOne == null) {
            continueToMain()
            return
        }

        BookingRequestDialog.show(
            context = this,
            layoutInflater = layoutInflater,
            phase = phaseOne,
            initialWhatsappNumber = user.whatsappNumber,
            cancelable = false,
            showCancelButton = false,
            onSubmit = { whatsappNumber, dialog ->
                submitPhaseOneBooking(user, phaseOne, whatsappNumber, dialog)
            }
        )
    }

    private fun submitPhaseOneBooking(
        user: User,
        phaseOne: com.shaplachottor.lab.models.Phase,
        whatsappNumber: String,
        dialog: AlertDialog
    ) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

        lifecycleScope.launch {
            val result = phaseRepository.requestSeat(phaseOne, whatsappNumber)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true

            when (result.outcome) {
                BookingRequestOutcome.REQUEST_CREATED -> {
                    dialog.dismiss()
                    showRequestSentDialog()
                }

                BookingRequestOutcome.ALREADY_PENDING -> {
                    syncWhatsappNumberIfMissing(user, result.booking?.whatsappNumber ?: whatsappNumber)
                    dialog.dismiss()
                    showPendingApprovalDialog(phaseOne.title, result.booking)
                }

                BookingRequestOutcome.ALREADY_APPROVED -> {
                    syncWhatsappNumberIfMissing(user, result.booking?.whatsappNumber ?: whatsappNumber)
                    dialog.dismiss()
                    continueToMain()
                }

                BookingRequestOutcome.NO_SEATS_AVAILABLE -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "No seats available for this classroom right now.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                BookingRequestOutcome.INVALID_CONTACT_INFO -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "WhatsApp number is required.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                BookingRequestOutcome.PREREQUISITE_NOT_MET -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Complete the current classroom to 100% before requesting the next phase.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                BookingRequestOutcome.FAILED -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Request failed. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun syncWhatsappNumberIfMissing(user: User, whatsappNumber: String) {
        if (user.whatsappNumber.isNullOrBlank() && whatsappNumber.isNotBlank()) {
            runCatching {
                userRepository.saveUser(user.copy(whatsappNumber = whatsappNumber))
            }.onFailure {
                android.util.Log.w("LoginActivity", "Failed to backfill whatsappNumber for ${user.id}", it)
            }
        }
    }

    private fun showRequestSentDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Request Sent")
            .setMessage("Teacher will WhatsApp")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                continueToMain()
            }
            .show()
    }

    private fun showPendingApprovalDialog(phaseTitle: String, booking: Booking?) {
        val message = when (booking?.status) {
            Booking.STATUS_REVIEWING ->
                "Teacher will WhatsApp"

            else -> {
                "Teacher will WhatsApp"
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Awaiting Teacher Review")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                continueToMain()
            }
            .show()
    }

    private fun continueToMain() {
        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnGoogleLogin.isEnabled = !isLoading
    }
}
