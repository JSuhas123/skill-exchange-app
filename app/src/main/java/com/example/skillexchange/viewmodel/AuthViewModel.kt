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

    init {
        initializeAuth()
    }
    
    private var verificationId: String? = null
    private var pendingName: String = ""
    private var pendingPhone: String = ""

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
                            Timber.d("Session recovered")
                        }
                        .onFailure { e ->
                            _isAuthenticated.value = false
                            Timber.d("No existing session to recover")
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

            pendingName = name.trim()
            pendingPhone = phoneNumber.trim()

            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    viewModelScope.launch {
                        authRepository.signInWithPhoneCredential(
                            credential = credential,
                            userName = pendingName,
                            phoneNumber = pendingPhone
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

                override fun onVerificationFailed(e: Exception) {
                    _error.value = ErrorHandler.getErrorMessage(e)
                    _isAuthenticated.value = false
                    _isLoading.value = false
                }

                override fun onCodeSent(
                    newVerificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = newVerificationId
                    _isCodeSent.value = true
                    _isLoading.value = false
                }
            }

            authRepository.startPhoneNumberVerification(
                activity = activity,
                phoneNumber = pendingPhone,
                callbacks = callbacks
            )
        }
    }

    fun verifyCode(code: String) {
        if (_isLoading.value) return
        val currentVerificationId = verificationId
        if (currentVerificationId.isNullOrBlank()) {
            _error.value = "Please request OTP first"
            return
        }
        if (code.length != OTP_LENGTH) {
            _error.value = "Enter a valid 6-digit OTP"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val credential = PhoneAuthProvider.getCredential(currentVerificationId, code.trim())
            authRepository.signInWithPhoneCredential(
                credential = credential,
                userName = pendingName,
                phoneNumber = pendingPhone
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
                    verificationId = null
                    pendingName = ""
                    pendingPhone = ""
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
