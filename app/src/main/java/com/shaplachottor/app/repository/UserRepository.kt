package com.shaplachottor.app.repository

import com.shaplachottor.app.data.AppGraph
import com.shaplachottor.app.data.AppStore
import com.shaplachottor.app.data.AuthSessionProvider
import com.shaplachottor.app.models.User

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
