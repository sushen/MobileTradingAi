package com.shaplachottor.lab.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.repository.UserRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyLearningViewModel(
    private val phaseRepository: PhaseRepository = PhaseRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _currentPhase = MutableLiveData<Phase?>()
    val currentPhase: LiveData<Phase?> = _currentPhase

    private val _completedPhasesList = MutableLiveData<List<Phase>>()
    val completedPhasesList: LiveData<List<Phase>> = _completedPhasesList

    fun loadUserData() {
        val userId = AppGraph.authSessionProvider().currentUser()?.uid ?: return
        
        viewModelScope.launch {
            // Use stream for real-time updates
            AppGraph.appStore().getUserStream(userId).collectLatest { user ->
                _userData.value = user
                
                if (user != null) {
                    // Current phase is usually the last unlocked one that isn't completed
                    val lastUnlockedPhaseId = user.unlockedPhases.lastOrNull()
                    if (lastUnlockedPhaseId != null) {
                        _currentPhase.value = phaseRepository.getPhaseById(lastUnlockedPhaseId)
                    } else {
                        _currentPhase.value = null
                    }

                    if (user.completedPhases.isNotEmpty()) {
                        val phases = phaseRepository.getPhases()
                        _completedPhasesList.value = phases.filter { user.completedPhases.contains(it.phaseId) }
                    } else {
                        _completedPhasesList.value = emptyList()
                    }
                }
            }
        }
    }
}
