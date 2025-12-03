package com.example.jetlink.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jetlink.data.entity.MessageEntity
import com.example.jetlink.data.entity.UserEntity
import com.example.jetlink.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val currentSessionId: String,
    private val currentUserId: String
) : ViewModel() {

    val messages: StateFlow<List<MessageEntity>> = repository.getMessagesStream(currentSessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messageMap: StateFlow<Map<Long, MessageEntity>> = messages
        .map { list -> list.associateBy { it.msgId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val chatUser: StateFlow<UserEntity?> = repository.getUser(currentSessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _replyingToMessage = MutableStateFlow<MessageEntity?>(null)
    val replyingToMessage: StateFlow<MessageEntity?> = _replyingToMessage.asStateFlow()

    private val _isOtherTyping = MutableStateFlow(false)
    val isOtherTyping: StateFlow<Boolean> = _isOtherTyping.asStateFlow()
    private var typingJob: Job? = null
    
    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    init {
        listenForTypingStatus()
    }

    private fun listenForTypingStatus() {
        repository.typingFlow?.onEach { (senderId, isTyping) ->
            if (senderId == currentSessionId && isTyping) {
                _isOtherTyping.value = true
                typingJob?.cancel()
                typingJob = viewModelScope.launch {
                    delay(3000)
                    _isOtherTyping.value = false
                }
            }
        }?.launchIn(viewModelScope)
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            val replyId = _replyingToMessage.value?.msgId
            val newMessage = MessageEntity(
                sessionId = currentSessionId,
                senderId = currentUserId,
                content = content,
                msgType = 0, // TEXT
                replyToId = replyId,
                timestamp = System.currentTimeMillis(),
                isRead = true
            )
            try {
                repository.sendMessage(newMessage)
                clearReply()
            } catch (e: Exception) {
                _errorEvents.emit("发送失败: ${e.message}")
            }
        }
    }

    fun sendImageMessage(uri: Uri) {
        viewModelScope.launch {
            val replyId = _replyingToMessage.value?.msgId
            val newMessage = MessageEntity(
                sessionId = currentSessionId,
                senderId = currentUserId,
                content = uri.toString(),
                msgType = 1, // IMAGE
                replyToId = replyId,
                timestamp = System.currentTimeMillis(),
                isRead = true
            )
            try {
                repository.sendMessage(newMessage)
                clearReply()
            } catch (e: Exception) {
                _errorEvents.emit("图片发送失败: ${e.message}")
            }
        }
    }

    fun deleteMessage(message: MessageEntity) {
        viewModelScope.launch { repository.deleteMessage(message) }
    }

    fun replyMessage(originalMsg: MessageEntity) {
        _replyingToMessage.value = originalMsg
    }

    fun clearReply() {
        _replyingToMessage.value = null
    }

    fun sendTypingStatus() {
        repository.sendTypingStatus(to = currentSessionId)
    }

    class Factory(
        private val repository: ChatRepository,
        private val sessionId: String,
        private val userId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(repository, sessionId, userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
