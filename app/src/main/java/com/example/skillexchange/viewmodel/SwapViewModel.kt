package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.Swap
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.data.repository.SwapRepository
import com.example.skillexchange.utils.InputValidator
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
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
    
    private val _pendingActions = MutableStateFlow<Set<String>>(emptySet())
    val pendingActions: StateFlow<Set<String>> = _pendingActions.asStateFlow()
    
    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

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

    fun initiateSwap(userB: String, skillA: String, skillB: String, hours: Int) {
        // Validate inputs
        val errors = mutableMapOf<String, String>()
        
        if (!InputValidator.isValidSkill(skillA)) {
            errors["skillA"] = "Skill must be 2-100 characters"
        }
        if (!InputValidator.isValidSkill(skillB)) {
            errors["skillB"] = "Skill must be 2-100 characters"
        }
        if (!InputValidator.isValidHours(hours)) {
            errors["hours"] = "Hours must be between 1-168"
        }
        if (userB.isBlank()) {
            errors["userB"] = "Invalid user"
        }
        
        if (errors.isNotEmpty()) {
            _validationErrors.value = errors
            return
        }
        
        _validationErrors.value = emptyMap()
        
        if (currentUserId.isEmpty() || isActionPending(userB)) return
        
        val swap = Swap(
            userA = currentUserId,
            userB = userB,
            users = listOf(currentUserId, userB),
            skillA = skillA,
            skillB = skillB,
            hours = hours,
            status = "pending"
        )
        
        viewModelScope.launch {
            addPendingAction(userB)
            _actionStatus.value = Resource.Loading()
            
            swapRepository.createSwap(swap)
                .onSuccess {
                    _actionStatus.value = Resource.Success(Unit)
                    Timber.d("Swap initiated successfully")
                }
                .onFailure { e ->
                    _actionStatus.value = Resource.Error(e.message ?: "Failed to propose swap", e)
                    Timber.e(e, "Failed to initiate swap")
                }
                .also { removePendingAction(userB) }
        }
    }

    fun acceptSwap(swapId: String) {
        if (isActionPending(swapId)) return
        
        viewModelScope.launch {
            addPendingAction(swapId)
            _actionStatus.value = Resource.Loading()
            
            swapRepository.acceptSwap(swapId)
                .onSuccess {
                    _actionStatus.value = Resource.Success(Unit)
                    Timber.d("Swap accepted successfully")
                }
                .onFailure { e ->
                    _actionStatus.value = Resource.Error(e.message ?: "Failed to accept swap", e)
                    Timber.e(e, "Failed to accept swap")
                }
                .also { removePendingAction(swapId) }
        }
    }

    fun confirmCompletion(swapId: String) {
        if (isActionPending(swapId)) return
        
        if (currentUserId.isEmpty()) return
        
        viewModelScope.launch {
            addPendingAction(swapId)
            _actionStatus.value = Resource.Loading()
            
            // We need to know if current user is A or B to pass correct flag to repository
            val currentSwaps = (swaps.value as? Resource.Success)?.data ?: emptyList()
            val swap = currentSwaps.find { it.id == swapId }
            
            if (swap != null) {
                val isUserA = swap.userA == currentUserId
                
                // Check if already confirmed
                val alreadyConfirmed = if (isUserA) swap.confirmedA else swap.confirmedB
                if (alreadyConfirmed) {
                    _actionStatus.value = Resource.Error("You have already confirmed this swap")
                    return@launch
                }
                
                swapRepository.confirmSwapAndProgress(swapId, isUserA)
                    .onSuccess {
                        _actionStatus.value = Resource.Success(Unit)
                        Timber.d("Swap completion confirmed successfully")
                    }
                    .onFailure { e ->
                        _actionStatus.value = Resource.Error(e.message ?: "Failed to confirm completion", e)
                        Timber.e(e, "Failed to confirm completion")
                    }
            } else {
                _actionStatus.value = Resource.Error("Swap not found")
                Timber.e("Swap not found: $swapId")
            }
            
            removePendingAction(swapId)
        }
    }

    fun cancelSwap(swapId: String) {
        if (isActionPending(swapId)) return
        
        viewModelScope.launch {
            addPendingAction(swapId)
            _actionStatus.value = Resource.Loading()
            
            swapRepository.updateSwapStatus(swapId, "cancelled")
                .onSuccess {
                    _actionStatus.value = Resource.Success(Unit)
                    Timber.d("Swap cancelled successfully")
                }
                .onFailure { e ->
                    _actionStatus.value = Resource.Error(e.message ?: "Failed to cancel swap", e)
                    Timber.e(e, "Failed to cancel swap")
                }
                .also { removePendingAction(swapId) }
        }
    }

    fun resetActionStatus() {
        _actionStatus.value = null
    }
    
    fun clearValidationErrors() {
        _validationErrors.value = emptyMap()
    }
    
    private fun isActionPending(id: String): Boolean {
        return _pendingActions.value.contains(id)
    }
    
    private fun addPendingAction(id: String) {
        _pendingActions.value = _pendingActions.value + id
    }
    
    private fun removePendingAction(id: String) {
        _pendingActions.value = _pendingActions.value - id
    }
}
