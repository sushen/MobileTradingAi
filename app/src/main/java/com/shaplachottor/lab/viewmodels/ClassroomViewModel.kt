package com.shaplachottor.lab.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shaplachottor.lab.models.Lesson
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User
import com.shaplachottor.lab.repositories.PhaseRepository
import com.shaplachottor.lab.repository.UserRepository
import kotlinx.coroutines.launch

class ClassroomViewModel(
    private val repository: PhaseRepository,
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _lessons = MutableLiveData<List<Lesson>>()
    val lessons: LiveData<List<Lesson>> = _lessons

    private val _phase = MutableLiveData<Phase?>()
    val phase: LiveData<Phase?> = _phase

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _accessDenied = MutableLiveData<Boolean>()
    val accessDenied: LiveData<Boolean> = _accessDenied

    private val _selectedLesson = MutableLiveData<Lesson?>()
    val selectedLesson: LiveData<Lesson?> = _selectedLesson

    private var currentPhaseId: String? = null

    fun loadClassroom(phaseId: String) {
        if (currentPhaseId == phaseId && _lessons.value != null) return
        currentPhaseId = phaseId
        
        viewModelScope.launch {
            if (!repository.canAccessPhase(phaseId)) {
                _accessDenied.value = true
                return@launch
            }

            _phase.value = repository.getPhaseById(phaseId)
            refreshLessons(phaseId)
            
            // Fetch user for progress binding
            _user.value = userRepository.getCurrentUserOrNull()
        }
    }

    private suspend fun refreshLessons(phaseId: String) {
        val currentLessons = repository.getLessonsForPhase(phaseId)
        _lessons.value = currentLessons
        
        // Update selected lesson if it exists in the list (to refresh isCompleted state)
        val currentSelectedId = _selectedLesson.value?.id
        if (currentSelectedId != null) {
            _selectedLesson.value = currentLessons.find { it.id == currentSelectedId }
        }
    }

    fun selectLesson(phaseId: String, lessonId: String) {
        if (currentPhaseId != phaseId || _lessons.value == null) {
            loadClassroom(phaseId)
        }
        
        viewModelScope.launch {
            // Wait for lessons to be loaded if they are null
            if (_lessons.value == null) {
                val currentLessons = repository.getLessonsForPhase(phaseId)
                _lessons.value = currentLessons
            }
            _selectedLesson.value = _lessons.value?.find { it.id == lessonId }
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
            val success = repository.updateLessonProgress(phaseId, lessonId, isCompleted)
            if (success) {
                // Refresh local state
                refreshLessons(phaseId)
                _user.value = userRepository.getCurrentUserOrNull()
                _operationStatus.value = OperationResult.Success
            } else {
                _operationStatus.value = OperationResult.Error("Failed to update progress. Please check your connection or permissions.")
            }
        }
    }
}
