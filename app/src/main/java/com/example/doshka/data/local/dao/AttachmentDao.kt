package com.example.doshka.data.local.dao

import androidx.room.*
import com.example.doshka.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE id = :attachmentId")
    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE taskId = :taskId ORDER BY createdAt DESC")
    fun observeAttachmentsByTask(taskId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE taskId = :taskId ORDER BY createdAt DESC")
    suspend fun getAttachmentsByTask(taskId: String): List<AttachmentEntity>

    @Query("SELECT COUNT(*) FROM attachments WHERE taskId = :taskId")
    suspend fun getAttachmentCount(taskId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Update
    suspend fun updateAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteAttachment(attachmentId: String)

    @Query("DELETE FROM attachments WHERE taskId = :taskId")
    suspend fun deleteAttachmentsByTask(taskId: String)

    @Query("DELETE FROM attachments")
    suspend fun deleteAllAttachments()
}
