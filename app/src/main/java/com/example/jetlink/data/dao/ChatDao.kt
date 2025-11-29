package com.example.jetlink.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jetlink.data.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // 插入消息
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    // 获取指定会话的所有消息，按时间升序排列
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySessionId(sessionId: String): Flow<List<MessageEntity>>

    // 获取最近的会话列表
    // 这里通过分组查询每个会话的最新一条消息
    @Query("""
        SELECT * FROM messages 
        WHERE msgId IN (
            SELECT MAX(msgId) FROM messages GROUP BY sessionId
        ) 
        ORDER BY timestamp DESC
    """)
    fun getRecentSessions(): Flow<List<MessageEntity>>
}
