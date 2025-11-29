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
 * 简单的 Socket 服务端
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
                Log.d(TAG, "Received: $json")
                
                // 解析消息以验证格式（这里简单解析，主要为了转发）
                try {
                    val message = gson.fromJson(json, SocketMessage::class.java)
                    if (message != null) {
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
            // 这里为了简化逻辑，也回传给发送者，实际应用中可能不需要
            // 或者由客户端根据 from 字段判断是否是自己
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
