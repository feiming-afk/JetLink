package com.example.jetlink.socket

import android.content.Context
import android.util.Log
import com.example.jetlink.model.MessagePayload
import com.example.jetlink.model.SocketMessage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * 客户端 Socket 管理器 (V2.0 Updated)
 * 负责连接服务端、发送消息、接收消息并回调
 */
class SocketClientManager(
    private val context: Context, // 需要 Context 来保存接收到的图片
    private val currentUserId: String
) {
    private var socket: Socket? = null
    private var output: PrintWriter? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isConnected = false

    // 暴露 TYPING 事件流 (senderId, isTyping)
    private val _typingFlow = MutableSharedFlow<Pair<String, Boolean>>()
    val typingFlow: SharedFlow<Pair<String, Boolean>> = _typingFlow.asSharedFlow()

    private val _messageFlow = MutableSharedFlow<SocketMessage>()
    val messageFlow: SharedFlow<SocketMessage> = _messageFlow.asSharedFlow()

    companion object {
        private const val TAG = "SocketClientManager"
        private const val SERVER_IP = "10.0.2.2" // Android 模拟器访问宿主机 localhost
        private const val SERVER_PORT = 8888
    }

    fun connect() {
        scope.launch {
            while (isActive) {
                if (!isConnected) {
                    try {
                        Log.d(TAG, "Connecting to $SERVER_IP:$SERVER_PORT...")
                        socket = Socket(SERVER_IP, SERVER_PORT)
                        output = PrintWriter(socket!!.getOutputStream(), true)
                        isConnected = true
                        Log.d(TAG, "Connected!")

                        // 启动接收线程
                        launch { listenForMessages() }
                    } catch (e: Exception) {
                        Log.e(TAG, "Connection failed, retrying in 3s...", e)
                        delay(3000)
                    }
                } else {
                    delay(5000) // 保持检查间隔
                }
            }
        }
    }

    private suspend fun listenForMessages() {
        try {
            val input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            while (isConnected) {
                val json = input.readLine() ?: break
                
                val logMsg = if (json.length > 200) json.substring(0, 200) + "..." else json
                Log.d(TAG, "Received: $logMsg")
                
                try {
                    val socketMessage = gson.fromJson(json, SocketMessage::class.java)
                    if (socketMessage != null && socketMessage.from != currentUserId) {
                        _messageFlow.emit(socketMessage)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse message", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listening for messages", e)
        } finally {
            disconnect()
        }
    }

    suspend fun sendMessage(to: String?, contentType: String, content: String, replyId: Long? = null) {
        if (!isConnected || output == null) {
            throw IllegalStateException("Socket not connected")
        }
        val payload = MessagePayload(
            contentType = contentType,
            content = content,
            replyId = replyId
        )
        
        val message = SocketMessage(
            type = "MSG",
            from = currentUserId,
            to = to,
            payload = payload
        )
        
        val json = gson.toJson(message)
        output?.println(json)
    }

    // sendTypingStatus 也可以是 suspend, 但因为它不那么关键，我们允许它在内部 scope 中快速失败
    fun sendTypingStatus(to: String?) {
        scope.launch {
            if (isConnected && output != null) {
                val message = SocketMessage(
                    type = "TYPING",
                    from = currentUserId,
                    to = to
                )
                
                val json = gson.toJson(message)
                try {
                    output?.println(json)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send typing status", e)
                }
            }
        }
    }

    private fun disconnect() {
        isConnected = false
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
