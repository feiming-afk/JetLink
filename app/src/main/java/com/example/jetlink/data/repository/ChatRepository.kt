package com.example.jetlink.data.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.jetlink.data.dao.ChatDao
import com.example.jetlink.data.entity.MessageEntity
import com.example.jetlink.data.entity.UserEntity
import com.example.jetlink.socket.SocketClientManager
import com.example.jetlink.util.ImageUtils
import com.example.jetlink.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 聊天数据仓库
 * 负责协调本地数据库操作和 Socket 网络通信。
 */
class ChatRepository(
    private val chatDao: ChatDao,
    private val context: Context // 需要 Context 来进行图片压缩
) {

    private var socketManager: SocketClientManager? = null

    init {
        // 创建 NotificationChannel
        NotificationHelper.createNotificationChannel(context)
    }

    fun setSocketManager(manager: SocketClientManager) {
        this.socketManager = manager
    }

    // 暴露 typing 事件流
    val typingFlow: SharedFlow<Pair<String, Boolean>>? 
        get() = socketManager?.typingFlow

    fun getUser(userId: String): Flow<UserEntity?> = chatDao.getUserById(userId)

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
     * 1. (图片) 复制到内部存储，更新 content 为 file:// Uri
     * 2. 将 MessageEntity 存入本地数据库
     * 3. (图片) 将 file:// Uri 压缩为 Base64
     * 4. 通过 Socket 发送给服务端
     * @param message 消息实体
     */
    suspend fun sendMessage(message: MessageEntity) = withContext(Dispatchers.IO) {
        
        var messageToSave = message
        var contentToSend = message.content
        val contentType = if (message.msgType == 1) "IMAGE" else "TEXT"

        // 1. (仅图片) 复制到内部存储并更新 content 为 file:// Uri
        if (message.msgType == 1) {
            val originalUri = Uri.parse(message.content)
            val localFileUri = ImageUtils.copyImageToInternalStorage(context, originalUri)

            if (localFileUri != null) {
                // 更新要存入数据库的实体
                messageToSave = message.copy(content = localFileUri.toString())
                // 压缩新的 file Uri 为 Base64 用于发送
                val base64 = ImageUtils.compressImageToBase64(context, localFileUri)
                if (base64 != null) {
                    contentToSend = base64
                } else {
                    // 压缩失败
                    // TODO: 抛出异常或返回 Result，通知 ViewModel
                    return@withContext
                }
            } else {
                // 文件复制失败
                // TODO: 抛出异常或返回 Result，通知 ViewModel
                return@withContext
            }
        }

        // 2. 保存到本地数据库
        chatDao.insertMessage(messageToSave)

        // 3. 通过 Socket 发送
        try {
            socketManager?.sendMessage(
                to = messageToSave.sessionId, 
                contentType = contentType,
                content = contentToSend,
                replyId = messageToSave.replyToId
            )
        } catch (e: Exception) {
            // 网络发送失败
            // TODO: 抛出异常或返回 Result，通知 ViewModel
            e.printStackTrace()
        }
    }

    /**
     * 保存接收到的消息
     * 这是给 SocketClientManager 回调用的
     */
    suspend fun saveReceivedMessage(
        senderId: String, 
        content: String, 
        msgType: Int, 
        replyToId: Long?,
        timestamp: Long
    ) {
        val message = MessageEntity(
            sessionId = senderId, // 简单逻辑：如果是单聊，sessionId 就是对方的 ID
            senderId = senderId,
            content = content, // 这里已经是本地 Uri 字符串(如果是图片) 或 文本
            msgType = msgType,
            replyToId = replyToId,
            timestamp = timestamp,
            isRead = false
        )
        chatDao.insertMessage(message)

        // 检查应用是否在后台，如果是则发送系统通知
        checkAndShowNotification(senderId, content, msgType)
    }

    private fun checkAndShowNotification(senderId: String, content: String, msgType: Int) {
        // 必须在主线程检查 ProcessLifecycleOwner 状态
        GlobalScope.launch(Dispatchers.Main) {
            val currentState = ProcessLifecycleOwner.get().lifecycle.currentState
            if (!currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                val notificationContent = if (msgType == 1) "[图片]" else content
                NotificationHelper.showNotification(
                    context,
                    senderId,
                    notificationContent,
                    senderId
                )
            }
        }
    }
    
    /**
     * 发送正在输入的状态
     */
    fun sendTypingStatus(to: String?) {
        socketManager?.sendTypingStatus(to)
    }

    /**
     * 清空指定会话的历史记录
     */
    suspend fun clearHistory(sessionId: String) {
        chatDao.clearHistory(sessionId)
    }

    /**
     * 删除单条消息
     */
    suspend fun deleteMessage(message: MessageEntity) {
        chatDao.deleteMessage(message)
    }
}
