package com.shaplachottor.lab.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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

        if (authSessionProvider.currentUser()?.email == "sushen.biswas.aga@gmail.com") {
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
    }

    private fun loadUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = userRepository.getCurrentUserOrNull()
            user?.let {
                binding.tvProfileName.text = it.name
                binding.tvProfileEmail.text = it.email
                if (!it.photoUrl.isNullOrEmpty()) {
                    com.bumptech.glide.Glide.with(this@ProfileFragment)
                        .load(it.photoUrl)
                        .into(binding.ivProfilePicture)
                }
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
