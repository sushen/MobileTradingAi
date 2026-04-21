package com.shaplachottor.app.adapters

import android.graphics.Color
import android.os.CountDownTimer
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shaplachottor.app.databinding.ItemPhaseBinding
import com.shaplachottor.app.models.Booking
import com.shaplachottor.app.models.Phase
import java.util.Date
import java.util.Locale

class PhaseAdapter(
    private val phases: List<Phase>,
    private val userUnlockedPhases: List<String>,
    private val bookingStates: Map<String, Booking>,
    private val onPhaseClick: (Phase) -> Unit
) : RecyclerView.Adapter<PhaseAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPhaseBinding) : RecyclerView.ViewHolder(binding.root) {
        var timer: CountDownTimer? = null

        fun stopTimer() {
            timer?.cancel()
            timer = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhaseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.stopTimer()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val phase = phases[position]
        val isUnlocked = userUnlockedPhases.contains(phase.phaseId)
        val booking = bookingStates[phase.phaseId]
        val context = holder.binding.root.context
        
        holder.stopTimer()

        // Find missing prerequisite
        val previousPhase = if (position > 0) phases[position - 1] else null
        val isMissingPrerequisite = previousPhase != null && !userUnlockedPhases.contains(previousPhase.phaseId)

        holder.binding.apply {
            tvPhaseNumber.text = "Phase ${phase.order}"
            tvPhaseTitle.text = phase.title
            tvPhaseLevel.text = "Level: ${phase.level}"
            tvPhaseDescription.text = phase.description
            tvSeatsAvailable.text = "Seats available: ${phase.availableSeats} / ${phase.totalSeats}"

            val phaseState: String
            var statusMessage: String
            val buttonText: String
            var buttonEnabled = true
            var badgeColor = "#757575" // Grey for Locked

            when {
                isUnlocked -> {
                    phaseState = "UNLOCKED"
                    statusMessage = "Approved and unlocked."
                    buttonText = "Unlocked"
                    buttonEnabled = false
                    badgeColor = "#2E7D32" // Shapla Green
                }
                booking?.status == Booking.STATUS_PENDING -> {
                    phaseState = "PENDING"
                    badgeColor = "#D4AF37" // Soft Gold for importance
                    buttonText = "Waiting Approval"
                    buttonEnabled = false
                    
                    val remainingMillis = booking.expiresAt - System.currentTimeMillis()
                    if (remainingMillis > 0) {
                        holder.timer = object : CountDownTimer(remainingMillis, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                val minutes = (millisUntilFinished / 1000) / 60
                                val seconds = (millisUntilFinished / 1000) % 60
                                tvStatusMessage.text = String.format(Locale.getDefault(), "Wait for WhatsApp call. Expires in %02d:%02d", minutes, seconds)
                                tvStatusMessage.setTextColor(Color.parseColor("#D4AF37")) // Gold alert
                            }

                            override fun onFinish() {
                                tvStatusMessage.text = "Request expired. Please try again."
                                tvStatusMessage.setTextColor(Color.RED)
                                tvPhaseStateBadge.text = "EXPIRED"
                                tvPhaseStateBadge.background.setTint(Color.RED)
                            }
                        }.start()
                        statusMessage = "Calculating time..."
                    } else {
                        statusMessage = "Request expired. Please try again."
                        badgeColor = "#B00020" // Error Red
                    }
                }
                isMissingPrerequisite -> {
                    phaseState = "LOCKED"
                    statusMessage = "Complete ${previousPhase?.title} before requesting this phase."
                    buttonText = "Locked by Progress"
                    buttonEnabled = false
                }
                phase.availableSeats <= 0 -> {
                    phaseState = "LOCKED"
                    statusMessage = "No seats available for this phase right now."
                    buttonText = "No Seats Available"
                    buttonEnabled = false
                }
                else -> {
                    phaseState = "LOCKED"
                    statusMessage = when (booking?.status) {
                        Booking.STATUS_REJECTED -> "Your previous request was rejected. You can submit again."
                        Booking.STATUS_EXPIRED -> "Previous request expired. You can submit again."
                        else -> "Request access to unlock this phase."
                    }
                    buttonText = if (booking?.status == Booking.STATUS_REJECTED || booking?.status == Booking.STATUS_EXPIRED) {
                        "Request Again"
                    } else {
                        "Book Seat"
                    }
                    buttonEnabled = true
                }
            }

            tvPhaseStateBadge.text = phaseState
            tvPhaseStateBadge.background.setTint(Color.parseColor(badgeColor))
            tvStatusMessage.text = statusMessage
            btnAction.text = buttonText
            btnAction.isEnabled = buttonEnabled

            btnAction.setOnClickListener { onPhaseClick(phase) }
            root.setOnClickListener { if (isUnlocked) onPhaseClick(phase) }
        }
    }

    override fun getItemCount() = phases.size
}
