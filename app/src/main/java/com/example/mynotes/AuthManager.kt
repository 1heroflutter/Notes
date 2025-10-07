package com.example.mynotes

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AuthManager {
    private val _auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow(isUserLoggedIn())
    val authState: StateFlow<Boolean> = _authState

    fun isUserLoggedIn(): Boolean {
        return _auth.currentUser != null
    }
    fun getUserEmail(): String {
        return _auth.currentUser?.email ?: "Khách"
    }

    fun isEmailPasswordUser(): Boolean {
        val user = _auth.currentUser
        return user?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true
    }

    init {
        _auth.addAuthStateListener {
            _authState.value = isUserLoggedIn()
        }
    }
}
