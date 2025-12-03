package com.example.jetlink.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jetlink.ui.components.ChatBubble
import com.example.jetlink.ui.components.ChatInputBar
import com.example.jetlink.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    currentUserId: String,
    onBackClick: () -> Unit,
    onHeaderClick: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val messageMap by viewModel.messageMap.collectAsStateWithLifecycle()
    val replyingMessage by viewModel.replyingToMessage.collectAsStateWithLifecycle()
    val isOtherTyping by viewModel.isOtherTyping.collectAsStateWithLifecycle()
    val chatUser by viewModel.chatUser.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendImageMessage(uri)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 使用全屏布局，手动处理 Insets
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )
            )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            // 关键修改：让 Scaffold 处理 topBar 的 padding，但不处理 bottomBar 的 padding
            // 这样 Scaffold 的 content 就会占据除 TopBar 之外的所有空间（包括底部导航栏区域）
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onHeaderClick() }
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = chatUser?.userName?.take(1)?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = chatUser?.userName ?: "Chat", 
                                    style = MaterialTheme.typography.titleMedium
                                )
                                AnimatedVisibility(visible = isOtherTyping) {
                                    Text(
                                        text = "对方正在输入...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            // 使用 Column 布局，将输入框放在底部
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding) // 只应用 TopBar 的 padding
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp)
                ) {
                    items(
                        items = messages,
                        key = { it.msgId }
                    ) { message ->
                        val repliedMsg = message.replyToId?.let { replyId ->
                            messageMap[replyId]
                        }

                        ChatBubble(
                            message = message,
                            isMe = message.senderId == currentUserId,
                            repliedMessage = repliedMsg,
                            onReplyClick = { viewModel.replyMessage(it) },
                            onDeleteClick = { viewModel.deleteMessage(it) }
                        )
                    }
                }

                // 输入框放置在 Column 底部
                ChatInputBar(
                    onSendMessage = { content -> viewModel.sendMessage(content) },
                    onAddImageClick = { imagePickerLauncher.launch("image/*") },
                    onTyping = { viewModel.sendTypingStatus() },
                    onCancelReply = { viewModel.clearReply() },
                    replyingMessage = replyingMessage,
                    modifier = Modifier
                        // 修复：使用 union 合并 IME 和 NavigationBars，取最大值，避免叠加产生的空白
                        .windowInsetsPadding(
                            WindowInsets.ime.union(WindowInsets.navigationBars)
                        )
                )
            }
        }
    }
}
