package com.shaplachottor.lab.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shaplachottor.lab.R
import com.shaplachottor.lab.adapters.PhaseAdapter
import com.shaplachottor.lab.databinding.DialogBookingRequestBinding
import com.shaplachottor.lab.databinding.FragmentMyLearningBinding
import com.shaplachottor.lab.models.BookingRequestOutcome
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.PhaseProgressionSnapshot
import com.shaplachottor.lab.models.PhaseProgressionState
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.viewmodels.MyLearningViewModel
import kotlinx.coroutines.launch

class MyLearningFragment : Fragment() {
    private var _binding: FragmentMyLearningBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyLearningViewModel by viewModels()
    private val phaseRepository = PhaseRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyLearningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        viewModel.loadUserData()
    }

    private fun setupObservers() {
        viewModel.learningJourneyProgress.observe(viewLifecycleOwner) { learningJourneyProgress ->
            val overallPercent = learningJourneyProgress?.overallLearningProgress?.percent ?: 0
            binding.tvTotalProgress.text = "${overallPercent}%"
            binding.totalProgressIndicator.setProgress(overallPercent, true)

            val currentPhaseProgress = learningJourneyProgress?.currentPhaseProgress
            if (currentPhaseProgress != null) {
                val phase = currentPhaseProgress.phase
                val progress = currentPhaseProgress.progress
                binding.cardCurrentPhase.visibility = View.VISIBLE
                binding.tvNoActivePhase.visibility = View.GONE
                binding.tvCurrentPhaseTitle.text = "Phase ${phase.order}: ${phase.title}"
                binding.tvCurrentPhaseDesc.text = phase.description
                binding.tvCurrentPhaseProgressPercent.text = "${progress.percent}%"
                binding.currentPhaseProgressIndicator.setProgress(progress.percent, true)
                binding.tvCurrentPhaseLessonSummary.text = getString(
                    R.string.lesson_progress_summary,
                    progress.completedLessons,
                    progress.totalLessons
                )

                binding.btnContinue.setOnClickListener {
                    val navController = findNavController()
                    if (navController.currentDestination?.id == R.id.myLearningFragment) {
                        val bundle = Bundle().apply { putString("phaseId", phase.phaseId) }
                        navController.navigate(R.id.action_myLearningFragment_to_classroomFragment, bundle)
                    }
                }
            } else {
                binding.cardCurrentPhase.visibility = View.GONE
                binding.tvNoActivePhase.visibility = View.VISIBLE
            }
        }

        viewModel.completedPhasesList.observe(viewLifecycleOwner) { completedPhases ->
            if (completedPhases.isNotEmpty()) {
                binding.tvCompletedLabel.visibility = View.VISIBLE
                binding.rvCompletedPhases.visibility = View.VISIBLE

                val completedSnapshots = completedPhases.map { phase ->
                    PhaseProgressionSnapshot(
                        phase = phase,
                        state = PhaseProgressionState.COMPLETED,
                        badgeLabel = "COMPLETED",
                        statusMessage = "Classroom progress reached 100%. Review this class anytime.",
                        actionLabel = "Review Classroom",
                        isActionEnabled = true,
                        canEnterClassroom = true
                    )
                }

                val adapter = PhaseAdapter(
                    phaseSnapshots = completedSnapshots,
                    onPhaseClick = { snapshot ->
                        val navController = findNavController()
                        if (navController.currentDestination?.id == R.id.myLearningFragment) {
                            val bundle = Bundle().apply { putString("phaseId", snapshot.phase.phaseId) }
                            navController.navigate(R.id.action_myLearningFragment_to_classroomFragment, bundle)
                        }
                    },
                    onRequestSeat = { phase ->
                        showBookingRequestDialog(phase)
                    }
                )
                binding.rvCompletedPhases.layoutManager = LinearLayoutManager(requireContext())
                binding.rvCompletedPhases.adapter = adapter
            } else {
                binding.tvCompletedLabel.visibility = View.GONE
                binding.rvCompletedPhases.visibility = View.GONE
            }
        }
    }

    private fun showBookingRequestDialog(phase: Phase) {
        val dialogBinding = DialogBookingRequestBinding.inflate(layoutInflater)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Request Seat")
            .setMessage("Please provide your WhatsApp number for ${phase.title}")
            .setView(dialogBinding.root)
            .setPositiveButton("Submit") { _, _ ->
                val whatsapp = dialogBinding.etWhatsappNumber.text.toString().trim()
                if (whatsapp.isNotEmpty()) {
                    performRequestSeat(phase, whatsapp)
                } else {
                    Toast.makeText(requireContext(), "WhatsApp number is required.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performRequestSeat(phase: Phase, whatsapp: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = phaseRepository.requestSeat(phase, whatsapp)
            if (result.outcome == BookingRequestOutcome.REQUEST_CREATED) {
                Toast.makeText(requireContext(), "Request submitted successfully", Toast.LENGTH_SHORT).show()
                viewModel.loadUserData()
            } else {
                Toast.makeText(requireContext(), "Request failed: ${result.outcome}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
