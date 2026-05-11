package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.User
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.data.repository.UserRepository
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val uiState: StateFlow<Resource<User>> = _uiState.asStateFlow()

    private val _saveStatus = MutableStateFlow<Resource<Unit>?>(null)
    val saveStatus: StateFlow<Resource<Unit>?> = _saveStatus.asStateFlow()

    init {
        fetchUserProfile()
    }

    fun fetchUserProfile() {
        val userId = authRepository.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                _uiState.value = Resource.Loading()
                userRepository.getUser(userId)
                    .onSuccess { user ->
                        if (user != null) {
                            _uiState.value = Resource.Success(user)
                        } else {
                            val newUser = User(id = userId, email = authRepository.currentUser?.email ?: "")
                            _uiState.value = Resource.Success(newUser)
                        }
                    }
                    .onFailure { e ->
                        _uiState.value = Resource.Error(e.message ?: "Failed to load profile")
                    }
            }
        } else {
            _uiState.value = Resource.Error("Not authenticated")
        }
    }

    fun saveProfile(name: String, skillsOffered: List<String>, skillsNeeded: List<String>) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _saveStatus.value = Resource.Loading()
            val user = User(
                id = userId,
                name = name,
                email = authRepository.currentUser?.email ?: "",
                skillsOffered = skillsOffered,
                skillsNeeded = skillsNeeded
            )
            userRepository.saveUser(user)
                .onSuccess {
                    _uiState.value = Resource.Success(user)
                    _saveStatus.value = Resource.Success(Unit)
                }
                .onFailure { e ->
                    _saveStatus.value = Resource.Error(e.message ?: "Failed to save profile")
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
    
    fun resetSaveStatus() {
        _saveStatus.value = null
    }
}
