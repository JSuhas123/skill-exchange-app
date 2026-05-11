package com.example.skillexchange.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.User
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.data.repository.ProfileRepository
import com.example.skillexchange.data.repository.UserRepository
import com.example.skillexchange.utils.InputValidator
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val uiState: StateFlow<Resource<User>> = _uiState.asStateFlow()

    private val _saveStatus = MutableStateFlow<Resource<Unit>?>(null)
    val saveStatus: StateFlow<Resource<Unit>?> = _saveStatus.asStateFlow()
    
    private val _uploadStatus = MutableStateFlow<Resource<String>?>(null)
    val uploadStatus: StateFlow<Resource<String>?> = _uploadStatus.asStateFlow()
    
    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

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
                            Timber.d("User profile loaded")
                        } else {
                            val newUser = User(
                                id = userId,
                                email = authRepository.currentUser?.email ?: "",
                                name = "User"
                            )
                            _uiState.value = Resource.Success(newUser)
                            Timber.d("New user profile created")
                        }
                    }
                    .onFailure { e ->
                        _uiState.value = Resource.Error(e.message ?: "Failed to load profile", e)
                        Timber.e(e, "Failed to load user profile")
                    }
            }
        } else {
            _uiState.value = Resource.Error("Not authenticated")
        }
    }

    fun saveProfile(name: String, skillsOffered: List<String>, skillsNeeded: List<String>) {
        // Validate inputs
        val errors = mutableMapOf<String, String>()
        
        if (!InputValidator.isValidName(name)) {
            errors["name"] = "Name must be 2-100 characters"
        }
        if (!InputValidator.isValidSkillList(skillsOffered)) {
            errors["skillsOffered"] = "Add at least one valid skill"
        }
        if (skillsNeeded.isNotEmpty() && !InputValidator.isValidSkillList(skillsNeeded)) {
            errors["skillsNeeded"] = "Invalid skills in needed list"
        }
        
        if (errors.isNotEmpty()) {
            _validationErrors.value = errors
            return
        }
        
        _validationErrors.value = emptyMap()
        
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _saveStatus.value = Resource.Loading()
            val currentUser = (uiState.value as? Resource.Success)?.data
            val user = User(
                id = userId,
                name = name,
                email = authRepository.currentUser?.email ?: "",
                skillsOffered = skillsOffered,
                skillsNeeded = skillsNeeded,
                trustScore = currentUser?.trustScore ?: 0,
                skillPoints = currentUser?.skillPoints ?: 10
            )
            
            userRepository.saveUser(user)
                .onSuccess {
                    _uiState.value = Resource.Success(user)
                    _saveStatus.value = Resource.Success(Unit)
                    Timber.d("Profile saved successfully")
                }
                .onFailure { e ->
                    _saveStatus.value = Resource.Error(e.message ?: "Failed to save profile", e)
                    Timber.e(e, "Failed to save profile")
                }
        }
    }
    
    fun uploadProfileImage(userId: String, imageUri: Uri) {
        viewModelScope.launch {
            _uploadStatus.value = Resource.Loading()
            profileRepository.updateProfilePicture(userId, imageUri)
                .onSuccess {
                    _uploadStatus.value = Resource.Success("Profile image updated")
                    // Refresh profile to get new image URL
                    fetchUserProfile()
                    Timber.d("Profile image uploaded successfully")
                }
                .onFailure { e ->
                    _uploadStatus.value = Resource.Error(e.message ?: "Failed to upload image", e)
                    Timber.e(e, "Failed to upload profile image")
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            Timber.d("User signed out")
        }
    }
    
    fun resetSaveStatus() {
        _saveStatus.value = null
    }
    
    fun resetUploadStatus() {
        _uploadStatus.value = null
    }
    
    fun clearValidationErrors() {
        _validationErrors.value = emptyMap()
    }
}
