package com.shaplachottor.lab.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.shaplachottor.lab.R
import com.shaplachottor.lab.databinding.FragmentAdvancedBinding

import androidx.lifecycle.lifecycleScope
import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.models.AdvancedFeatures
import kotlinx.coroutines.launch

class AdvancedFragment : Fragment() {
    private var _binding: FragmentAdvancedBinding? = null
    private val binding get() = _binding!!
    private val appStore = AppGraph.appStore()
    private val authProvider = AppGraph.authSessionProvider()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdvancedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeGating()
        setupListeners()
    }

    private fun observeGating() {
        val userId = authProvider.currentUser()?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            appStore.getUserStream(userId).collect { user ->
                user?.unlockedFeatures?.let { features ->
                    updateGatingUI(features)
                }
            }
        }
    }

    private fun updateGatingUI(features: AdvancedFeatures) {
        // Install Bot Gate
        binding.btnInstallBot.isEnabled = features.tradingBot
        binding.btnInstallBot.alpha = if (features.tradingBot) 1.0f else 0.5f
        binding.ivLockInstall.visibility = if (features.tradingBot) View.GONE else View.VISIBLE

        // Invest Gate
        binding.btnInvest.isEnabled = features.investment
        binding.btnInvest.alpha = if (features.investment) 1.0f else 0.5f
        binding.ivLockInvest.visibility = if (features.investment) View.GONE else View.VISIBLE

        // Affiliate Gate
        binding.btnAffiliate.isEnabled = features.affiliate
        binding.btnAffiliate.alpha = if (features.affiliate) 1.0f else 0.5f
        binding.ivLockAffiliate.visibility = if (features.affiliate) View.GONE else View.VISIBLE
    }

    private fun setupListeners() {
        binding.btnInstallBot.setOnClickListener {
            findNavController().navigate(R.id.installFragment)
        }
        binding.btnInvest.setOnClickListener {
            findNavController().navigate(R.id.investFragment)
        }
        binding.btnAffiliate.setOnClickListener {
            findNavController().navigate(R.id.affiliateFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
