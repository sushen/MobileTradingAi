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
import com.shaplachottor.lab.adapters.ContentBlockAdapter
import com.shaplachottor.lab.databinding.FragmentLessonDetailBinding
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.viewmodels.ClassroomViewModel
import com.shaplachottor.lab.viewmodels.ClassroomViewModelFactory

class LessonDetailFragment : Fragment() {

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
        viewModel = ViewModelProvider(requireActivity(), factory)[ClassroomViewModel::class.java]

        setupToolbar()
        setupObservers()
        
        viewModel.selectLesson(args.phaseId, args.lessonId)
    }

    private fun setupToolbar() {
        binding.toolbarLessonDetail.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupObservers() {
        viewModel.selectedLesson.observe(viewLifecycleOwner) { lesson ->
            lesson?.let {
                binding.tvLessonDetailTitle.text = it.title
                binding.rvContentBlocks.layoutManager = LinearLayoutManager(requireContext())
                binding.rvContentBlocks.adapter = ContentBlockAdapter(it.contentBlocks)
                
                binding.btnCompleteLesson.isEnabled = !it.isCompleted
                binding.btnCompleteLesson.text = if (it.isCompleted) "Completed" else "Mark as Complete"
                
                binding.btnCompleteLesson.setOnClickListener {
                    binding.btnCompleteLesson.isEnabled = false
                    viewModel.completeLesson(args.phaseId, args.lessonId)
                }
            }
        }

        viewModel.operationStatus.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    is ClassroomViewModel.OperationResult.Success -> {
                        Toast.makeText(context, "Lesson completed!", Toast.LENGTH_SHORT).show()
                    }
                    is ClassroomViewModel.OperationResult.Error -> {
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
