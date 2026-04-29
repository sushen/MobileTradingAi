package com.shaplachottor.lab.repository

import com.shaplachottor.lab.data.AppGraph
import com.shaplachottor.lab.data.AppStore
import com.shaplachottor.lab.data.AuthSessionProvider
import com.shaplachottor.lab.models.User

class UserRepository(
    private val authSessionProvider: AuthSessionProvider = AppGraph.authSessionProvider(),
    private val appStore: AppStore = AppGraph.appStore()
) {
    suspend fun getCurrentUserOrNull(): User? {
        val uid = authSessionProvider.currentUser()?.uid ?: return null
        return appStore.getUser(uid)
    }

    suspend fun saveUser(user: User) {
        appStore.setUser(user)
    }
}
