package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.utils.ErrorHandler
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepository.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(authRepository.currentUser != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        initializeAuth()
    }
    
    private fun initializeAuth() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Check if there's an existing session
                val isValid = authRepository.isSessionValid()
                
                if (isValid) {
                    _currentUser.value = authRepository.currentUser
                    _isAuthenticated.value = true
                    Timber.d("Existing session validated")
                } else {
                    // Try to recover session or sign in anonymously
                    authRepository.recoverSession()
                        .onSuccess { user ->
                            _currentUser.value = user
                            _isAuthenticated.value = true
                            Timber.d("Session recovered")
                        }
                        .onFailure { e ->
                            _error.value = ErrorHandler.getErrorMessage(e)
                            Timber.e(e, "Failed to recover session")
                        }
                }
            } catch (e: Exception) {
                _error.value = ErrorHandler.getErrorMessage(e)
                Timber.e(e, "Auth initialization failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInAnonymously() {
        if (_isLoading.value) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            authRepository.signInAnonymously()
                .onSuccess { user ->
                    _currentUser.value = user
                    _isAuthenticated.value = user != null
                    Timber.d("Anonymous sign in successful")
                }
                .onFailure { e ->
                    _error.value = ErrorHandler.getErrorMessage(e)
                    _isAuthenticated.value = false
                    Timber.e(e, "Anonymous sign in failed")
                    
                    // Retry once if it's a network error
                    if (ErrorHandler.isRetryableError(e)) {
                        retrySignIn()
                    }
                }
            
            _isLoading.value = false
        }
    }
    
    private fun retrySignIn() {
        viewModelScope.launch {
            Timber.d("Retrying anonymous sign in")
            _isLoading.value = true
            
            kotlinx.coroutines.delay(1000) // Wait 1 second before retry
            
            authRepository.signInAnonymously()
                .onSuccess { user ->
                    _currentUser.value = user
                    _isAuthenticated.value = user != null
                    _error.value = null
                    Timber.d("Retry successful")
                }
                .onFailure { e ->
                    _error.value = "Connection failed. Please check your internet and try again."
                    Timber.e(e, "Retry failed")
                }
            
            _isLoading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
                .onSuccess {
                    _currentUser.value = null
                    _isAuthenticated.value = false
                    _error.value = null
                    Timber.d("Sign out successful")
                }
                .onFailure { e ->
                    _error.value = ErrorHandler.getErrorMessage(e)
                    Timber.e(e, "Sign out failed")
                }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
