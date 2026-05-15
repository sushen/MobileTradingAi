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
import com.shaplachottor.lab.R
import com.shaplachottor.lab.adapters.PhaseAdapter
import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.databinding.DialogBookingRequestBinding
import com.shaplachottor.lab.databinding.FragmentPhasesBinding
import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.BookingRequestOutcome
import com.shaplachottor.lab.models.LearningJourneyProgress
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.PhaseProgressionSnapshot
import com.shaplachottor.lab.models.PhaseProgressionState
import com.shaplachottor.lab.models.User
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.viewmodels.PhaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class PhasesFragment : Fragment() {

    companion object {
        private const val TAG = "PhasesFragment"
    }

    private var _binding: FragmentPhasesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PhaseViewModel
    private val phaseRepository = PhaseRepository()
    private val appStore = AppGraph.appStore()
    private val authProvider = AppGraph.authSessionProvider()

    private var currentUser: User? = null
    private var learningJourneyProgress: LearningJourneyProgress? = null
    private var allPhases: List<Phase> = emptyList()
    private var visiblePhases: List<Phase> = emptyList()
    private var currentBookingStates: Map<String, Booking> = emptyMap()
    private var allPhaseSnapshots: List<PhaseProgressionSnapshot> = emptyList()
    private var visiblePhaseSnapshots: List<PhaseProgressionSnapshot> = emptyList()
    private var hasObservedUserStream = false
    private var hasObservedBookingUpdates = false
    private var lastUnlockedPhaseIds: Set<String> = emptySet()
    private var lastBookingStatusByPhase: Map<String, String> = emptyMap()
    private var refreshUiJob: Job? = null
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
        observeViewModel()
        observeCurrentUser()
        viewModel.loadPhases()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPhases()
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
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) = Unit
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

    private fun observeCurrentUser() {
        val userId = authProvider.currentUser()?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                appStore.getUserStream(userId).collectLatest { user ->
                    val previousUnlocked = lastUnlockedPhaseIds
                    currentUser = user
                    learningJourneyProgress = phaseRepository.getLearningJourneyProgress(user)
                    lastUnlockedPhaseIds = user?.unlockedPhases.orEmpty().toSet()

                    if (hasObservedUserStream) {
                        val newlyUnlocked = lastUnlockedPhaseIds - previousUnlocked
                        if (newlyUnlocked.isNotEmpty()) {
                            val unlockedPhase = allPhases.firstOrNull { newlyUnlocked.contains(it.phaseId) }
                            val unlockedTitle = unlockedPhase?.title ?: newlyUnlocked.first()
                            android.util.Log.d(
                                TAG,
                                "Teacher approval synced to learner: unlockedPhases=$newlyUnlocked, activePhase=${learningJourneyProgress?.activePhaseId}"
                            )
                            context?.let {
                                Toast.makeText(
                                    it,
                                    "$unlockedTitle is approved. You can enter the classroom now.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    hasObservedUserStream = true
                    refreshUi()
                }
            } catch (e: Exception) {
                android.util.Log.d(TAG, "User stream closed: ${e.message}")
            }
        }
    }

    private fun observeViewModel() {
        viewModel.allPhases.observe(viewLifecycleOwner) { phases ->
            allPhases = phases.sortedBy { it.order }
            handlePassedPhaseId()
            refreshUi()
        }

        viewModel.phases.observe(viewLifecycleOwner) { phases ->
            visiblePhases = phases
            refreshUi()
        }

        viewModel.bookingStates.observe(viewLifecycleOwner) { bookingStates ->
            if (hasObservedBookingUpdates) {
                detectBookingStatusTransitions(bookingStates)
            }
            currentBookingStates = bookingStates
            lastBookingStatusByPhase = bookingStates.mapValues { it.value.status }
            hasObservedBookingUpdates = true
            refreshUi()
        }

        viewModel.bookingResult.observe(viewLifecycleOwner) { result ->
            when (result.outcome) {
                BookingRequestOutcome.REQUEST_CREATED -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Request Sent")
                        .setMessage(
                            "Your request has been sent to the teacher. The next classroom stays locked until the teacher reviews your readiness and approves access."
                        )
                        .setPositiveButton("OK", null)
                        .show()
                }

                BookingRequestOutcome.ALREADY_PENDING -> {
                    result.booking?.let(::showPendingApprovalDialog)
                }

                BookingRequestOutcome.ALREADY_APPROVED -> {
                    Toast.makeText(
                        context,
                        "This classroom is already approved. Enter it from the card.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                BookingRequestOutcome.NO_SEATS_AVAILABLE -> {
                    Toast.makeText(context, "No seats available for this classroom right now.", Toast.LENGTH_SHORT).show()
                }

                BookingRequestOutcome.INVALID_CONTACT_INFO -> {
                    Toast.makeText(
                        context,
                        "WhatsApp number is required.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                BookingRequestOutcome.FAILED -> {
                    Toast.makeText(context, "Request failed: ${result.error ?: result.outcome}. Please try again.", Toast.LENGTH_LONG).show()
                }

                BookingRequestOutcome.PREREQUISITE_NOT_MET -> {
                    Toast.makeText(
                        context,
                        "Complete the current classroom to 100% before requesting the next phase.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun detectBookingStatusTransitions(bookingStates: Map<String, Booking>) {
        bookingStates.forEach { (phaseId, booking) ->
            val previousStatus = lastBookingStatusByPhase[phaseId]
            if (previousStatus == null || previousStatus == booking.status) {
                return@forEach
            }

            android.util.Log.d(
                TAG,
                "Booking status changed: phaseId=$phaseId, previousStatus=$previousStatus, newStatus=${booking.status}"
            )

            when (booking.status) {
                Booking.STATUS_REVIEWING -> {
                    context?.let {
                        Toast.makeText(
                            it,
                            "Teacher started reviewing your request for ${phaseTitle(phaseId)}.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                Booking.STATUS_REJECTED -> {
                    context?.let {
                        Toast.makeText(
                            it,
                            "Your request for ${phaseTitle(phaseId)} was rejected. Continue practicing, then request again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun phaseTitle(phaseId: String): String {
        return allPhases.firstOrNull { it.phaseId == phaseId }?.title ?: phaseId
    }

    private fun handlePassedPhaseId() {
        val targetPhaseId = args.phaseId ?: return
        val targetPhase = allPhases.find { it.phaseId == targetPhaseId } ?: return
        val tabIndex = when (targetPhase.level) {
            "Beginner" -> 0
            "Intermediate" -> 1
            "Advanced" -> 2
            else -> 0
        }
        _binding?.tabLayoutLevels?.getTabAt(tabIndex)?.select()
    }

    private fun refreshUi() {
        refreshUiJob?.cancel()
        if (_binding == null) return

        refreshUiJob = viewLifecycleOwner.lifecycleScope.launch {
            allPhaseSnapshots = if (allPhases.isEmpty()) {
                emptyList()
            } else {
                phaseRepository.getPhaseProgressionSnapshots(
                    phases = allPhases,
                    currentUser = currentUser,
                    bookingStates = currentBookingStates,
                    learningJourneyProgress = learningJourneyProgress
                )
            }

            val snapshotsById = allPhaseSnapshots.associateBy { it.phase.phaseId }
            visiblePhaseSnapshots = visiblePhases.mapNotNull { snapshotsById[it.phaseId] }

            val activeBinding = _binding ?: return@launch
            activeBinding.rvPhases.adapter = PhaseAdapter(
                phaseSnapshots = visiblePhaseSnapshots,
                onPhaseClick = { snapshot -> handlePhaseClick(snapshot) },
                onRequestSeat = { phase ->
                    showBookingRequestDialog(phase)
                }
            )
            updateProgressSummary()
        }
    }

    private fun updateProgressSummary() {
        val activeBinding = _binding ?: return
        activeBinding.tvCurrentTrack.text = "Current Track: ${viewModel.selectedLevel}"

        val overallProgress = learningJourneyProgress?.overallLearningProgress
        if (overallProgress != null) {
            activeBinding.tvOverallProgress.text =
                "Overall Learning: ${overallProgress.percent}% • ${overallProgress.completedPhases}/${overallProgress.totalPhases} phases complete"
        } else {
            activeBinding.tvOverallProgress.text = "Overall Learning: 0%"
        }

        val activePhase = learningJourneyProgress?.currentPhaseProgress?.phase
        val activePhasePercent = learningJourneyProgress?.currentPhaseProgress?.progress?.percent
        val nextRequestable = allPhaseSnapshots.firstOrNull {
            it.state == PhaseProgressionState.READY_FOR_REQUEST
        }
        val pendingRequest = allPhaseSnapshots.firstOrNull { it.state == PhaseProgressionState.REQUEST_PENDING }

        activeBinding.tvNextPhase.text = when {
            activePhase != null && activePhasePercent != null ->
                "Active Phase: ${activePhase.title} ($activePhasePercent%)"
            pendingRequest != null ->
                "Next Step: Awaiting Teacher Review"
            nextRequestable != null ->
                "Next Step: Request ${nextRequestable.phase.title}"
            overallProgress?.percent == 100 ->
                "Next Step: Journey Completed"
            else ->
                "Next Step: Continue Practicing"
        }

        android.util.Log.d(
            TAG,
            "Progress summary updated: activePhase=${learningJourneyProgress?.activePhaseId}, completedPhases=${learningJourneyProgress?.completedPhaseIds}, unlockedPhases=${currentUser?.unlockedPhases}, overallPercent=${learningJourneyProgress?.overallLearningProgress?.percent}"
        )
    }

    private fun handlePhaseClick(snapshot: PhaseProgressionSnapshot) {
        val phase = snapshot.phase
        when {
            snapshot.canEnterClassroom -> {
                android.util.Log.d(TAG, "Entering classroom: phaseId=${phase.phaseId}, state=${snapshot.state}")
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.phasesFragment) {
                    val action = PhasesFragmentDirections.actionPhasesFragmentToClassroomFragment(phase.phaseId)
                    navController.navigate(action)
                }
            }

            snapshot.state == PhaseProgressionState.REQUEST_PENDING -> {
                snapshot.booking?.let(::showPendingApprovalDialog)
            }

            else -> {
                context?.let {
                    Toast.makeText(it, snapshot.statusMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun showBookingRequestDialog(phase: Phase) {
        val dialogBinding = DialogBookingRequestBinding.inflate(layoutInflater)
        
        currentUser?.whatsappNumber?.let {
            dialogBinding.etWhatsappNumber.setText(it)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Request Seat")
            .setMessage("Please provide your WhatsApp number for ${phase.title}")
            .setView(dialogBinding.root)
            .setPositiveButton("Submit") { _, _ ->
                val whatsapp = dialogBinding.etWhatsappNumber.text.toString().trim()
                if (whatsapp.isNotEmpty()) {
                    viewModel.requestSeat(phase, whatsapp)
                } else {
                    Toast.makeText(requireContext(), "WhatsApp number is required.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPendingApprovalDialog(booking: Booking) {
        val message = when (booking.status) {
            Booking.STATUS_REVIEWING ->
                "Teacher is reviewing your readiness for ${phaseTitle(booking.phaseId)}. Access stays locked until approval."
            else -> {
                val formattedExpiryTime = DateFormat.getTimeFormat(requireContext())
                    .format(Date(booking.expiresAt))
                "Your request is waiting for teacher pickup until $formattedExpiryTime. Access stays locked until approval."
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Awaiting Teacher Review")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        refreshUiJob?.cancel()
        refreshUiJob = null
        super.onDestroyView()
        _binding = null
    }
}
