package com.shaplachottor.app.data

object AppGraph {
    private val _authSessionProvider: AuthSessionProvider by lazy { FirebaseAuthSessionProvider() }
    private val _appStore: AppStore by lazy { FirestoreAppStore() }

    fun authSessionProvider(): AuthSessionProvider = _authSessionProvider
    fun appStore(): AppStore = _appStore
}
