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
    val userState: StateFlow<Resource<User>> = _userState.asStateFlow()

    private val _completedSwaps = MutableStateFlow<Resource<List<Swap>>>(Resource.Loading())
    val completedSwaps: StateFlow<Resource<List<Swap>>> = _completedSwaps.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        val userId = authRepository.currentUser?.uid ?: run {
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
            swapRepository.getSwapsForUser(userId).collect { resource ->
                if (resource is Resource.Success) {
                    val completed = resource.data?.filter { it.status == "completed" } ?: emptyList()
                    _completedSwaps.value = Resource.Success(completed)
                } else {
                    _completedSwaps.value = resource
                }
            }
        }
    }
}
