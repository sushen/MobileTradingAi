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
import com.google.android.material.tabs.TabLayout
import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.databinding.FragmentAdminPanelBinding
import com.shaplachottor.lab.databinding.ItemBookingRequestBinding
import com.shaplachottor.lab.models.Booking
import kotlinx.coroutines.launch
import java.util.Date

class AdminPanelFragment : Fragment() {
    private var _binding: FragmentAdminPanelBinding? = null
    private val binding get() = _binding!!
    
    private val appStore = AppGraph.appStore()
    private val authProvider = AppGraph.authSessionProvider()
    private var currentTab = 0 // 0: Pending, 1: All

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
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadRequests()
    }

    private fun loadRequests() {
        viewLifecycleOwner.lifecycleScope.launch {
            val requests = if (currentTab == 0) {
                appStore.getPendingBookings()
            } else {
                appStore.getAllBookings().sortedByDescending { it.createdAt }
            }

            if (requests.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvRequests.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvRequests.visibility = View.VISIBLE
                binding.rvRequests.adapter = BookingRequestAdapter(
                    requests = requests,
                    onApprove = { approveBooking(it) },
                    onReject = { rejectBooking(it) },
                    onCancel = { cancelBooking(it) }
                )
            }
        }
    }

    private fun approveBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val updatedBooking = booking.copy(status = Booking.STATUS_APPROVED)
                appStore.setBooking(updatedBooking)

                val user = appStore.getUser(booking.userId)
                if (user != null) {
                    val updatedUnlockedPhases = user.unlockedPhases.toMutableList()
                    if (!updatedUnlockedPhases.contains(booking.phaseId)) {
                        updatedUnlockedPhases.add(booking.phaseId)
                    }
                    appStore.setUser(user.copy(unlockedPhases = updatedUnlockedPhases))
                }

                Toast.makeText(requireContext(), "Approved successfully", Toast.LENGTH_SHORT).show()
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejectBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                appStore.setBooking(booking.copy(status = Booking.STATUS_REJECTED))
                Toast.makeText(requireContext(), "Rejected successfully", Toast.LENGTH_SHORT).show()
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelBooking(booking: Booking) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Mark booking as cancelled
                appStore.setBooking(booking.copy(status = Booking.STATUS_CANCELLED))

                // 2. Remove phase from user's unlocked list
                val user = appStore.getUser(booking.userId)
                if (user != null) {
                    val updatedUnlockedPhases = user.unlockedPhases.toMutableList()
                    updatedUnlockedPhases.remove(booking.phaseId)
                    appStore.setUser(user.copy(unlockedPhases = updatedUnlockedPhases))
                }

                Toast.makeText(requireContext(), "Seat cancelled successfully", Toast.LENGTH_SHORT).show()
                loadRequests()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                tvUserEmail.text = "UID: ${request.userId}"
                tvPhaseId.text = "Phase: ${request.phaseId}\nStatus: ${request.status.uppercase()}"
                tvContactInfo.text = "P: ${request.phoneNumber} | W: ${request.whatsappNumber}"
                
                if (request.status == Booking.STATUS_PENDING) {
                    tvExpiresAt.visibility = View.VISIBLE
                    tvExpiresAt.text = "Expires at: ${timeFormatter.format(Date(request.expiresAt))}"
                    btnApprove.visibility = View.VISIBLE
                    btnReject.visibility = View.VISIBLE
                    btnReject.text = "Reject"
                } else if (request.status == Booking.STATUS_APPROVED) {
                    tvExpiresAt.visibility = View.GONE
                    btnApprove.visibility = View.GONE
                    btnReject.visibility = View.VISIBLE
                    btnReject.text = "Cancel Seat"
                } else {
                    tvExpiresAt.visibility = View.GONE
                    btnApprove.visibility = View.GONE
                    btnReject.visibility = View.GONE
                }
                
                btnApprove.setOnClickListener { onApprove(request) }
                btnReject.setOnClickListener { 
                    if (request.status == Booking.STATUS_APPROVED) onCancel(request)
                    else onReject(request)
                }
            }
        }

        override fun getItemCount() = requests.size
    }
}
