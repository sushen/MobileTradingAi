package com.shaplachottor.lab.fragments

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shaplachottor.lab.adapters.PhaseAdapter
import com.shaplachottor.lab.databinding.DialogBookingRequestBinding
import com.shaplachottor.lab.databinding.FragmentPhasesBinding
import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.BookingRequestOutcome
import com.shaplachottor.lab.models.LearningJourneyProgress
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User
import com.shaplachottor.lab.repository.UserRepository
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.viewmodels.PhaseViewModel
import kotlinx.coroutines.launch
import java.util.Date

class PhasesFragment : Fragment() {

    private var _binding: FragmentPhasesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PhaseViewModel
    private val userRepository = UserRepository()
    private val phaseRepository = PhaseRepository()
    private var currentUser: User? = null
    private var learningJourneyProgress: LearningJourneyProgress? = null
    private var allPhases: List<Phase> = emptyList()
    private var visiblePhases: List<Phase> = emptyList()
    private var currentBookingStates: Map<String, Booking> = emptyMap()
    private val args: PhasesFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhasesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupTabs()
        binding.rvPhases.layoutManager = LinearLayoutManager(context)
        fetchUserAndPhases()
        observeViewModel()
        
        handlePassedPhaseId()
    }

    private fun handlePassedPhaseId() {
        val targetPhaseId = args.phaseId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            // Ensure phases are loaded
            val phases = viewModel.phases.value ?: phaseRepository.getPhases()
            val targetPhase = phases.find { it.phaseId == targetPhaseId }
            targetPhase?.let { phase ->
                val tabIndex = when (phase.level) {
                    "Beginner" -> 0
                    "Intermediate" -> 1
                    "Advanced" -> 2
                    else -> 0
                }
                binding.tabLayoutLevels.getTabAt(tabIndex)?.select()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchUserAndPhases()
    }

    private fun setupTabs() {
        val beginnerTab = binding.tabLayoutLevels.newTab().setText("Beginner")
        binding.tabLayoutLevels.addTab(beginnerTab)
        binding.tabLayoutLevels.addTab(binding.tabLayoutLevels.newTab().setText("Intermediate"))
        binding.tabLayoutLevels.addTab(binding.tabLayoutLevels.newTab().setText("Advanced"))

        binding.tabLayoutLevels.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                viewModel.filterByLevel(tab?.text.toString())
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        beginnerTab.select()
    }

    private fun setupViewModel() {
        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PhaseViewModel(phaseRepository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[PhaseViewModel::class.java]
    }

    private fun fetchUserAndPhases() {
        viewLifecycleOwner.lifecycleScope.launch {
            currentUser = userRepository.getCurrentUserOrNull()
            allPhases = phaseRepository.getPhases().sortedBy { it.order }
            learningJourneyProgress = phaseRepository.getLearningJourneyProgress(currentUser)
            viewModel.loadPhases()
            updateProgressSummary()
            renderPhases()
        }
    }

    private fun observeViewModel() {
        viewModel.phases.observe(viewLifecycleOwner) { phases ->
            visiblePhases = phases
            renderPhases()
            updateProgressSummary()
        }

        viewModel.bookingStates.observe(viewLifecycleOwner) { bookingStates ->
            currentBookingStates = bookingStates
            renderPhases()
            updateProgressSummary()
        }

        viewModel.bookingResult.observe(viewLifecycleOwner) { result ->
            when (result.outcome) {
                BookingRequestOutcome.REQUEST_CREATED -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Request Sent")
                        .setMessage(
                            "Your booking request is pending manual approval. Someone from our team will call you on WhatsApp to confirm the booking. If it is not approved within 15 minutes, it will expire automatically."
                        )
                        .setPositiveButton("OK") { _, _ ->
                            fetchUserAndPhases()
                        }
                        .show()
                }

                BookingRequestOutcome.ALREADY_PENDING -> {
                    result.booking?.let(::showPendingApprovalDialog)
                }

                BookingRequestOutcome.ALREADY_APPROVED -> {
                    fetchUserAndPhases()
                    Toast.makeText(
                        context,
                        "This request is already approved. Refreshing your access now.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                BookingRequestOutcome.NO_SEATS_AVAILABLE -> {
                    Toast.makeText(context, "No seats available for this phase!", Toast.LENGTH_SHORT).show()
                }

                BookingRequestOutcome.INVALID_CONTACT_INFO -> {
                    Toast.makeText(
                        context,
                        "Phone number and WhatsApp number are required.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                BookingRequestOutcome.FAILED -> {
                    Toast.makeText(context, "Booking request failed. Please try again.", Toast.LENGTH_SHORT).show()
                }

                BookingRequestOutcome.PREREQUISITE_NOT_MET -> {
                    Toast.makeText(
                        context,
                        "Please complete the previous phase before booking this one.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateProgressSummary() {
        binding.tvCurrentTrack.text = "Current Track: ${viewModel.selectedLevel}"

        val overallProgress = learningJourneyProgress?.overallLearningProgress
        if (overallProgress != null) {
            binding.tvOverallProgress.text =
                "Overall Learning: ${overallProgress.percent}% • ${overallProgress.completedPhases}/${overallProgress.totalPhases} phases complete"
        } else {
            binding.tvOverallProgress.text = "Overall Learning: 0%"
        }

        val activePhase = learningJourneyProgress?.currentPhaseProgress?.phase
        val activePhasePercent = learningJourneyProgress?.currentPhaseProgress?.progress?.percent
        val nextPhase = allPhases.firstOrNull { phase ->
            !learningJourneyProgress?.completedPhaseIds.orEmpty().contains(phase.phaseId) &&
                !currentUser?.unlockedPhases.orEmpty().contains(phase.phaseId) &&
                phase.type != Phase.TYPE_FREE
        }

        binding.tvNextPhase.text = when {
            activePhase != null && activePhasePercent != null ->
                "Active Phase: ${activePhase.title} ($activePhasePercent%)"
            nextPhase != null ->
                "Next Phase: ${nextPhase.title}"
            overallProgress?.percent == 100 ->
                "Next Phase: Journey Completed"
            else ->
                "Next Phase: Awaiting Unlock"
        }

        android.util.Log.d(
            "PhasesFragment",
            "Progress summary updated: activePhase=${learningJourneyProgress?.activePhaseId}, completedPhases=${learningJourneyProgress?.completedPhaseIds}, unlockedPhases=${currentUser?.unlockedPhases}, overallPercent=${learningJourneyProgress?.overallLearningProgress?.percent}"
        )
    }

    private fun renderPhases() {
        if (_binding == null) return

        binding.rvPhases.adapter = PhaseAdapter(
            visiblePhases,
            currentUser?.unlockedPhases ?: emptyList(),
            learningJourneyProgress?.completedPhaseIds.orEmpty(),
            currentBookingStates
        ) { phase ->
            handlePhaseClick(phase)
        }
    }

    private fun handlePhaseClick(phase: Phase) {
        if (phase.type == Phase.TYPE_FREE || currentUser?.unlockedPhases?.contains(phase.phaseId) == true) {
            val action = PhasesFragmentDirections.actionPhasesFragmentToClassroomFragment(phase.phaseId)
            findNavController().navigate(action)
            return
        }

        when (currentBookingStates[phase.phaseId]?.status) {
            Booking.STATUS_PENDING -> {
                currentBookingStates[phase.phaseId]?.let(::showPendingApprovalDialog)
            }

            Booking.STATUS_APPROVED -> {
                fetchUserAndPhases()
                Toast.makeText(
                    context,
                    "Approval detected. Refreshing your unlocked phases.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                if (phase.availableSeats > 0) {
                    showBookingRequestDialog(phase)
                } else {
                    Toast.makeText(context, "No seats available for this phase!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showBookingRequestDialog(phase: Phase) {
        val dialogBinding = DialogBookingRequestBinding.inflate(layoutInflater)
        val existingBooking = currentBookingStates[phase.phaseId]
        if (existingBooking != null) {
            dialogBinding.etWhatsappNumber.setText(existingBooking.whatsappNumber)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Request Seat")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Submit Request", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val whatsappNumber = dialogBinding.etWhatsappNumber.text?.toString().orEmpty().trim()

                dialogBinding.inputLayoutWhatsappNumber.error =
                    if (whatsappNumber.isBlank()) "WhatsApp number is required." else null

                if (whatsappNumber.isBlank()) {
                    return@setOnClickListener
                }

                dialog.dismiss()
                viewModel.requestSeat(phase, whatsappNumber, whatsappNumber)
            }
        }

        dialog.show()
    }

    private fun showPendingApprovalDialog(booking: Booking) {
        val formattedExpiryTime = DateFormat.getTimeFormat(requireContext())
            .format(Date(booking.expiresAt))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Approval Pending")
            .setMessage(
                "Your request is waiting for manual approval until $formattedExpiryTime. Someone from our team will call you on WhatsApp to confirm your booking before unlocking this classroom."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
