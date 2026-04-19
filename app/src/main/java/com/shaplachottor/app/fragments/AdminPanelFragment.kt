package com.shaplachottor.app.fragments

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
import com.shaplachottor.app.data.AppGraph
import com.shaplachottor.app.databinding.FragmentAdminPanelBinding
import com.shaplachottor.app.databinding.ItemBookingRequestBinding
import com.shaplachottor.app.models.Booking
import com.shaplachottor.app.models.User
import kotlinx.coroutines.launch
import java.util.Date

class AdminPanelFragment : Fragment() {
    private var _binding: FragmentAdminPanelBinding? = null
    private val binding get() = _binding!!
    
    private val appStore = AppGraph.appStore()
    private val authProvider = AppGraph.authSessionProvider()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Security check
        val currentUserEmail = authProvider.currentUser()?.email
        if (currentUserEmail != "sushen.biswas.aga@gmail.com") {
            Toast.makeText(requireContext(), "Access Denied", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
            return
        }

        binding.rvRequests.layoutManager = LinearLayoutManager(requireContext())
        loadRequests()
    }

    private fun loadRequests() {
        viewLifecycleOwner.lifecycleScope.launch {
            val requests = appStore.getPendingBookings()
            if (requests.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvRequests.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvRequests.visibility = View.VISIBLE
                binding.rvRequests.adapter = BookingRequestAdapter(
                    requests = requests,
                    onApprove = { booking -> approveBooking(booking) },
                    onReject = { booking -> rejectBooking(booking) }
                )
            }
        }
    }

    private fun approveBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Update Booking status
                val updatedBooking = booking.copy(status = Booking.STATUS_APPROVED)
                appStore.setBooking(updatedBooking)

                // 2. Unlock Phase for User
                val user = appStore.getUser(booking.userId)
                if (user != null) {
                    val updatedUnlockedPhases = user.unlockedPhases.toMutableList()
                    if (!updatedUnlockedPhases.contains(booking.phaseId)) {
                        updatedUnlockedPhases.add(booking.phaseId)
                    }
                    val updatedUser = user.copy(unlockedPhases = updatedUnlockedPhases)
                    appStore.setUser(updatedUser)
                }

                Toast.makeText(requireContext(), "Approved successfully", Toast.LENGTH_SHORT).show()
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to approve: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejectBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val updatedBooking = booking.copy(status = Booking.STATUS_REJECTED)
                appStore.setBooking(updatedBooking)
                Toast.makeText(requireContext(), "Rejected successfully", Toast.LENGTH_SHORT).show()
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to reject: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class BookingRequestAdapter(
        private val requests: List<Booking>,
        private val onApprove: (Booking) -> Unit,
        private val onReject: (Booking) -> Unit
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
                // We don't have user email in Booking, so we might need to fetch it or just show UID
                tvUserEmail.text = "User ID: ${request.userId}"
                tvPhaseId.text = "Phase: ${request.phaseId}"
                tvContactInfo.text = "P: ${request.phoneNumber} | W: ${request.whatsappNumber}"
                tvExpiresAt.text = "Expires at: ${timeFormatter.format(Date(request.expiresAt))}"
                
                btnApprove.setOnClickListener { onApprove(request) }
                btnReject.setOnClickListener { onReject(request) }
            }
        }

        override fun getItemCount() = requests.size
    }
}
