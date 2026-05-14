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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shaplachottor.lab.databinding.DialogBookingRequestBinding
import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.BookingRequestOutcome
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User
import com.shaplachottor.lab.repositories.PhaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class EducationFragment : Fragment() {
    private var _binding: FragmentEducationBinding? = null
    private val binding get() = _binding!!
    private val phaseRepository = PhaseRepository()
    private val appStore = AppGraph.appStore()

    private var allPhases: List<Phase> = emptyList()
    private var filterType: String = Phase.TYPE_FREE
    private var observationJob: Job? = null

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
                startObservingData() // Re-trigger UI update with current data
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadPhases() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                allPhases = phaseRepository.getPhases()
                startObservingData()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load courses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startObservingData() {
        val userId = AppGraph.authSessionProvider().currentUser()?.uid ?: return
        observationJob?.cancel()
        observationJob = viewLifecycleOwner.lifecycleScope.launch {
            combine(
                appStore.getUserStream(userId),
                phaseRepository.observeCurrentUserBookings()
            ) { user, bookings ->
                user to bookings
            }.collect { (user, bookings) ->
                updateList(user, bookings)
            }
        }
    }

    private suspend fun updateList(user: User? = null, bookings: Map<String, Booking>? = null) {
        if (allPhases.isEmpty()) return
        
        val filtered = allPhases.filter { it.type == filterType && it.isVisible }
        val userId = AppGraph.authSessionProvider().currentUser()?.uid ?: return

        val currentUser = user ?: appStore.getUser(userId)
        val currentBookings = bookings ?: phaseRepository.getCurrentUserBookings(filtered)
        val learningJourneyProgress = phaseRepository.getLearningJourneyProgress(currentUser)
        val phaseSnapshots = phaseRepository.getPhaseProgressionSnapshots(
            phases = filtered,
            currentUser = currentUser,
            bookingStates = currentBookings,
            learningJourneyProgress = learningJourneyProgress
        )

        binding.rvCourses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCourses.adapter = PhaseAdapter(
            phaseSnapshots = phaseSnapshots,
            onPhaseClick = { snapshot ->
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
            },
            onRequestSeat = { phase ->
                showBookingRequestDialog(phase)
            }
        )
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
                    Toast.makeText(requireContext(), "WhatsApp number required", Toast.LENGTH_SHORT).show()
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
                updateList()
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
