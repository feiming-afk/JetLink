package com.example.jetlink.data.repository

import com.example.jetlink.data.dao.ChatDao
import com.example.jetlink.data.entity.MessageEntity
import com.example.jetlink.socket.SocketClientManager
import kotlinx.coroutines.flow.Flow

/**
 * 聊天数据仓库
 * 负责协调本地数据库操作和 Socket 网络通信。
 */
class ChatRepository(private val chatDao: ChatDao) {

    private var socketManager: SocketClientManager? = null

    fun setSocketManager(manager: SocketClientManager) {
        this.socketManager = manager
    }

    /**
     * 获取指定会话的所有消息列表流
     * @param sessionId 会话ID
     */
    fun getMessagesStream(sessionId: String): Flow<List<MessageEntity>> =
        chatDao.getMessagesBySessionId(sessionId)

    /**
     * 获取最近的会话列表流
     */
    fun getRecentSessionsStream(): Flow<List<MessageEntity>> =
        chatDao.getRecentSessions()

    /**
     * 发送消息
     * 1. 保存到本地数据库
     * 2. 通过 Socket 发送给服务端
     * @param message 消息实体
     */
    suspend fun sendMessage(message: MessageEntity) {
        // 保存到本地
        chatDao.insertMessage(message)
        
        // 通过 Socket 发送
        // 注意：这里为了简化，我们只发送文本内容，接收端需要知道是谁发的
        // 实际协议中通常包含更多信息
        socketManager?.sendMessage(message.content)
    }

    /**
     * 保存接收到的消息
     * 这是给 SocketClientManager 回调用的
     */
    suspend fun saveReceivedMessage(senderId: String, content: String) {
        val message = MessageEntity(
            sessionId = senderId, // 简单逻辑：如果是单聊，sessionId 就是对方的 ID
            senderId = senderId,
            content = content,
            msgType = "TEXT",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        chatDao.insertMessage(message)
    }
    
    // 仅用于本地测试插入，不走网络
    suspend fun insertMessage(message: MessageEntity) {
        chatDao.insertMessage(message)
    }
}
