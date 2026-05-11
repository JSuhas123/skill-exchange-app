package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.ExchangeRequest
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.data.repository.ExchangeRepository
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val exchangeRepository: ExchangeRepository
) : ViewModel() {

    private val _requests = MutableStateFlow<Resource<List<ExchangeRequest>>>(Resource.Loading())
    val requests: StateFlow<Resource<List<ExchangeRequest>>> = _requests

    init {
        fetchRequests()
    }

    private fun fetchRequests() {
        val userId = authRepository.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                // Placeholder for fetching requests
                _requests.value = Resource.Success(emptyList())
            }
        }
    }

    fun updateRequestStatus(requestId: String, status: String) {
        viewModelScope.launch {
            // Logic to update request status
        }
    }
}
