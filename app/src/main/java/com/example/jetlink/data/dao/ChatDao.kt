package com.example.jetlink.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jetlink.data.entity.MessageEntity
import com.example.jetlink.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    // === 用户相关 ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun getUserById(userId: String): Flow<UserEntity?>

    // === 消息相关 ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesBySessionId(sessionId: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE msgId IN (
            SELECT MAX(msgId) FROM messages GROUP BY sessionId
        ) 
        ORDER BY timestamp DESC
    """)
    fun getRecentSessions(): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE msgId = :msgId")
    suspend fun getMessageById(msgId: Long): MessageEntity?

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun clearHistory(sessionId: String)
}
