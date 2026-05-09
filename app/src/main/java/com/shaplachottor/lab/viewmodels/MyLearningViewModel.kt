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

class MyLearningViewModel(
    private val phaseRepository: PhaseRepository = PhaseRepository()
) : ViewModel() {

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _currentPhase = MutableLiveData<Phase?>()
    val currentPhase: LiveData<Phase?> = _currentPhase

    private val _learningJourneyProgress = MutableLiveData<LearningJourneyProgress?>()
    val learningJourneyProgress: LiveData<LearningJourneyProgress?> = _learningJourneyProgress

    private val _completedPhasesList = MutableLiveData<List<Phase>>()
    val completedPhasesList: LiveData<List<Phase>> = _completedPhasesList

    fun loadUserData() {
        val userId = AppGraph.authSessionProvider().currentUser()?.uid ?: return
        
        viewModelScope.launch {
            try {
                // Use stream for real-time updates
                AppGraph.appStore().getUserStream(userId).collectLatest { user ->
                    _userData.value = user

                    if (user != null) {
                        val learningJourneyProgress = phaseRepository.getLearningJourneyProgress(user)
                        _learningJourneyProgress.value = learningJourneyProgress
                        _currentPhase.value = learningJourneyProgress?.currentPhaseProgress?.phase
                        android.util.Log.d(
                            "MyLearningViewModel",
                            "My Learning progress loaded: activePhase=${learningJourneyProgress?.activePhaseId}, completedPhases=${learningJourneyProgress?.completedPhaseIds}, unlockedPhases=${learningJourneyProgress?.unlockedPhaseIds}, overallPercent=${learningJourneyProgress?.overallLearningProgress?.percent}"
                        )

                        val phases = phaseRepository.getPhases()
                        val completedPhaseIds = learningJourneyProgress?.completedPhaseIds.orEmpty()
                        _completedPhasesList.value = phases.filter { completedPhaseIds.contains(it.phaseId) }
                    } else {
                        _learningJourneyProgress.value = null
                        _currentPhase.value = null
                        _completedPhasesList.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                // This can happen during sign out when permissions are revoked
                android.util.Log.d("MyLearningViewModel", "User stream closed: ${e.message}")
            }
        }
    }
}
