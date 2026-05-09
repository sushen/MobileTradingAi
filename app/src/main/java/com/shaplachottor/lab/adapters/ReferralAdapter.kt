package com.shaplachottor.lab.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shaplachottor.lab.R
import com.shaplachottor.lab.databinding.ItemReferralBinding
import com.shaplachottor.lab.models.ReferralEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReferralAdapter : ListAdapter<ReferralEvent, ReferralAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReferralBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemReferralBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: ReferralEvent) {
            binding.tvUserId.text = if (event.referredUserName.isNotEmpty()) event.referredUserName else "Researcher ${event.referredUserId.takeLast(4)}"
            
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvTimestamp.text = "Joined ${sdf.format(Date(event.timestamp))}"
            
            binding.tvStatusBadge.text = event.status
            val context = binding.root.context
            
            if (event.status == "converted") {
                binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge) // Assuming this exists or using a color
                binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    context.getColor(R.color.accent)
                )
                binding.ivStatus.setImageResource(R.drawable.ic_profile_colorful) // Or a checkmark if available
            } else {
                binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    context.getColor(R.color.text_secondary)
                )
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ReferralEvent>() {
        override fun areItemsTheSame(oldItem: ReferralEvent, newItem: ReferralEvent) = 
            oldItem.referredUserId == newItem.referredUserId
        override fun areContentsTheSame(oldItem: ReferralEvent, newItem: ReferralEvent) = oldItem == newItem
    }
}
