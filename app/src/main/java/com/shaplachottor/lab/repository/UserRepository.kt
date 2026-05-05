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
        val existingUser = appStore.getUser(user.id)
        if (existingUser != null) {
            val mergedUser = existingUser.copy(
                email = user.email.ifEmpty { existingUser.email },
                name = user.name.ifEmpty { existingUser.name },
                photoUrl = user.photoUrl ?: existingUser.photoUrl
            )
            appStore.setUser(mergedUser)
        } else {
            // New user: Generate referral code and handle attribution
            val referralCode = user.id.takeLast(6).uppercase()
            appStore.setUser(user.copy(referralCode = referralCode))
            
            // Log the referral event if referredBy is present
            if (user.referredBy != null) {
                appStore.logReferralEvent(user.referredBy, user.id)
            }
        }
    }
}
