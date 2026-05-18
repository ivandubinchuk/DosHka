package com.example.doshka.data.local.dao

import androidx.room.*
import com.example.doshka.data.local.entity.LabelEntity
import com.example.doshka.data.local.entity.TaskLabelCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {

    @Query("SELECT * FROM labels WHERE id = :labelId")
    suspend fun getLabelById(labelId: String): LabelEntity?

    @Query("SELECT * FROM labels WHERE teamId = :teamId")
    fun observeLabelsByTeam(teamId: String): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE teamId = :teamId")
    suspend fun getLabelsByTeam(teamId: String): List<LabelEntity>

    @Query("""
        SELECT l.* FROM labels l
        INNER JOIN task_labels tl ON l.id = tl.labelId
        WHERE tl.taskId = :taskId
    """)
    fun observeLabelsByTask(taskId: String): Flow<List<LabelEntity>>

    @Query("""
        SELECT l.* FROM labels l
        INNER JOIN task_labels tl ON l.id = tl.labelId
        WHERE tl.taskId = :taskId
    """)
    suspend fun getLabelsByTask(taskId: String): List<LabelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: LabelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<LabelEntity>)

    @Update
    suspend fun updateLabel(label: LabelEntity)

    @Query("DELETE FROM labels WHERE id = :labelId")
    suspend fun deleteLabel(labelId: String)

    // Операції з TaskLabelCrossRef
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addLabelToTask(crossRef: TaskLabelCrossRef)

    @Delete
    suspend fun removeLabelFromTask(crossRef: TaskLabelCrossRef)

    @Query("DELETE FROM task_labels WHERE taskId = :taskId")
    suspend fun removeAllLabelsFromTask(taskId: String)

    @Query("DELETE FROM labels")
    suspend fun deleteAllLabels()

    @Query("DELETE FROM task_labels")
    suspend fun deleteAllTaskLabels()
}
