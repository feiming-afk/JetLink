package com.example.jetlink.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jetlink.data.entity.MessageEntity
import com.example.jetlink.ui.theme.BubbleMeDark
import com.example.jetlink.ui.theme.BubbleMeLight
import com.example.jetlink.ui.theme.BubbleOtherDark
import com.example.jetlink.ui.theme.BubbleOtherLight
import com.example.jetlink.ui.theme.ChatBubbleShapeMe
import com.example.jetlink.ui.theme.ChatBubbleShapeOther
import com.example.jetlink.ui.theme.OnBubbleMe
import com.example.jetlink.ui.theme.OnBubbleOtherDark
import com.example.jetlink.ui.theme.OnBubbleOtherLight

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: MessageEntity,
    isMe: Boolean,
    isDarkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    onReplyClick: (MessageEntity) -> Unit = {},
    onDeleteClick: (MessageEntity) -> Unit = {},
    repliedMessage: MessageEntity? = null, // 被引用的原始消息
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    // Determine colors based on theme and sender
    val backgroundColor = if (isMe) {
        if (isDarkTheme) BubbleMeDark else BubbleMeLight
    } else {
        if (isDarkTheme) BubbleOtherDark else BubbleOtherLight
    }

    val contentColor = if (isMe) {
        OnBubbleMe
    } else {
        if (isDarkTheme) OnBubbleOtherDark else OnBubbleOtherLight
    }

    val bubbleShape = if (isMe) ChatBubbleShapeMe else ChatBubbleShapeOther

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(backgroundColor)
                .combinedClickable(
                    onClick = { /* 可以添加查看大图等逻辑 */ },
                    onLongClick = { showMenu = true }
                )
                .padding(8.dp)
        ) {
            Column {
                // 引用回复区域
                if (repliedMessage != null) {
                    ReplyQuote(
                        repliedMessage = repliedMessage,
                        contentColor = contentColor.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 消息内容区域
                when (message.msgType) {
                    1 -> { // IMAGE
                        AsyncImage(
                            model = message.content,
                            contentDescription = "Image Message",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> { // TEXT
                        Text(
                            text = message.content,
                            color = contentColor,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // 长按菜单
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (message.msgType == 0) { // 只有文本可以复制
                    DropdownMenuItem(
                        text = { Text("复制") },
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            showMenu = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("回复") },
                    onClick = {
                        onReplyClick(message)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = {
                        onDeleteClick(message)
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
fun ReplyQuote(
    repliedMessage: MessageEntity,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VerticalDivider(
            modifier = Modifier
                .width(2.dp)
                .padding(vertical = 2.dp),
            color = contentColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = repliedMessage.senderId, // 这里最好能转成用户名
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                text = if (repliedMessage.msgType == 1) "[图片]" else repliedMessage.content,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
