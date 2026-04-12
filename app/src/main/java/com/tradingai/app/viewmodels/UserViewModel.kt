package com.tradingai.app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.tradingai.app.models.User
import com.tradingai.app.repository.UserRepository

class UserViewModel : ViewModel() {
    private val repository = UserRepository()
    val userData: LiveData<User?> = repository.userData

    fun fetchUser() {
        repository.fetchCurrentUser()
    }
}
