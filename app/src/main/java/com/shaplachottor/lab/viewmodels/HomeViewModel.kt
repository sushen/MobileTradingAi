package com.shaplachottor.lab.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User
import com.shaplachottor.lab.repositories.PhaseRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(
    private val phaseRepository: PhaseRepository = PhaseRepository()
) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _currentPhase = MutableLiveData<Phase?>()
    val currentPhase: LiveData<Phase?> = _currentPhase

    fun loadUserData() {
        val userId = AppGraph.authSessionProvider().currentUser()?.uid ?: return
        
        viewModelScope.launch {
            AppGraph.appStore().getUserStream(userId).collectLatest { userData ->
                _user.value = userData
                
                userData?.unlockedPhases?.lastOrNull()?.let { phaseId ->
                    _currentPhase.value = phaseRepository.getPhaseById(phaseId)
                }
            }
        }
    }
}
