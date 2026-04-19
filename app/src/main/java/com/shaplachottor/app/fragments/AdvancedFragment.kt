package com.shaplachottor.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.shaplachottor.app.R
import com.shaplachottor.app.databinding.FragmentAdvancedBinding

class AdvancedFragment : Fragment() {
    private var _binding: FragmentAdvancedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdvancedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
