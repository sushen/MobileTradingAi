package com.shaplachottor.lab.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.shaplachottor.lab.databinding.FragmentAffiliateBinding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shaplachottor.lab.adapters.ReferralAdapter
import com.shaplachottor.lab.data.AppGraph
import kotlinx.coroutines.launch

class AffiliateFragment : Fragment() {
    private var _binding: FragmentAffiliateBinding? = null
    private val binding get() = _binding!!
    private val appStore = AppGraph.appStore()
    private val authProvider = AppGraph.authSessionProvider()
    private val referralAdapter = ReferralAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAffiliateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadDashboardData()
        setupListeners()
    }

    private fun setupRecyclerView() {
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = referralAdapter
    }

    private fun loadDashboardData() {
        val userId = authProvider.currentUser()?.uid ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val user = appStore.getUser(userId)
                val stats = appStore.getAffiliateStats(userId)
                val events = appStore.getReferralEvents(userId)

                binding.tvReferralCode.text = user?.referralCode ?: "NOT_SET"
                binding.tvTotalInvites.text = (stats?.get("totalInvites") ?: 0).toString()
                binding.tvTotalConversions.text = (stats?.get("conversions") ?: 0).toString()

                if (events.isEmpty()) {
                    binding.tvEmptyHistory.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                } else {
                    binding.tvEmptyHistory.visibility = View.GONE
                    binding.rvHistory.visibility = View.VISIBLE
                    referralAdapter.submitList(events)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading affiliate data", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupListeners() {
        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvReferralCode.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Referral Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnShare.setOnClickListener {
            val code = binding.tvReferralCode.text.toString()
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Join me on TradingAI! Use my partner code: $code and start your journey.")
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
