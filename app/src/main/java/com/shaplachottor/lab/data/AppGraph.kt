package com.shaplachottor.lab.data

object AppGraph {
    private val _authSessionProvider: AuthSessionProvider by lazy { FirebaseAuthSessionProvider() }
    private val _appStore: AppStore by lazy { FirestoreAppStore() }
    private var _networkMonitor: com.shaplachottor.lab.util.NetworkMonitor? = null

    fun init(context: android.content.Context) {
        _networkMonitor = com.shaplachottor.lab.util.NetworkMonitor(context.applicationContext)
    }

    fun authSessionProvider(): AuthSessionProvider = _authSessionProvider
    fun appStore(): AppStore = _appStore
    fun networkMonitor(): com.shaplachottor.lab.util.NetworkMonitor = 
        _networkMonitor ?: throw IllegalStateException("AppGraph not initialized with context")
}
