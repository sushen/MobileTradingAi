package com.tradingai.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tradingai.app.databinding.ItemInvestmentPlanBinding
import com.tradingai.app.models.InvestmentPlan

class InvestmentPlanAdapter(private val plans: List<InvestmentPlan>) :
    RecyclerView.Adapter<InvestmentPlanAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemInvestmentPlanBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInvestmentPlanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plan = plans[position]
        holder.binding.apply {
            tvPlanName.text = plan.name
            tvMinCapital.text = "Min Capital: ${plan.minCapital}"
            tvRoi.text = "ROI: ${plan.roi}"
            tvRiskLevel.text = "Risk: ${plan.risk}"
        }
    }

    override fun getItemCount() = plans.size
}
