package com.example.jetlink.model

import com.google.gson.annotations.SerializedName

/**
 * Socket 通信的消息协议模型
 */
data class SocketMessage(
    @SerializedName("type")
    val type: String, // MSG
    
    @SerializedName("from")
    val from: String, // 发送者ID
    
    @SerializedName("content")
    val content: String // 消息内容
)
