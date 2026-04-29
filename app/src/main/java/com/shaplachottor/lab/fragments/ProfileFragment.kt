package com.shaplachottor.lab.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
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
            // 1. Sign out from Firebase
            authSessionProvider.signOut()
            
            // 2. Sign out from Google to prevent auto-login
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
            googleSignInClient.signOut().addOnCompleteListener {
                // 3. Navigate back to LoginActivity
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
