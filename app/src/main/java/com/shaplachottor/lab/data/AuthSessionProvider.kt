package com.shaplachottor.lab.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

interface AuthSessionProvider {
    fun currentUser(): FirebaseUser?
    fun signOut()
}

class FirebaseAuthSessionProvider : AuthSessionProvider {
    private val auth = FirebaseAuth.getInstance()
    override fun currentUser(): FirebaseUser? = auth.currentUser
    override fun signOut() = auth.signOut()
}
