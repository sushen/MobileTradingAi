package com.shaplachottor.lab.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.shaplachottor.lab.R
import com.shaplachottor.lab.activities.LoginActivity
import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.databinding.FragmentProfileBinding
import com.shaplachottor.lab.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val userRepository = UserRepository()
    private val authSessionProvider = AppGraph.authSessionProvider()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadUserProfile()
        
        binding.btnLogout.setOnClickListener {
            handleLogout()
        }

        val email = authSessionProvider.currentUser()?.email
        if (email == "sushen.biswas.aga@gmail.com" || email == "sushen.biswas.aga@googlemail.com") {
            binding.chipRole.text = "Admin"
            binding.chipRole.setOnClickListener {
                androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(
                    R.id.action_profileFragment_to_adminPanelFragment
                )
            }
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            val intent = Intent(requireContext(), com.shaplachottor.lab.activities.PrivacyPolicyActivity::class.java)
            startActivity(intent)
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showDeleteAccountDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Account?")
            .setMessage("This will permanently delete your account, learning progress, bookings, and affiliate data. This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val hostActivity = activity ?: return
        
        // Disable buttons and show loading state if possible
        binding.btnDeleteAccount.isEnabled = false
        binding.btnLogout.isEnabled = false
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                userRepository.deleteAccount()
                
                // On Success
                Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_LONG).show()
                
                // Sign out completely and navigate to login
                completeSignOutAndNavigateToLogin(hostActivity)
                
            } catch (e: Exception) {
                binding.btnDeleteAccount.isEnabled = true
                binding.btnLogout.isEnabled = true

                if (e is FirebaseAuthRecentLoginRequiredException) {
                    showRecentLoginRequiredDialog(hostActivity)
                } else {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showRecentLoginRequiredDialog(hostActivity: FragmentActivity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Security Re-authentication Required")
            .setMessage("For security reasons, please sign in again before deleting your account.")
            .setPositiveButton("Sign Out & Re-login") { _, _ ->
                completeSignOutAndNavigateToLogin(hostActivity)
            }
            .setCancelable(false)
            .show()
    }

    private fun completeSignOutAndNavigateToLogin(hostActivity: FragmentActivity) {
        authSessionProvider.signOut()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(hostActivity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(hostActivity, gso)

        googleSignInClient.signOut()
            .addOnCompleteListener(hostActivity) {
                navigateToLogin(hostActivity)
            }
    }

    private fun loadUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = userRepository.getCurrentUserOrNull()
            user?.let {
                binding.tvProfileName.text = it.name
                binding.tvProfileEmail.text = it.email
                binding.tvReferralCode.text = it.referralCode
                
                if (!it.photoUrl.isNullOrEmpty()) {
                    com.bumptech.glide.Glide.with(this@ProfileFragment)
                        .load(it.photoUrl)
                        .into(binding.ivProfilePicture)
                }

                binding.btnShareReferral.setOnClickListener { _ ->
                    shareReferralCode(it.referralCode)
                }

                loadAffiliateStats(it.id)
            }
        }
    }

    private fun shareReferralCode(code: String) {
        val shareText = "Join me in the Trading AI Lab! Use my referral code $code to start your research journey: https://shaplachottor.lab/join"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Share Referral Code"))
    }

    private fun loadAffiliateStats(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val stats = AppGraph.appStore().getAffiliateStats(userId)
            stats?.let {
                binding.tvTotalInvites.text = (it["totalInvites"] ?: 0).toString()
                binding.tvConversions.text = (it["conversions"] ?: 0).toString()
            }
        }
    }

    private fun handleLogout() {
        val hostActivity = activity ?: return
        binding.btnLogout.isEnabled = false

        authSessionProvider.signOut()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(hostActivity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(hostActivity, gso)

        googleSignInClient.signOut()
            .addOnCompleteListener(hostActivity) {
                navigateToLogin(hostActivity)
            }
    }

    private fun navigateToLogin(hostActivity: FragmentActivity) {
        val intent = Intent(hostActivity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        hostActivity.startActivity(intent)
        hostActivity.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
