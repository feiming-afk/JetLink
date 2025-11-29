package com.example.jetlink.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetlink.ui.components.ChatBubble
import com.example.jetlink.ui.components.ChatInputBar
import com.example.jetlink.ui.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    currentUserId: String
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // 只有当消息数量增加时，才滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // 键盘弹出时调整底部 Padding
        bottomBar = {
            ChatInputBar(
                onSendMessage = { content ->
                    viewModel.sendMessage(content)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                items(messages) { message ->
                    ChatBubble(
                        message = message.content,
                        isMe = message.senderId == currentUserId
                    )
                }
            }
        }
    }
}
