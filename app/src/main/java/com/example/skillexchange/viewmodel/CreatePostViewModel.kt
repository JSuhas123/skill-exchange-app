package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.Post
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.data.repository.PostRepository
import com.example.skillexchange.data.repository.UserRepository
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Resource<Unit>?>(null)
    val uiState: StateFlow<Resource<Unit>?> = _uiState.asStateFlow()

    fun createPost(skillRequired: String, skillOffered: String, description: String) {
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
                        }
                        .onFailure { e ->
                            _uiState.value = Resource.Error(e.message ?: "Failed to create post")
                        }
                }
                .onFailure { e ->
                    _uiState.value = Resource.Error(e.message ?: "Failed to fetch user info")
                }
        }
    }

    fun resetState() {
        _uiState.value = null
    }
}
