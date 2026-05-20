package com.example.skillexchange.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.utils.ErrorHandler
import com.example.skillexchange.utils.InputValidator
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private data class PendingVerification(
        val verificationId: String? = null,
        val name: String = "",
        val phone: String = ""
    )

    private companion object {
        const val OTP_LENGTH = 6
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepository.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(authRepository.currentUser != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isCodeSent = MutableStateFlow(false)
    val isCodeSent: StateFlow<Boolean> = _isCodeSent.asStateFlow()
    private val pendingVerification = MutableStateFlow(PendingVerification())

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
                            _isAuthenticated.value = user != null
                            if (user != null) {
                                Timber.d("Session recovered")
                            } else {
                                Timber.d("No existing session")
                            }
                        }
                        .onFailure { e ->
                            _isAuthenticated.value = false
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

    fun sendVerificationCode(name: String, phoneNumber: String, activity: Activity) {
        if (_isLoading.value) return
        if (!InputValidator.isValidName(name)) {
            _error.value = "Name must be 2-100 characters"
            return
        }
        if (!InputValidator.isValidPhoneNumber(phoneNumber)) {
            _error.value = "Enter a valid phone number with country code"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            pendingVerification.value = PendingVerification(
                verificationId = null,
                name = name.trim(),
                phone = phoneNumber.trim()
            )

            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    viewModelScope.launch {
                        val pending = pendingVerification.value
                        authRepository.signInWithPhoneCredential(
                            credential = credential,
                            userName = pending.name,
                            phoneNumber = pending.phone
                        ).onSuccess { user ->
                            _currentUser.value = user
                            _isAuthenticated.value = user != null
                            _isCodeSent.value = false
                            _error.value = null
                        }.onFailure { e ->
                            _error.value = ErrorHandler.getErrorMessage(e)
                            _isAuthenticated.value = false
                        }
                        _isLoading.value = false
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    _error.value = ErrorHandler.getErrorMessage(e)
                    _isAuthenticated.value = false
                    _isLoading.value = false
                }

                override fun onCodeSent(
                    newVerificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    pendingVerification.update { it.copy(verificationId = newVerificationId) }
                    _isCodeSent.value = true
                    _isLoading.value = false
                }
            }

            authRepository.startPhoneNumberVerification(
                activity = activity,
                phoneNumber = pendingVerification.value.phone,
                callbacks = callbacks
            )
        }
    }

    fun verifyCode(code: String) {
        if (_isLoading.value) return
        val pending = pendingVerification.value
        val currentVerificationId = pending.verificationId
        if (currentVerificationId.isNullOrBlank()) {
            _error.value = "Please request OTP first"
            return
        }
        if (!code.trim().matches(Regex("^\\d{$OTP_LENGTH}$"))) {
            _error.value = "Enter a valid $OTP_LENGTH-digit OTP"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val credential = PhoneAuthProvider.getCredential(currentVerificationId, code.trim())
            authRepository.signInWithPhoneCredential(
                credential = credential,
                userName = pending.name,
                phoneNumber = pending.phone
            ).onSuccess { user ->
                _currentUser.value = user
                _isAuthenticated.value = user != null
                _isCodeSent.value = false
                _error.value = null
            }.onFailure { e ->
                _error.value = ErrorHandler.getErrorMessage(e)
                _isAuthenticated.value = false
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
                    _isCodeSent.value = false
                    pendingVerification.value = PendingVerification()
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
