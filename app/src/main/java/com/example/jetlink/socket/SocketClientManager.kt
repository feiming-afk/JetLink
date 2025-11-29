package com.example.jetlink.socket

import android.util.Log
import com.example.jetlink.data.repository.ChatRepository
import com.example.jetlink.model.SocketMessage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * 客户端 Socket 管理器
 * 负责连接服务端、发送消息、接收消息并回调
 */
class SocketClientManager(
    private val repository: ChatRepository,
    private val currentUserId: String
) {
    private var socket: Socket? = null
    private var output: PrintWriter? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isConnected = false

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
                Log.d(TAG, "Received: $json")
                handleIncomingMessage(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listening for messages", e)
        } finally {
            disconnect()
        }
    }

    private suspend fun handleIncomingMessage(json: String) {
        try {
            val socketMessage = gson.fromJson(json, SocketMessage::class.java)
            if (socketMessage != null && socketMessage.type == "MSG") {
                // 如果是自己发的消息，通常已经在本地保存了，这里可以选择忽略或更新状态
                // 但为了简化，如果是自己发的，我们已经在 sendMessage 时保存了，这里只处理别人的消息
                if (socketMessage.from != currentUserId) {
                    repository.saveReceivedMessage(
                        senderId = socketMessage.from,
                        content = socketMessage.content
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message", e)
        }
    }

    fun sendMessage(content: String) {
        scope.launch {
            if (isConnected && output != null) {
                val message = SocketMessage(
                    type = "MSG",
                    from = currentUserId,
                    content = content
                )
                val json = gson.toJson(message)
                try {
                    output?.println(json)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send message", e)
                    disconnect() // 发送失败可能意味着连接断开
                }
            } else {
                Log.w(TAG, "Not connected, message ignored")
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
