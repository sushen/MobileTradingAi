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
import com.shaplachottor.lab.adapters.LessonAdapter
import com.shaplachottor.lab.databinding.FragmentClassroomBinding
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.viewmodels.ClassroomViewModel
import com.shaplachottor.lab.viewmodels.ClassroomViewModelFactory

class ClassroomFragment : Fragment() {

    private var _binding: FragmentClassroomBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ClassroomViewModel
    private val args: ClassroomFragmentArgs by navArgs()

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
        viewModel = ViewModelProvider(this, factory)[ClassroomViewModel::class.java]

        setupToolbar()
        setupObservers()
        
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
            binding.rvLessons.layoutManager = LinearLayoutManager(requireContext())
            binding.rvLessons.adapter = LessonAdapter(lessons, 
                onLessonClick = { lesson ->
                    Toast.makeText(requireContext(), "Opening ${lesson.title}", Toast.LENGTH_SHORT).show()
                },
                onCompleteToggle = { lesson, isChecked ->
                    viewModel.toggleLessonComplete(args.phaseId, lesson.id, isChecked)
                }
            )
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                val progress = it.phaseProgress[args.phaseId] ?: 0
                val previousProgress = binding.classroomProgressIndicator.progress
                
                binding.classroomProgressIndicator.setProgress(progress, true)
                binding.tvClassroomProgressPercent.text = "$progress%"

                if (progress == 100 && previousProgress < 100) {
                    showCompletionDialog()
                }
            }
        }
    }

    private fun showCompletionDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Phase Completed! 🎉")
            .setMessage("Congratulations! You've successfully completed this phase. Keep up the great work!")
            .setPositiveButton("Continue") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
