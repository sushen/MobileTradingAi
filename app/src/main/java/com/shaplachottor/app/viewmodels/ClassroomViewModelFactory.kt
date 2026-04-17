package com.shaplachottor.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shaplachottor.app.repositories.PhaseRepository

class ClassroomViewModelFactory(private val repository: PhaseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClassroomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClassroomViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
