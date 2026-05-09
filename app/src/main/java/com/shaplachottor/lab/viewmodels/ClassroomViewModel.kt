package com.shaplachottor.lab.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shaplachottor.lab.models.Lesson
import com.shaplachottor.lab.models.LearningJourneyProgress
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.repository.UserRepository
import kotlinx.coroutines.launch

class ClassroomViewModel(
    private val repository: PhaseRepository,
    private val userRepository: UserRepository = UserRepository(),
    private val networkMonitor: com.shaplachottor.lab.util.NetworkMonitor = com.shaplachottor.lab.data.AppGraph.networkMonitor()
) : ViewModel() {
    companion object {
        private const val TAG = "ClassroomViewModel"
    }

    private val _isOnline = MutableLiveData<Boolean>(true)
    val isOnline: LiveData<Boolean> = _isOnline

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOnline.value = online
                if (online && currentPhaseId != null) {
                    android.util.Log.d(TAG, "Network restored. Refreshing lessons for phaseId=$currentPhaseId")
                    refreshLessons(currentPhaseId!!)
                    val refreshedUser = repository.reconcileProgressState() ?: userRepository.getCurrentUserOrNull()
                    _user.value = refreshedUser
                    refreshLearningJourneyProgress(refreshedUser)
                }
            }
        }
    }

    private val _lessons = MutableLiveData<List<Lesson>>()
    val lessons: LiveData<List<Lesson>> = _lessons

    private val _phase = MutableLiveData<Phase?>()
    val phase: LiveData<Phase?> = _phase

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _learningJourneyProgress = MutableLiveData<LearningJourneyProgress?>()
    val learningJourneyProgress: LiveData<LearningJourneyProgress?> = _learningJourneyProgress

    private val _accessDenied = MutableLiveData<Boolean>()
    val accessDenied: LiveData<Boolean> = _accessDenied

    private val _selectedLesson = MutableLiveData<Lesson?>()
    val selectedLesson: LiveData<Lesson?> = _selectedLesson

    private var currentPhaseId: String? = null
    private var selectedLessonPhaseId: String? = null

    fun loadClassroom(phaseId: String) {
        viewModelScope.launch {
            android.util.Log.d(TAG, "Loading classroom for phaseId=$phaseId")
            currentPhaseId = phaseId
            _accessDenied.value = false

            val reconciledUser = repository.reconcileProgressState()
            if (!repository.canAccessPhase(phaseId)) {
                android.util.Log.w(TAG, "Access denied for phaseId=$phaseId")
                _accessDenied.value = true
                return@launch
            }

            _phase.value = repository.getPhaseById(phaseId)
            refreshLessons(phaseId)
            val resolvedUser = reconciledUser ?: userRepository.getCurrentUserOrNull()
            _user.value = resolvedUser
            refreshLearningJourneyProgress(resolvedUser)
            android.util.Log.d(
                TAG,
                "Classroom loaded: phaseId=$phaseId, lessons=${_lessons.value?.map { "${it.id}:${it.isCompleted}" }}, phaseProgress=${_user.value?.phaseProgress?.get(phaseId)}"
            )
        }
    }

    private suspend fun refreshLessons(phaseId: String) {
        val currentLessons = repository.getLessonsForPhase(phaseId)
        _lessons.value = currentLessons

        android.util.Log.d(
            TAG,
            "Lessons refreshed: phaseId=$phaseId, renderedStates=${currentLessons.mapIndexed { index, lesson -> "${lesson.id}@index=$index/order=${lesson.order}/completed=${lesson.isCompleted}" }}"
        )

        val currentSelectedId = _selectedLesson.value?.id
        if (selectedLessonPhaseId == phaseId && currentSelectedId != null) {
            _selectedLesson.value = currentLessons.find { it.id == currentSelectedId }
        }
    }

    fun selectLesson(phaseId: String, lessonId: String) {
        viewModelScope.launch {
            selectedLessonPhaseId = phaseId
            android.util.Log.d(TAG, "Selecting lesson: phaseId=$phaseId, lessonId=$lessonId")

            if (currentPhaseId != phaseId || _lessons.value == null) {
                currentPhaseId = phaseId
                _accessDenied.value = false
                val reconciledUser = repository.reconcileProgressState()
                if (!repository.canAccessPhase(phaseId)) {
                    android.util.Log.w(TAG, "Access denied while selecting lesson: phaseId=$phaseId, lessonId=$lessonId")
                    _accessDenied.value = true
                    return@launch
                }

                _phase.value = repository.getPhaseById(phaseId)
                refreshLessons(phaseId)
                val resolvedUser = reconciledUser ?: userRepository.getCurrentUserOrNull()
                _user.value = resolvedUser
                refreshLearningJourneyProgress(resolvedUser)
            }

            _selectedLesson.value = _lessons.value?.find { it.id == lessonId }
            android.util.Log.d(
                TAG,
                "Selected lesson resolved: requestedLessonId=$lessonId, selectedLessonId=${_selectedLesson.value?.id}, selectedLessonIndex=${_lessons.value?.indexOfFirst { it.id == lessonId }}"
            )
        }
    }

    private val _operationStatus = MutableLiveData<OperationResult?>()
    val operationStatus: LiveData<OperationResult?> = _operationStatus

    sealed class OperationResult {
        object Success : OperationResult()
        data class Error(val message: String) : OperationResult()
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }

    fun completeLesson(phaseId: String, lessonId: String) {
        toggleLessonComplete(phaseId, lessonId, true)
    }

    fun toggleLessonComplete(phaseId: String, lessonId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            android.util.Log.d(
                TAG,
                "Toggling lesson completion: phaseId=$phaseId, lessonId=$lessonId, isCompleted=$isCompleted, currentLessonStates=${_lessons.value?.map { "${it.id}:${it.isCompleted}" }}"
            )
            val success = repository.updateLessonProgress(phaseId, lessonId, isCompleted)
            if (success) {
                refreshLessons(phaseId)
                val resolvedUser = repository.reconcileProgressState() ?: userRepository.getCurrentUserOrNull()
                _user.value = resolvedUser
                refreshLearningJourneyProgress(resolvedUser)
                _operationStatus.value = OperationResult.Success
            } else {
                _operationStatus.value = OperationResult.Error("Failed to update progress. Please check your connection or permissions.")
            }
        }
    }

    private suspend fun refreshLearningJourneyProgress(user: User?) {
        val learningJourneyProgress = repository.getLearningJourneyProgress(user)
        _learningJourneyProgress.value = learningJourneyProgress

        android.util.Log.d(
            TAG,
            "Learning journey refreshed: activePhase=${learningJourneyProgress?.activePhaseId}, overallPercent=${learningJourneyProgress?.overallLearningProgress?.percent}, unlockedPhases=${learningJourneyProgress?.unlockedPhaseIds}, completedPhases=${learningJourneyProgress?.completedPhaseIds}"
        )
    }
}
