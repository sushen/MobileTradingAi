package com.shaplachottor.lab.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.models.LearningJourneyProgress
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

    private val _learningJourneyProgress = MutableLiveData<LearningJourneyProgress?>()
    val learningJourneyProgress: LiveData<LearningJourneyProgress?> = _learningJourneyProgress

    fun loadUserData() {
        val userId = AppGraph.authSessionProvider().currentUser()?.uid ?: return
        
        viewModelScope.launch {
            try {
                AppGraph.appStore().getUserStream(userId).collectLatest { userData ->
                    _user.value = userData

                    if (userData != null) {
                        val learningJourneyProgress = phaseRepository.getLearningJourneyProgress(userData)
                        _learningJourneyProgress.value = learningJourneyProgress
                        _currentPhase.value = learningJourneyProgress?.currentPhaseProgress?.phase
                        android.util.Log.d(
                            "HomeViewModel",
                            "Home progress loaded: activePhase=${learningJourneyProgress?.activePhaseId}, completedPhases=${learningJourneyProgress?.completedPhaseIds}, unlockedPhases=${learningJourneyProgress?.unlockedPhaseIds}, overallPercent=${learningJourneyProgress?.overallLearningProgress?.percent}"
                        )
                    } else {
                        _learningJourneyProgress.value = null
                        _currentPhase.value = null
                    }
                }
            } catch (e: Exception) {
                // This can happen during sign out when permissions are revoked
                android.util.Log.d("HomeViewModel", "User stream closed: ${e.message}")
            }
        }
    }
}
