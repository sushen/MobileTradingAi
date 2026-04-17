package com.shaplachottor.app.fragments

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shaplachottor.app.adapters.PhaseAdapter
import com.shaplachottor.app.databinding.DialogBookingRequestBinding
import com.shaplachottor.app.databinding.FragmentPhasesBinding
import com.shaplachottor.app.models.Booking
import com.shaplachottor.app.models.BookingRequestOutcome
import com.shaplachottor.app.models.Phase
import com.shaplachottor.app.models.User
import com.shaplachottor.app.repository.UserRepository
import com.shaplachottor.app.repositories.PhaseRepository
import com.shaplachottor.app.viewmodels.PhaseViewModel
import kotlinx.coroutines.launch
import java.util.Date

class PhasesFragment : Fragment() {

    private var _binding: FragmentPhasesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PhaseViewModel
    private val userRepository = UserRepository()
    private var currentUser: User? = null
    private var visiblePhases: List<Phase> = emptyList()
    private var currentBookingStates: Map<String, Booking> = emptyMap()

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
    }

    override fun onResume() {
        super.onResume()
        fetchUserAndPhases()
    }

    private fun setupTabs() {
        binding.tabLayoutLevels.addTab(binding.tabLayoutLevels.newTab().setText("Beginner"))
        binding.tabLayoutLevels.addTab(binding.tabLayoutLevels.newTab().setText("Intermediate"))
        binding.tabLayoutLevels.addTab(binding.tabLayoutLevels.newTab().setText("Advanced"))

        binding.tabLayoutLevels.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                viewModel.filterByLevel(tab?.text.toString())
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupViewModel() {
        val repository = PhaseRepository()
        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PhaseViewModel(repository) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)[PhaseViewModel::class.java]
    }

    private fun fetchUserAndPhases() {
        viewLifecycleOwner.lifecycleScope.launch {
            currentUser = userRepository.getCurrentUserOrNull()
            viewModel.loadPhases()
            renderPhases()
        }
    }

    private fun observeViewModel() {
        viewModel.phases.observe(viewLifecycleOwner) { phases ->
            visiblePhases = phases
            renderPhases()
        }

        viewModel.bookingStates.observe(viewLifecycleOwner) { bookingStates ->
            currentBookingStates = bookingStates
            renderPhases()
        }

        viewModel.bookingResult.observe(viewLifecycleOwner) { result ->
            when (result.outcome) {
                BookingRequestOutcome.REQUEST_CREATED -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Request Sent")
                        .setMessage(
                            "Your booking request is pending manual approval. The admin will contact you on WhatsApp. If it is not approved within 15 minutes, it will expire automatically."
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
            }
        }
    }

    private fun renderPhases() {
        if (_binding == null) {
            return
        }

        binding.rvPhases.adapter = PhaseAdapter(
            visiblePhases,
            currentUser?.unlockedPhases ?: emptyList(),
            currentBookingStates
        ) { phase ->
            handlePhaseClick(phase)
        }
    }

    private fun handlePhaseClick(phase: Phase) {
        if (currentUser?.unlockedPhases?.contains(phase.phaseId) == true) {
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
            dialogBinding.etPhoneNumber.setText(existingBooking.phoneNumber)
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
                val phoneNumber = dialogBinding.etPhoneNumber.text?.toString().orEmpty().trim()
                val whatsappNumber = dialogBinding.etWhatsappNumber.text?.toString().orEmpty().trim()

                dialogBinding.inputLayoutPhoneNumber.error =
                    if (phoneNumber.isBlank()) "Phone number is required." else null
                dialogBinding.inputLayoutWhatsappNumber.error =
                    if (whatsappNumber.isBlank()) "WhatsApp number is required." else null

                if (phoneNumber.isBlank() || whatsappNumber.isBlank()) {
                    return@setOnClickListener
                }

                dialog.dismiss()
                viewModel.requestSeat(phase, phoneNumber, whatsappNumber)
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
                "Your request is waiting for manual approval until $formattedExpiryTime. The admin will contact you on WhatsApp before unlocking this classroom."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
