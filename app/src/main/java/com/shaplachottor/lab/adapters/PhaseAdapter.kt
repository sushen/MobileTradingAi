package com.shaplachottor.lab.adapters

import android.graphics.Color
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shaplachottor.lab.databinding.ItemPhaseBinding
import com.shaplachottor.lab.models.Booking
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.PhaseProgressionSnapshot
import com.shaplachottor.lab.models.PhaseProgressionState
import java.util.Locale

class PhaseAdapter(
    private var phaseSnapshots: List<PhaseProgressionSnapshot>,
    private val onPhaseClick: (PhaseProgressionSnapshot) -> Unit,
    private val onRequestSeat: (Phase) -> Unit
) : RecyclerView.Adapter<PhaseAdapter.ViewHolder>() {

    fun updateData(newSnapshots: List<PhaseProgressionSnapshot>) {
        phaseSnapshots = newSnapshots
        notifyDataSetChanged()
    }

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
        val snapshot = phaseSnapshots[position]
        val phase = snapshot.phase
        val booking = snapshot.booking

        holder.stopTimer()

        holder.binding.apply {
            tvPhaseNumber.text = if (phase.type == Phase.TYPE_PREMIUM) {
                "Class ${phase.order} (Mentored)"
            } else {
                "Phase ${phase.order}"
            }
            tvPhaseTitle.text = phase.title
            tvPhaseLevel.text = "Level: ${phase.level}"
            tvPhaseDescription.text = phase.description
            tvSeatsAvailable.text = if (phase.type == Phase.TYPE_PREMIUM) {
                "Seat holds: ${phase.availableSeats} remaining out of ${phase.totalSeats}"
            } else {
                "Open classroom access"
            }

            tvPhaseStateBadge.text = snapshot.badgeLabel
            tvPhaseStateBadge.background.setTint(resolveBadgeColor(snapshot.state))

            tvStatusMessage.text = snapshot.statusMessage
            tvStatusMessage.setTextColor(
                if (snapshot.state == PhaseProgressionState.REQUEST_PENDING) {
                    Color.parseColor("#D4AF37")
                } else {
                    Color.parseColor("#0F4C5C")
                }
            )

            val isRequestable = snapshot.state == PhaseProgressionState.READY_FOR_REQUEST || 
                               snapshot.state == PhaseProgressionState.AVAILABLE || 
                               snapshot.state == PhaseProgressionState.REJECTED

            tvStatusMessage.text = snapshot.statusMessage
            tvStatusMessage.setTextColor(
                if (snapshot.state == PhaseProgressionState.REQUEST_PENDING) {
                    Color.parseColor("#D4AF37")
                } else {
                    Color.parseColor("#0F4C5C")
                }
            )

            btnAction.text = snapshot.actionLabel
            btnAction.isEnabled = snapshot.isActionEnabled

            if (booking?.status == Booking.STATUS_PENDING && booking.expiresAt > System.currentTimeMillis()) {
                startPendingCountdown(holder, booking)
            }

            btnAction.setOnClickListener {
                if (isRequestable) {
                    onRequestSeat(phase)
                } else {
                    onPhaseClick(snapshot)
                }
            }
            
            root.setOnClickListener {
                if (snapshot.canEnterClassroom) {
                    onPhaseClick(snapshot)
                }
            }
        }
    }

    override fun getItemCount() = phaseSnapshots.size

    private fun resolveBadgeColor(state: PhaseProgressionState): Int {
        return when (state) {
            PhaseProgressionState.LOCKED -> Color.parseColor("#757575")
            PhaseProgressionState.AVAILABLE -> Color.parseColor("#0F4C5C")
            PhaseProgressionState.READY_FOR_REQUEST -> Color.parseColor("#0F4C5C")
            PhaseProgressionState.IN_PROGRESS -> Color.parseColor("#2E7D32")
            PhaseProgressionState.COMPLETED -> Color.parseColor("#2E7D32")
            PhaseProgressionState.REQUEST_PENDING -> Color.parseColor("#D4AF37")
            PhaseProgressionState.APPROVED -> Color.parseColor("#2E7D32")
            PhaseProgressionState.REJECTED -> Color.parseColor("#B00020")
        }
    }

    private fun startPendingCountdown(holder: ViewHolder, booking: Booking) {
        holder.binding.apply {
            val remainingMillis = booking.expiresAt - System.currentTimeMillis()
            if (remainingMillis <= 0L) {
                tvStatusMessage.text = "Pending request expired. Submit again when ready."
                tvPhaseStateBadge.text = "EXPIRED"
                tvPhaseStateBadge.background.setTint(Color.parseColor("#B00020"))
                return
            }

            holder.timer = object : CountDownTimer(remainingMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val minutes = (millisUntilFinished / 1000) / 60
                    val seconds = (millisUntilFinished / 1000) % 60
                    tvStatusMessage.text = String.format(
                        Locale.getDefault(),
                        "Awaiting teacher pickup. Seat hold expires in %02d:%02d",
                        minutes,
                        seconds
                    )
                }

                override fun onFinish() {
                    tvStatusMessage.text = "Pending request expired. Submit again when ready."
                    tvPhaseStateBadge.text = "EXPIRED"
                    tvPhaseStateBadge.background.setTint(Color.parseColor("#B00020"))
                }
            }.start()
        }
    }
}
