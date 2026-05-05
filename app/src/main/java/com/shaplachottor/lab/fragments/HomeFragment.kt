package com.shaplachottor.lab.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.shaplachottor.lab.R
import com.shaplachottor.lab.databinding.FragmentHomeBinding

import androidx.fragment.app.viewModels
import com.shaplachottor.lab.viewmodels.HomeViewModel

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        viewModel.loadUserData()

        binding.btnExploreCourses.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_phasesFragment)
        }
        binding.btnResume.setOnClickListener {
            viewModel.currentPhase.value?.let { phase ->
                val bundle = Bundle().apply { putString("phaseId", phase.phaseId) }
                findNavController().navigate(R.id.action_homeFragment_to_classroomFragment, bundle)
            } ?: run {
                findNavController().navigate(R.id.action_homeFragment_to_phasesFragment)
            }
        }
    }

    private fun setupObservers() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvWelcome.text = "Welcome back, ${user.name.split(" ").get(0)}!"
                binding.tvProgressPercent.text = "${user.progress}%"
                binding.progressIndicator.setProgress(user.progress, true)
                
                user.photoUrl?.let { url ->
                    if (url.isNotEmpty()) {
                        Glide.with(this)
                            .load(url)
                            .circleCrop()
                            .into(binding.ivProfilePic)
                    }
                }
            }
        }

        viewModel.currentPhase.observe(viewLifecycleOwner) { phase ->
            if (phase != null) {
                binding.tvCurrentPhaseName.text = "Phase ${phase.order}: ${phase.title}"
                binding.tvCurrentCourse.text = phase.focus
                binding.cardContinueLearning.visibility = View.VISIBLE
            } else {
                binding.cardContinueLearning.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
