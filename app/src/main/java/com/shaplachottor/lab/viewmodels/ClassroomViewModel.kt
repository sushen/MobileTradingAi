package com.shaplachottor.lab.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shaplachottor.lab.models.Lesson
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User
import com.shaplachottor.lab.repositories.PhaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ClassroomViewModel(private val repository: PhaseRepository) : ViewModel() {

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
            fetchUser()
        }
    }

    private fun fetchUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { snapshot ->
                _user.value = snapshot.toObject(User::class.java)
            }
    }

    fun toggleLessonComplete(phaseId: String, lessonId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val success = repository.updateLessonProgress(phaseId, lessonId, isCompleted)
            if (success) {
                fetchUser() // Refresh to get new progress %
            }
        }
    }
}
