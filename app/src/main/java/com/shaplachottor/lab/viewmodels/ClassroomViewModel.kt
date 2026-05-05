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

    fun loadClassroom(phaseId: String) {
        viewModelScope.launch {
            if (!repository.canAccessPhase(phaseId)) {
                _accessDenied.value = true
                return@launch
            }

            _phase.value = repository.getPhaseById(phaseId)
            _lessons.value = repository.getLessonsForPhase(phaseId)
            
            // Fetch user for progress binding
            _user.value = userRepository.getCurrentUserOrNull()
        }
    }

    fun toggleLessonComplete(phaseId: String, lessonId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val success = repository.updateLessonProgress(phaseId, lessonId, isCompleted)
            if (success) {
                // Refresh local state
                _lessons.value = repository.getLessonsForPhase(phaseId)
                _user.value = userRepository.getCurrentUserOrNull()
            }
        }
    }
}
