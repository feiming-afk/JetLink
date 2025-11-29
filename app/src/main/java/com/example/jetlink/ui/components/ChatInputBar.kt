package com.example.jetlink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jetlink.ui.theme.JetLinkTheme

@Composable
fun ChatInputBar(
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }

    // 简单的 Emoji 列表
    val emojis = listOf(
        "😀", "😂", "😍", "😎", "😭", "😡", "👍", "👎", "🎉", "❤️",
        "🤔", "🙄", "😴", "🤮", "🤯", "🥳", "🥴", "🥵", "🥶", "😱",
        "👋", "🙌", "👏", "🤝", "🙏", "💪", "👀", "🧠", "🔥", "✨"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 表情按钮
                IconButton(
                    onClick = { showEmojiPicker = !showEmojiPicker }
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "表情",
                        tint = if (showEmojiPicker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    placeholder = { Text("输入消息...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSendMessage(text)
                            text = ""
                            showEmojiPicker = false
                        }
                    },
                    enabled = text.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            // 表情选择区域
            if (showEmojiPicker) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 40.dp),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    text += emoji
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatInputBarPreview() {
    JetLinkTheme {
        ChatInputBar(onSendMessage = {})
    }
}
