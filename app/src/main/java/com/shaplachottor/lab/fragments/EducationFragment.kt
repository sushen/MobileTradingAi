package com.shaplachottor.lab.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.shaplachottor.lab.R
import com.shaplachottor.lab.adapters.PhaseAdapter
import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.databinding.FragmentEducationBinding
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.repositories.PhaseRepository
import kotlinx.coroutines.launch

class EducationFragment : Fragment() {
    private var _binding: FragmentEducationBinding? = null
    private val binding get() = _binding!!
    private val phaseRepository = PhaseRepository()
    private val appStore = AppGraph.appStore()

    private var allPhases: List<Phase> = emptyList()
    private var filterType: String = Phase.TYPE_FREE

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEducationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        loadPhases()
    }

    private fun setupTabs() {
        binding.tabLayoutEducation.addTab(binding.tabLayoutEducation.newTab().setText("Free Phases"))
        binding.tabLayoutEducation.addTab(binding.tabLayoutEducation.newTab().setText("Premium Cohorts"))

        binding.tabLayoutEducation.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterType = if (tab?.position == 0) Phase.TYPE_FREE else Phase.TYPE_PREMIUM
                updateList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadPhases() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                allPhases = phaseRepository.getPhases()
                updateList()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load courses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateList() {
        val filtered = allPhases.filter { it.type == filterType && it.isVisible }
        val userId = AppGraph.authSessionProvider().currentUser()?.uid ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val user = appStore.getUser(userId)
            val bookings = phaseRepository.getCurrentUserBookings(filtered)
            val learningJourneyProgress = phaseRepository.getLearningJourneyProgress(user)
            val phaseSnapshots = phaseRepository.getPhaseProgressionSnapshots(
                phases = filtered,
                currentUser = user,
                bookingStates = bookings,
                learningJourneyProgress = learningJourneyProgress
            )

            binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
            binding.rvCourses.adapter = PhaseAdapter(
                phaseSnapshots = phaseSnapshots
            ) { snapshot ->
                val navController = findNavController()
                if (navController.currentDestination?.id != R.id.educationFragment) {
                    return@PhaseAdapter
                }
                if (snapshot.canEnterClassroom) {
                    val action = EducationFragmentDirections.actionEducationFragmentToClassroomFragment(snapshot.phase.phaseId)
                    navController.navigate(action)
                } else {
                    val action = EducationFragmentDirections.actionEducationFragmentToPhasesFragment(snapshot.phase.phaseId)
                    navController.navigate(action)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
