package com.example.jetlink.model

import com.google.gson.annotations.SerializedName

/**
 * Socket 通信的消息协议模型 (Updated for V2.0)
 */
data class SocketMessage(
    @SerializedName("type")
    val type: String, // MSG | TYPING | HEARTBEAT
    
    @SerializedName("from")
    val from: String, // 发送者ID

    @SerializedName("to")
    val to: String? = null, // 目标ID (广播模式可为空或 "ALL")

    @SerializedName("payload")
    val payload: MessagePayload? = null, // 消息载荷

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

data class MessagePayload(
    @SerializedName("contentType")
    val contentType: String, // TEXT | IMAGE
    
    @SerializedName("content")
    val content: String, // 文本内容 或 Base64 字符串

    @SerializedName("replyId")
    val replyId: Long? = null // 引用回复的消息 ID
)
