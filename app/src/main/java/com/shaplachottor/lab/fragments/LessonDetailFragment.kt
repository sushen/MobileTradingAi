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
import com.shaplachottor.lab.adapters.ContentBlockAdapter
import com.shaplachottor.lab.databinding.FragmentLessonDetailBinding
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.viewmodels.ClassroomViewModel
import com.shaplachottor.lab.viewmodels.ClassroomViewModelFactory

class LessonDetailFragment : Fragment() {
    companion object {
        private const val TAG = "LessonDetailFragment"
    }

    private var _binding: FragmentLessonDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ClassroomViewModel
    private val args: LessonDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLessonDetailBinding.inflate(inflater, container, false)
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

        android.util.Log.d(TAG, "Opening lesson detail: phaseId=${args.phaseId}, lessonId=${args.lessonId}")
        viewModel.selectLesson(args.phaseId, args.lessonId)
    }

    private fun setupToolbar() {
        binding.toolbarLessonDetail.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupObservers() {
        viewModel.selectedLesson.observe(viewLifecycleOwner) { lesson ->
            lesson?.let { selectedLesson ->
                if (selectedLesson.id != args.lessonId) {
                    android.util.Log.w(
                        TAG,
                        "Selected lesson mismatch: argsLessonId=${args.lessonId}, renderedLessonId=${selectedLesson.id}, phaseId=${args.phaseId}"
                    )
                }

                android.util.Log.d(
                    TAG,
                    "Rendering lesson detail: phaseId=${args.phaseId}, lessonId=${selectedLesson.id}, lessonOrder=${selectedLesson.order}, isCompleted=${selectedLesson.isCompleted}"
                )
                binding.tvLessonDetailTitle.text = selectedLesson.title
                binding.rvContentBlocks.layoutManager = LinearLayoutManager(requireContext())
                binding.rvContentBlocks.adapter = ContentBlockAdapter(selectedLesson.contentBlocks)
                
                binding.btnCompleteLesson.isEnabled = !selectedLesson.isCompleted
                binding.btnCompleteLesson.text = if (selectedLesson.isCompleted) "Completed" else "Mark as Complete"
                
                binding.btnCompleteLesson.setOnClickListener {
                    binding.btnCompleteLesson.isEnabled = false
                    android.util.Log.d(
                        TAG,
                        "Mark as complete pressed: phaseId=${args.phaseId}, requestedLessonId=${args.lessonId}, renderedLessonId=${selectedLesson.id}, lessonOrder=${selectedLesson.order}"
                    )
                    viewModel.completeLesson(args.phaseId, args.lessonId)
                }
            }
        }

        viewModel.operationStatus.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    is ClassroomViewModel.OperationResult.Success -> {
                        android.util.Log.d(TAG, "Lesson completion succeeded: phaseId=${args.phaseId}, lessonId=${args.lessonId}")
                        com.google.android.material.snackbar.Snackbar.make(
                            binding.root, 
                            getString(com.shaplachottor.lab.R.string.lesson_completed_feedback),
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                        ).show()
                        // Auto-back to classroom to see progress
                        view?.postDelayed({ 
                            if (isAdded) {
                                findNavController().navigateUp()
                            }
                        }, 800)
                    }
                    is ClassroomViewModel.OperationResult.Error -> {
                        android.util.Log.e(TAG, "Lesson completion failed: phaseId=${args.phaseId}, lessonId=${args.lessonId}, message=${it.message}")
                        Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                        binding.btnCompleteLesson.isEnabled = true
                    }
                }
                viewModel.clearOperationStatus()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
