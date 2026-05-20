package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.Message
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.data.repository.ChatRepository
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _sendStatus = MutableStateFlow<Resource<Unit>>(Resource.Idle())
    val sendStatus: StateFlow<Resource<Unit>> = _sendStatus.asStateFlow()

    private val _messages = MutableStateFlow<Resource<List<Message>>>(Resource.Idle())
    val messages: StateFlow<Resource<List<Message>>> = _messages.asStateFlow()

    val currentUserId: String
        get() = authRepository.currentUser?.uid ?: ""

    fun listenForMessages(threadId: String) {
        viewModelScope.launch {
            _messages.value = Resource.Loading()
            chatRepository.getMessages(threadId).collect {
                _messages.value = it
            }
        }
    }

    fun getMessages(threadId: String): Flow<Resource<List<Message>>> {
        return chatRepository.getMessages(threadId)
    }

    fun sendMessage(threadId: String, text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            _sendStatus.value = Resource.Loading()
            val message = Message(
                senderId = currentUserId,
                text = text,
                timestamp = com.google.firebase.Timestamp.now()
            )
            val result = chatRepository.sendMessage(threadId, message)
            if (result.isSuccess) {
                _sendStatus.value = Resource.Success(Unit)
            } else {
                _sendStatus.value = Resource.Error(result.exceptionOrNull()?.message ?: "Failed to send message")
            }
        }
    }

    fun resetSendStatus() {
        _sendStatus.value = Resource.Idle()
    }
}
