package com.example.doshka.data.local.dao

import androidx.room.*
import com.example.doshka.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun observeMessagesByTask(taskId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getMessagesByTask(taskId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE taskId = :taskId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMessages(taskId: String, limit: Int): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE taskId = :taskId AND isRead = 0")
    fun observeUnreadCount(taskId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET isRead = 1 WHERE taskId = :taskId AND isRead = 0")
    suspend fun markAllAsRead(taskId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE taskId = :taskId")
    suspend fun deleteMessagesByTask(taskId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    // Синхронізація
    @Query("UPDATE messages SET id = :newId WHERE id = :oldId")
    suspend fun updateMessageId(oldId: String, newId: String)

    @Query("UPDATE messages SET isSynced = 1 WHERE id = :messageId")
    suspend fun markAsSynced(messageId: String)

    @Query("UPDATE messages SET isSynced = 0 WHERE id = :messageId")
    suspend fun markAsUnsynced(messageId: String)

    @Query("SELECT * FROM messages WHERE isSynced = 0")
    suspend fun getUnsyncedMessages(): List<MessageEntity>
}
