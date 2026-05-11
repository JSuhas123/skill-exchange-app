package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.Swap
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.data.repository.SwapRepository
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwapViewModel @Inject constructor(
    private val swapRepository: SwapRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUserId = authRepository.currentUser?.uid ?: ""

    private val _swaps = MutableStateFlow<Resource<List<Swap>>>(Resource.Loading())
    val swaps: StateFlow<Resource<List<Swap>>> = _swaps.asStateFlow()

    private val _actionStatus = MutableStateFlow<Resource<Unit>?>(null)
    val actionStatus: StateFlow<Resource<Unit>?> = _actionStatus.asStateFlow()

    init {
        fetchSwaps()
    }

    fun fetchSwaps() {
        if (currentUserId.isEmpty()) {
            _swaps.value = Resource.Error("User not authenticated")
            return
        }
        viewModelScope.launch {
            swapRepository.getSwapsForUser(currentUserId).collect {
                _swaps.value = it
            }
        }
    }

    fun initiateSwap(userB: String, skillA: String, skillB: String) {
        if (currentUserId.isEmpty()) return
        
        val swap = Swap(
            userA = currentUserId,
            userB = userB,
            users = listOf(currentUserId, userB),
            skillA = skillA,
            skillB = skillB,
            status = "pending"
        )
        viewModelScope.launch {
            _actionStatus.value = Resource.Loading()
            swapRepository.createSwap(swap)
                .onSuccess { _actionStatus.value = Resource.Success(Unit) }
                .onFailure { e -> _actionStatus.value = Resource.Error(e.message ?: "Failed to propose swap") }
        }
    }

    fun acceptSwap(swapId: String) {
        viewModelScope.launch {
            _actionStatus.value = Resource.Loading()
            swapRepository.updateSwapStatus(swapId, "accepted")
                .onSuccess { _actionStatus.value = Resource.Success(Unit) }
                .onFailure { e -> _actionStatus.value = Resource.Error(e.message ?: "Failed to accept swap") }
        }
    }

    fun confirmCompletion(swapId: String) {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            _actionStatus.value = Resource.Loading()
            
            // We need to know if current user is A or B to pass correct flag to repository
            val currentSwaps = (swaps.value as? Resource.Success)?.data ?: emptyList()
            val swap = currentSwaps.find { it.id == swapId }
            
            if (swap != null) {
                val isUserA = swap.userA == currentUserId
                swapRepository.confirmSwapAndProgress(swapId, isUserA)
                    .onSuccess { _actionStatus.value = Resource.Success(Unit) }
                    .onFailure { e -> _actionStatus.value = Resource.Error(e.message ?: "Failed to confirm completion") }
            } else {
                _actionStatus.value = Resource.Error("Swap not found")
            }
        }
    }

    fun cancelSwap(swapId: String) {
        viewModelScope.launch {
            _actionStatus.value = Resource.Loading()
            swapRepository.updateSwapStatus(swapId, "cancelled")
                .onSuccess { _actionStatus.value = Resource.Success(Unit) }
                .onFailure { e -> _actionStatus.value = Resource.Error(e.message ?: "Failed to cancel swap") }
        }
    }

    fun resetActionStatus() {
        _actionStatus.value = null
    }
}
