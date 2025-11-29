package com.example.jetlink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jetlink.data.entity.MessageEntity
import com.example.jetlink.data.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val currentSessionId: String,
    private val currentUserId: String
) : ViewModel() {

    // 暴露给 UI 的消息列表状态
    val messages: StateFlow<List<MessageEntity>> = repository.getMessagesStream(currentSessionId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sendMessage(content: String) {
        viewModelScope.launch {
            val newMessage = MessageEntity(
                sessionId = currentSessionId,
                senderId = currentUserId,
                content = content,
                msgType = "TEXT",
                timestamp = System.currentTimeMillis(),
                isRead = true // 自己发的消息默认已读
            )
            repository.insertMessage(newMessage)
        }
    }

    // 用于创建 ViewModel 的工厂类
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
