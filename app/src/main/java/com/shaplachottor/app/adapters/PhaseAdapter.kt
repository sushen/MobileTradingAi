package com.shaplachottor.app.adapters

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shaplachottor.app.databinding.ItemPhaseBinding
import com.shaplachottor.app.models.Booking
import com.shaplachottor.app.models.Phase
import java.util.Date

class PhaseAdapter(
    private val phases: List<Phase>,
    private val userUnlockedPhases: List<String>,
    private val bookingStates: Map<String, Booking>,
    private val onPhaseClick: (Phase) -> Unit
) : RecyclerView.Adapter<PhaseAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPhaseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhaseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val phase = phases[position]
        val isUnlocked = userUnlockedPhases.contains(phase.phaseId)
        val booking = bookingStates[phase.phaseId]
        val context = holder.binding.root.context
        val timeFormatter = DateFormat.getTimeFormat(context)
        
        holder.binding.apply {
            tvPhaseTitle.text = phase.title
            tvPhaseDescription.text = phase.description
            if (phase.focus.isBlank()) {
                tvPhaseFocus.visibility = View.GONE
            } else {
                tvPhaseFocus.visibility = View.VISIBLE
                tvPhaseFocus.text = "Focus: ${phase.focus}"
            }

            if (phase.identityShift.isBlank()) {
                tvPhaseIdentity.visibility = View.GONE
            } else {
                tvPhaseIdentity.visibility = View.VISIBLE
                tvPhaseIdentity.text = phase.identityShift
            }

            tvSeatsAvailable.text = "Seats available: ${phase.availableSeats} / ${phase.totalSeats}"
            
            if (isUnlocked) {
                ivLockStatus.setImageResource(com.shaplachottor.app.R.drawable.ic_lock_open)
                btnBookPhase.visibility = View.GONE
                tvBookingStatus.visibility = View.GONE
            } else {
                ivLockStatus.setImageResource(com.shaplachottor.app.R.drawable.ic_lock_closed)
                btnBookPhase.visibility = View.VISIBLE

                when (booking?.status) {
                    Booking.STATUS_PENDING -> {
                        btnBookPhase.isEnabled = true
                        btnBookPhase.text = "Pending Approval"
                        tvBookingStatus.visibility = View.VISIBLE
                        tvBookingStatus.text = "Approval pending until ${
                            timeFormatter.format(Date(booking.expiresAt))
                        }"
                    }

                    Booking.STATUS_APPROVED -> {
                        btnBookPhase.isEnabled = true
                        btnBookPhase.text = "Refresh Access"
                        tvBookingStatus.visibility = View.VISIBLE
                        tvBookingStatus.text = "Approved. Refreshing your access will unlock the classroom."
                    }

                    Booking.STATUS_EXPIRED -> {
                        btnBookPhase.isEnabled = phase.availableSeats > 0
                        btnBookPhase.text = if (phase.availableSeats > 0) "Request Again" else "Full"
                        tvBookingStatus.visibility = View.VISIBLE
                        tvBookingStatus.text = "Your previous request expired. Submit again to rejoin the queue."
                    }

                    else -> {
                        btnBookPhase.isEnabled = phase.availableSeats > 0
                        btnBookPhase.text = if (phase.availableSeats > 0) "Book Seat" else "Full"
                        tvBookingStatus.visibility = View.GONE
                    }
                }
            }

            btnBookPhase.setOnClickListener { onPhaseClick(phase) }
            root.setOnClickListener { if (isUnlocked) onPhaseClick(phase) }
        }
    }

    override fun getItemCount() = phases.size
}
