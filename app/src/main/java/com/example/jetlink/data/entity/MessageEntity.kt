package com.example.jetlink.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val msgId: Long = 0,
    val sessionId: String,
    val senderId: String,
    val msgType: Int, // 0=TEXT, 1=IMAGE, 2=SYSTEM
    val content: String, // Text or Local Uri String or Base64
    val replyToId: Long? = null,
    val timestamp: Long,
    val isRead: Boolean = false
)
