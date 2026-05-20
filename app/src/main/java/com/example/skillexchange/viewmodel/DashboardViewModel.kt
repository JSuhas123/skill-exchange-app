package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.Swap
import com.example.skillexchange.data.model.User
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.data.repository.SwapRepository
import com.example.skillexchange.data.repository.UserRepository
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val swapRepository: SwapRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<Resource<User>>(Resource.Loading())
    val currentUser: StateFlow<Resource<User>> = _userState.asStateFlow()

    private val _recentActivity = MutableStateFlow<Resource<List<String>>>(Resource.Loading())
    val recentActivity: StateFlow<Resource<List<String>>> = _recentActivity.asStateFlow()

    init {
        // Only refresh if user is available to prevent crashes during initialization
        try {
            if (authRepository.currentUser != null) {
                refreshDashboard()
            } else {
                _userState.value = Resource.Error("Waiting for authentication...")
            }
        } catch (e: Exception) {
            _userState.value = Resource.Error("Failed to initialize: ${e.message}")
        }
    }

    fun refreshDashboard() {
        val userId = authRepository.currentUser?.uid
        if (userId == null) {
            _userState.value = Resource.Error("User not authenticated")
            return
        }
        
        viewModelScope.launch {
            _userState.value = Resource.Loading()
            userRepository.getUser(userId)
                .onSuccess { user ->
                    if (user != null) {
                        _userState.value = Resource.Success(user)
                    } else {
                        _userState.value = Resource.Error("User profile not found")
                    }
                }
                .onFailure { e ->
                    _userState.value = Resource.Error(e.message ?: "Failed to load profile")
                }
        }

        viewModelScope.launch {
            _recentActivity.value = Resource.Loading()
            swapRepository.getSwapsForUser(userId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val activities = resource.data?.map { swap ->
                            "Swap ${swap.status}: ${swap.skillA} for ${swap.skillB}"
                        } ?: emptyList()
                        _recentActivity.value = Resource.Success(activities)
                    }
                    is Resource.Error -> {
                        _recentActivity.value = Resource.Error(resource.message ?: "Failed to load activity")
                    }
                    else -> {}
                }
            }
        }
    }
}
