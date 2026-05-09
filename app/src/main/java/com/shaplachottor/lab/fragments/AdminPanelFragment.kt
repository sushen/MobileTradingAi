package com.shaplachottor.lab.fragments

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.databinding.FragmentAdminPanelBinding
import com.shaplachottor.lab.databinding.ItemBookingRequestBinding
import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.repositories.PhaseRepository
import kotlinx.coroutines.launch
import java.util.Date

class AdminPanelFragment : Fragment() {
    companion object {
        private const val TAG = "AdminPanelFragment"
    }

    private var _binding: FragmentAdminPanelBinding? = null
    private val binding get() = _binding!!

    private val appStore = AppGraph.appStore()
    private val authProvider = AppGraph.authSessionProvider()
    private val phaseRepository = PhaseRepository()
    private var currentTab = 0 // 0: Open Requests, 1: All

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUserEmail = authProvider.currentUser()?.email
        if (currentUserEmail != "sushen.biswas.aga@gmail.com") {
            Toast.makeText(requireContext(), "Access Denied", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            return
        }

        binding.rvRequests.layoutManager = LinearLayoutManager(requireContext())

        binding.tabLayoutAdmin.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                loadRequests()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })

        loadRequests()
    }

    private fun loadRequests() {
        viewLifecycleOwner.lifecycleScope.launch {
            val allRequests = appStore.getAllBookings()
                .map { normalizeExpiredOpenRequest(it) }
                .sortedByDescending { it.lastUpdatedAt.takeIf { updatedAt -> updatedAt > 0L } ?: it.createdAt }

            val requests = if (currentTab == 0) {
                allRequests.filter { it.status == Booking.STATUS_PENDING || it.status == Booking.STATUS_REVIEWING }
            } else {
                allRequests
            }

            android.util.Log.d(
                TAG,
                "Admin request load: tab=$currentTab, requests=${requests.map { "${it.bookingId}:${it.status}" }}"
            )

            if (requests.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvRequests.visibility = View.GONE
                binding.tvEmptyState.text = if (currentTab == 0) {
                    "No open requests"
                } else {
                    "No requests found"
                }
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvRequests.visibility = View.VISIBLE
                binding.rvRequests.adapter = BookingRequestAdapter(
                    requests = requests,
                    onReview = { startReview(it) },
                    onApprove = { approveBooking(it) },
                    onReject = { rejectBooking(it) },
                    onCancel = { cancelBooking(it) }
                )
            }
        }
    }

    private suspend fun normalizeExpiredOpenRequest(booking: Booking): Booking {
        if (booking.status == Booking.STATUS_PENDING &&
            booking.expiresAt > 0L &&
            booking.expiresAt <= System.currentTimeMillis()
        ) {
            val expired = phaseRepository.expireBooking(booking.bookingId)
            if (expired) {
                android.util.Log.d(TAG, "Admin sync expired stale request: bookingId=${booking.bookingId}")
                return booking.copy(
                    status = Booking.STATUS_EXPIRED,
                    expiresAt = 0L,
                    lastUpdatedAt = System.currentTimeMillis()
                )
            }
        }
        return booking
    }

    private fun startReview(booking: Booking) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Start Teacher Review")
            .setMessage("Mark this request as under review after checking practical work, external assignments, and learner readiness.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Mark Reviewing") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val success = phaseRepository.markBookingReviewing(booking.bookingId)
                    if (success) {
                        Toast.makeText(requireContext(), "Request marked as reviewing", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Could not start review", Toast.LENGTH_SHORT).show()
                    }
                    loadRequests()
                }
            }
            .show()
    }

    private fun approveBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val phase = phaseRepository.getPhaseById(booking.phaseId)
                val user = appStore.getUser(booking.userId)
                val displayName = user?.let { "${it.name} (${it.email})" } ?: "UID: ${booking.userId}"

                if (phase?.type == Phase.TYPE_PREMIUM) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Approve Next Classroom")
                        .setMessage("Confirm payment and readiness for $displayName before unlocking ${phase.title}.")
                        .setPositiveButton("Approve Access") { _, _ ->
                            performApproval(booking)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    performApproval(booking)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performApproval(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                android.util.Log.d(TAG, "Admin approval requested: bookingId=${booking.bookingId}")
                val success = phaseRepository.approveBooking(booking.bookingId)
                if (success) {
                    Toast.makeText(requireContext(), "Phase unlocked successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Approval failed", Toast.LENGTH_SHORT).show()
                }
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejectBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                android.util.Log.d(TAG, "Admin rejection requested: bookingId=${booking.bookingId}")
                val success = phaseRepository.rejectBooking(booking.bookingId)
                if (success) {
                    Toast.makeText(requireContext(), "Request rejected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Rejection failed", Toast.LENGTH_SHORT).show()
                }
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                android.util.Log.d(TAG, "Admin revoke requested: bookingId=${booking.bookingId}")
                val success = phaseRepository.cancelBooking(booking.bookingId)
                if (success) {
                    Toast.makeText(requireContext(), "Access revoked successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Revocation failed", Toast.LENGTH_SHORT).show()
                }
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class BookingRequestAdapter(
        private val requests: List<Booking>,
        private val onReview: (Booking) -> Unit,
        private val onApprove: (Booking) -> Unit,
        private val onReject: (Booking) -> Unit,
        private val onCancel: (Booking) -> Unit
    ) : RecyclerView.Adapter<BookingRequestAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemBookingRequestBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemBookingRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val request = requests[position]
            val timeFormatter = DateFormat.getTimeFormat(requireContext())

            holder.binding.apply {
                viewLifecycleOwner.lifecycleScope.launch {
                    val user = appStore.getUser(request.userId)
                    tvUserEmail.text = if (user != null) {
                        "${user.name}\n${user.email}"
                    } else {
                        "UID: ${request.userId}"
                    }
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val phase = phaseRepository.getPhaseById(request.phaseId)
                    val completedPhaseLabel = resolveCompletedPhaseLabel(request, phase)
                    tvPhaseId.text = buildString {
                        append("Requested: ${phase?.title ?: request.phaseId}")
                        append("\nCompleted: $completedPhaseLabel")
                        append("\nStatus: ${request.status.uppercase()}")
                    }

                    if (phase != null && phase.type == Phase.TYPE_PREMIUM) {
                        tvPaymentInfo.visibility = View.VISIBLE
                        tvPaymentInfo.text = "Premium: ${phase.currency} ${phase.price}"
                    } else {
                        tvPaymentInfo.visibility = View.GONE
                    }
                }

                tvContactInfo.text = "WhatsApp: ${request.whatsappNumber}"

                when (request.status) {
                    Booking.STATUS_PENDING -> {
                        tvExpiresAt.visibility = View.VISIBLE
                        tvExpiresAt.text = "Seat hold until: ${timeFormatter.format(Date(request.expiresAt))}"
                        btnApprove.visibility = View.VISIBLE
                        btnApprove.text = "Start Review"
                        btnReject.visibility = View.VISIBLE
                        btnReject.text = "Reject"
                    }

                    Booking.STATUS_REVIEWING -> {
                        tvExpiresAt.visibility = View.VISIBLE
                        val reviewedAt = if (request.reviewedAt > 0L) {
                            timeFormatter.format(Date(request.reviewedAt))
                        } else {
                            "now"
                        }
                        tvExpiresAt.text = "Under review since: $reviewedAt"
                        btnApprove.visibility = View.VISIBLE
                        btnApprove.text = "Approve"
                        btnReject.visibility = View.VISIBLE
                        btnReject.text = "Reject"
                    }

                    Booking.STATUS_APPROVED -> {
                        tvExpiresAt.visibility = View.VISIBLE
                        val approvedAt = if (request.approvedAt > 0L) {
                            timeFormatter.format(Date(request.approvedAt))
                        } else {
                            "Completed"
                        }
                        tvExpiresAt.text = "Approved at: $approvedAt"
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.VISIBLE
                        btnReject.text = "Revoke Access"
                    }

                    else -> {
                        tvExpiresAt.visibility = View.GONE
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.GONE
                    }
                }

                btnApprove.setOnClickListener {
                    if (request.status == Booking.STATUS_PENDING) {
                        onReview(request)
                    } else {
                        onApprove(request)
                    }
                }
                btnReject.setOnClickListener {
                    if (request.status == Booking.STATUS_APPROVED) onCancel(request)
                    else onReject(request)
                }
                btnWhatsApp.setOnClickListener {
                    try {
                        val cleanedNumber = request.whatsappNumber.filter { it.isDigit() }
                        val url = "https://api.whatsapp.com/send?phone=$cleanedNumber"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                        intent.data = android.net.Uri.parse(url)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
                    }
                }
                btnCopy.setOnClickListener {
                    try {
                        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("WhatsApp Number", request.whatsappNumber)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireContext(), "Number copied: ${request.whatsappNumber}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Failed to copy", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private suspend fun resolveCompletedPhaseLabel(request: Booking, requestedPhase: Phase?): String {
            val completedPhaseId = request.completedPhaseId
                ?: requestedPhase
                    ?.let { phase ->
                        phaseRepository.getPhases().firstOrNull { it.order == phase.order - 1 }?.phaseId
                    }

            if (completedPhaseId.isNullOrBlank()) {
                return "Entry Phase"
            }

            val completedPhase = phaseRepository.getPhaseById(completedPhaseId)
            return completedPhase?.title ?: completedPhaseId
        }

        override fun getItemCount() = requests.size
    }
}
