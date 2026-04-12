package com.tradingai.app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.tradingai.app.adapters.CourseAdapter
import com.tradingai.app.databinding.FragmentEducationBinding
import com.tradingai.app.models.Course

class EducationFragment : Fragment() {

    private var _binding: FragmentEducationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEducationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupTabs()
        setupRecyclerView("Beginner")
    }

    private fun setupTabs() {
        binding.tabLayoutEducation.addTab(binding.tabLayoutEducation.newTab().setText("Beginner"))
        binding.tabLayoutEducation.addTab(binding.tabLayoutEducation.newTab().setText("Intermediate"))
        binding.tabLayoutEducation.addTab(binding.tabLayoutEducation.newTab().setText("Advanced"))

        binding.tabLayoutEducation.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                setupRecyclerView(tab?.text.toString())
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView(level: String) {
        val allCourses = listOf(
            Course("1", "Intro to Algo Trading", "Learn the basics of automated trading.", "Beginner", "https://youtube.com", "https://docs.tradingai.app"),
            Course("2", "Python for Finance", "Master Python for data analysis.", "Intermediate", "https://youtube.com", "https://docs.tradingai.app"),
            Course("3", "Advanced Neural Networks", "Apply AI to market predictions.", "Advanced", "https://youtube.com", "https://docs.tradingai.app")
        )
        val filteredCourses = allCourses.filter { it.level == level }
        binding.rvCourses.layoutManager = LinearLayoutManager(context)
        binding.rvCourses.adapter = CourseAdapter(filteredCourses)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
