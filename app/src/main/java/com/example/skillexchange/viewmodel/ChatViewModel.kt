package com.example.skillexchange.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.model.Message
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.data.repository.ChatRepository
import com.example.skillexchange.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _messages = MutableStateFlow<Resource<List<Message>>>(Resource.Loading())
    val messages: StateFlow<Resource<List<Message>>> = _messages.asStateFlow()

    val currentUserId = authRepository.currentUser?.uid ?: ""

    fun listenForMessages(threadId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(threadId).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(threadId: String, text: String) {
        if (text.isBlank()) return
        val message = Message(
            senderId = currentUserId,
            text = text
        )
        viewModelScope.launch {
            chatRepository.sendMessage(threadId, message)
        }
    }
}
