package com.shaplachottor.lab.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.repository.UserRepository

class ClassroomViewModelFactory(
    private val repository: PhaseRepository,
    private val userRepository: UserRepository = UserRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClassroomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClassroomViewModel(repository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
