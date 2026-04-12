package com.tradingai.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tradingai.app.adapters.InvestmentPlanAdapter
import com.tradingai.app.databinding.FragmentInvestBinding
import com.tradingai.app.models.InvestmentPlan

class InvestFragment : Fragment() {

    private var _binding: FragmentInvestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInvestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val plans = listOf(
            InvestmentPlan("Starter Plan", "$500", "5-10%", "Low"),
            InvestmentPlan("Professional Plan", "$5,000", "10-20%", "Medium"),
            InvestmentPlan("Institutional Plan", "$50,000", "20-35%", "High")
        )
        binding.rvInvestmentPlans.layoutManager = LinearLayoutManager(context)
        binding.rvInvestmentPlans.adapter = InvestmentPlanAdapter(plans)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
