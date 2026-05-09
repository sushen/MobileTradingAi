package com.shaplachottor.lab.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.shaplachottor.lab.R
import com.shaplachottor.lab.adapters.LessonAdapter
import com.shaplachottor.lab.databinding.FragmentClassroomBinding
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.viewmodels.ClassroomViewModel
import com.shaplachottor.lab.viewmodels.ClassroomViewModelFactory

class ClassroomFragment : Fragment() {
    companion object {
        private const val TAG = "ClassroomFragment"
    }

    private var _binding: FragmentClassroomBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ClassroomViewModel
    private val args: ClassroomFragmentArgs by navArgs()
    private var lastRenderedPhaseProgress: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClassroomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = PhaseRepository()
        val factory = ClassroomViewModelFactory(repository)
        val backStackEntry = findNavController().getBackStackEntry(R.id.classroomFragment)
        viewModel = ViewModelProvider(backStackEntry, factory)[ClassroomViewModel::class.java]

        setupToolbar()
        setupObservers()

        android.util.Log.d(TAG, "Opening classroom: phaseId=${args.phaseId}")
        viewModel.loadClassroom(args.phaseId)
    }

    private fun setupToolbar() {
        binding.toolbarClassroom.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupObservers() {
        viewModel.accessDenied.observe(viewLifecycleOwner) { denied ->
            if (denied == true) {
                Toast.makeText(
                    requireContext(),
                    "This classroom stays locked until your booking request is approved.",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigateUp()
            }
        }

        viewModel.phase.observe(viewLifecycleOwner) { phase ->
            phase?.let {
                binding.tvPhaseTitleClassroom.text = it.title
            }
        }

        viewModel.lessons.observe(viewLifecycleOwner) { lessons ->
            android.util.Log.d(
                TAG,
                "Rendering lessons: phaseId=${args.phaseId}, states=${lessons.mapIndexed { index, lesson -> "${lesson.id}@adapterPosition=$index/order=${lesson.order}/completed=${lesson.isCompleted}" }}"
            )
            binding.rvLessons.layoutManager = LinearLayoutManager(requireContext())
            binding.rvLessons.adapter = LessonAdapter(lessons,
                onLessonClick = { lesson ->
                    android.util.Log.d(
                        TAG,
                        "Opening lesson detail: phaseId=${args.phaseId}, lessonId=${lesson.id}, lessonOrder=${lesson.order}, lessonIndex=${lessons.indexOfFirst { it.id == lesson.id }}"
                    )
                    val action = ClassroomFragmentDirections.actionClassroomFragmentToLessonDetailFragment(
                        args.phaseId,
                        lesson.id
                    )
                    findNavController().navigate(action)
                },
                onCompleteToggle = { lesson, isChecked ->
                    android.util.Log.d(
                        TAG,
                        "Lesson toggle from classroom: phaseId=${args.phaseId}, lessonId=${lesson.id}, lessonOrder=${lesson.order}, requestedCompleted=$isChecked"
                    )
                    viewModel.toggleLessonComplete(args.phaseId, lesson.id, isChecked)
                }
            )
        }

        viewModel.learningJourneyProgress.observe(viewLifecycleOwner) { learningJourneyProgress ->
            val phaseProgress = learningJourneyProgress?.phaseProgressById?.get(args.phaseId) ?: return@observe
            val previousProgress = lastRenderedPhaseProgress

            android.util.Log.d(
                TAG,
                "UI progress update: phaseId=${args.phaseId}, activePhase=${learningJourneyProgress.activePhaseId}, completedLessons=${phaseProgress.completedLessons}, totalLessons=${phaseProgress.totalLessons}, progress=${phaseProgress.percent}, previousProgress=$previousProgress"
            )

            binding.tvClassroomProgressLabel.text = if (learningJourneyProgress.activePhaseId == args.phaseId) {
                getString(R.string.current_phase_progress)
            } else {
                getString(R.string.phase_progress)
            }
            binding.classroomProgressIndicator.setProgress(phaseProgress.percent, true)
            binding.tvClassroomProgressPercent.text = "${phaseProgress.percent}%"
            binding.tvClassroomProgressSummary.text = getString(
                R.string.lesson_progress_summary,
                phaseProgress.completedLessons,
                phaseProgress.totalLessons
            )

            if (previousProgress != null && phaseProgress.percent == 100 && previousProgress < 100) {
                showCompletionDialog()
            }

            lastRenderedPhaseProgress = phaseProgress.percent
        }
    }

    private fun showCompletionDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.phase_completed_title)
            .setMessage(R.string.phase_completed_message)
            .setCancelable(false)
            .setPositiveButton(R.string.return_to_phases) { dialog, _ ->
                dialog.dismiss()
                findNavController().navigateUp()
            }
            .setNegativeButton(R.string.stay_here, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
