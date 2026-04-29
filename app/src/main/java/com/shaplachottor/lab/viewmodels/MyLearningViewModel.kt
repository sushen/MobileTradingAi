package com.shaplachottor.lab.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shaplachottor.lab.data.PhaseCatalog
import com.shaplachottor.lab.models.Phase
import com.shaplachottor.lab.models.User

class MyLearningViewModel : ViewModel() {

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _currentPhase = MutableLiveData<Phase?>()
    val currentPhase: LiveData<Phase?> = _currentPhase

    private val _completedPhasesList = MutableLiveData<List<Phase>>()
    val completedPhasesList: LiveData<List<Phase>> = _completedPhasesList

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).addSnapshotListener { snapshot, _ ->
            val user = snapshot?.toObject(User::class.java)
            _userData.value = user
            
            if (user != null) {
                if (user.unlockedPhases.isNotEmpty()) {
                    val lastUnlockedPhaseId = user.unlockedPhases.last()
                    fetchPhaseDetails(lastUnlockedPhaseId)
                } else {
                    _currentPhase.value = null
                }

                if (user.completedPhases.isNotEmpty()) {
                    fetchCompletedPhasesDetails(user.completedPhases)
                } else {
                    _completedPhasesList.value = emptyList()
                }
            }
        }
    }

    private fun fetchPhaseDetails(phaseId: String) {
        db.collection("phases").document(phaseId).get().addOnSuccessListener { snapshot ->
            _currentPhase.value = snapshot.toObject(Phase::class.java) ?: PhaseCatalog.findById(phaseId)
        }.addOnFailureListener {
            _currentPhase.value = PhaseCatalog.findById(phaseId)
        }
    }

    private fun fetchCompletedPhasesDetails(phaseIds: List<String>) {
        db.collection("phases").whereIn("phaseId", phaseIds).get()
            .addOnSuccessListener { snapshot ->
                val firestorePhases = snapshot.toObjects(Phase::class.java)
                _completedPhasesList.value = if (firestorePhases.isNotEmpty()) {
                    firestorePhases.sortedBy { it.order }
                } else {
                    phaseIds.mapNotNull(PhaseCatalog::findById).sortedBy { it.order }
                }
            }
            .addOnFailureListener {
                _completedPhasesList.value = phaseIds.mapNotNull(PhaseCatalog::findById).sortedBy { it.order }
            }
    }
}
