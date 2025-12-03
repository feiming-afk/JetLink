package com.example.jetlink.socket

import android.util.Log
import com.example.jetlink.model.SocketMessage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 简单的 Socket 服务端 (V2.0 Updated)
 * 用于接收客户端连接，并广播消息给所有连接的客户端
 */
object SocketServer {
    private const val TAG = "SocketServer"
    private const val PORT = 8888
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    
    // 存储所有连接的客户端 Socket
    private val clients = CopyOnWriteArrayList<Socket>()
    private val gson = Gson()

    fun startServer() {
        if (isRunning) return
        
        GlobalScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(PORT)
                isRunning = true
                Log.d(TAG, "Server started on port $PORT")

                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    Log.d(TAG, "New client connected: ${client.inetAddress}")
                    clients.add(client)
                    
                    // 为每个客户端开启单独的线程进行监听
                    launch(Dispatchers.IO) {
                        handleClient(client)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting server", e)
            }
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            clients.forEach { it.close() }
            clients.clear()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            while (isRunning && !socket.isClosed) {
                val json = input.readLine() ?: break
                
                // 对于 Base64 图片，日志可能太长，做个截断处理
                val logMsg = if (json.length > 200) json.substring(0, 200) + "..." else json
                Log.d(TAG, "Received: $logMsg")
                
                // 解析消息以验证格式
                try {
                    val message = gson.fromJson(json, SocketMessage::class.java)
                    if (message != null) {
                        // V2.0 更新：如果是广播消息 (或者 to 为 null/ALL)，则转发给所有人
                        // 这里为了简化，暂时还是全量广播，客户端自己过滤
                        broadcastMessage(json, socket)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Invalid message format", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
        } finally {
            clients.remove(socket)
            try {
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun broadcastMessage(messageJson: String, senderSocket: Socket?) {
        clients.forEach { client ->
            // 转发给所有客户端 (包括发送者自己，客户端会根据 ID 过滤或去重)
            try {
                if (!client.isClosed) {
                    val output = PrintWriter(client.getOutputStream(), true)
                    output.println(messageJson)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error broadcasting to client", e)
            }
        }
    }
}
