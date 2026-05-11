package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.Post
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.data.repository.PostRepository
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
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Resource<Unit>?>(null)
    val uiState: StateFlow<Resource<Unit>?> = _uiState.asStateFlow()
    
    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    fun createPost(skillRequired: String, skillOffered: String, description: String) {
        // Validate inputs
        val errors = mutableMapOf<String, String>()
        
        if (!InputValidator.isValidSkill(skillRequired)) {
            errors["skillRequired"] = "Skill must be 2-100 characters"
        }
        if (!InputValidator.isValidSkill(skillOffered)) {
            errors["skillOffered"] = "Skill must be 2-100 characters"
        }
        if (!InputValidator.isValidDescription(description)) {
            errors["description"] = "Description must be 5-1000 characters"
        }
        
        if (errors.isNotEmpty()) {
            _validationErrors.value = errors
            return
        }
        
        _validationErrors.value = emptyMap()
        
        val userId = authRepository.currentUser?.uid ?: run {
            _uiState.value = Resource.Error("User not authenticated")
            return
        }

        viewModelScope.launch {
            _uiState.value = Resource.Loading()
            
            userRepository.getUser(userId)
                .onSuccess { user ->
                    val post = Post(
                        userId = userId,
                        userName = user?.name ?: "Anonymous",
                        skillRequired = skillRequired,
                        skillOffered = skillOffered,
                        description = description
                    )
                    
                    postRepository.createPost(post)
                        .onSuccess {
                            _uiState.value = Resource.Success(Unit)
                            Timber.d("Post created successfully")
                        }
                        .onFailure { e ->
                            _uiState.value = Resource.Error(e.message ?: "Failed to create post", e)
                            Timber.e(e, "Failed to create post")
                        }
                }
                .onFailure { e ->
                    _uiState.value = Resource.Error(e.message ?: "Failed to fetch user info", e)
                    Timber.e(e, "Failed to fetch user info")
                }
        }
    }

    fun resetState() {
        _uiState.value = null
        _validationErrors.value = emptyMap()
    }
}
