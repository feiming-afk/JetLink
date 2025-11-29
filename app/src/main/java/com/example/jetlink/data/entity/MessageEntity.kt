package com.example.jetlink.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val msgId: Long = 0,
    val sessionId: String,
    val senderId: String,
    val content: String,
    val msgType: String, // TEXT or IMAGE
    val timestamp: Long,
    val isRead: Boolean = false
)
