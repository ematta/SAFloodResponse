package edu.utap.auth

import com.google.firebase.auth.FirebaseUser

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
} 