package com.example.doshka.data.local.dao

import androidx.room.*
import com.example.doshka.data.local.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {

    @Query("SELECT * FROM pending_operations WHERE status = 'pending' ORDER BY createdAt ASC")
    fun observePendingOperations(): Flow<List<PendingOperationEntity>>

    @Query("SELECT * FROM pending_operations WHERE status = 'pending' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE status = 'failed' ORDER BY createdAt ASC")
    suspend fun getFailedOperations(): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE status = 'pending'")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: PendingOperationEntity): Long

    @Update
    suspend fun updateOperation(operation: PendingOperationEntity)

    @Query("UPDATE pending_operations SET status = :status, lastAttemptAt = :timestamp WHERE id = :operationId")
    suspend fun updateOperationStatus(operationId: Long, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE pending_operations SET retryCount = retryCount + 1, lastAttemptAt = :timestamp WHERE id = :operationId")
    suspend fun incrementRetryCount(operationId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE pending_operations SET status = 'failed', errorMessage = :errorMessage WHERE id = :operationId")
    suspend fun markAsFailed(operationId: Long, errorMessage: String)

    @Query("DELETE FROM pending_operations WHERE id = :operationId")
    suspend fun deleteOperation(operationId: Long)

    @Query("DELETE FROM pending_operations WHERE status = 'completed'")
    suspend fun deleteCompletedOperations()

    @Query("DELETE FROM pending_operations WHERE status = 'pending'")
    suspend fun deleteAllPending()

    @Query("DELETE FROM pending_operations WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteByEntity(entityType: String, entityId: String)

    @Query("DELETE FROM pending_operations")
    suspend fun deleteAllOperations()
}
