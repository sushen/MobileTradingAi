package com.tradingai.app.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tradingai.app.firebase.FirebaseManager
import com.tradingai.app.models.User

class UserRepository {
    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    fun fetchCurrentUser() {
        val uid = FirebaseManager.auth.currentUser?.uid ?: return
        FirebaseManager.db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                _userData.value = user
            }
            .addOnFailureListener {
                _userData.value = null
            }
    }
}
