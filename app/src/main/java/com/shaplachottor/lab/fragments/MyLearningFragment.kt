package com.shaplachottor.lab.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.shaplachottor.lab.databinding.FragmentMyLearningBinding

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.shaplachottor.lab.R
import com.shaplachottor.lab.adapters.PhaseAdapter
import com.shaplachottor.lab.models.PhaseProgressionSnapshot
import com.shaplachottor.lab.models.PhaseProgressionState
import com.shaplachottor.lab.viewmodels.MyLearningViewModel

class MyLearningFragment : Fragment() {
    private var _binding: FragmentMyLearningBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyLearningViewModel by viewModels()

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
                    phaseSnapshots = completedSnapshots
                ) { snapshot ->
                    val navController = findNavController()
                    if (navController.currentDestination?.id == R.id.myLearningFragment) {
                        val bundle = Bundle().apply { putString("phaseId", snapshot.phase.phaseId) }
                        navController.navigate(R.id.action_myLearningFragment_to_classroomFragment, bundle)
                    }
                }
                binding.rvCompletedPhases.layoutManager = LinearLayoutManager(requireContext())
                binding.rvCompletedPhases.adapter = adapter
            } else {
                binding.tvCompletedLabel.visibility = View.GONE
                binding.rvCompletedPhases.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
