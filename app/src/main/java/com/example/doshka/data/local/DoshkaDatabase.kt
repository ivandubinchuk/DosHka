package com.example.doshka.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.doshka.data.local.dao.*
import com.example.doshka.data.local.entity.*

/**
 * Головна база даних Room для DoshKa
 */
@Database(
    entities = [
        UserEntity::class,
        TeamEntity::class,
        BoardEntity::class,
        ColumnEntity::class,
        TaskEntity::class,
        LabelEntity::class,
        TaskLabelCrossRef::class,
        MessageEntity::class,
        AttachmentEntity::class,
        PendingOperationEntity::class,
        TaskTemplateEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class DoshkaDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun teamDao(): TeamDao
    abstract fun boardDao(): BoardDao
    abstract fun columnDao(): ColumnDao
    abstract fun taskDao(): TaskDao
    abstract fun labelDao(): LabelDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun pendingOperationDao(): PendingOperationDao

    companion object {
        const val DATABASE_NAME = "doshka_database"
    }
}
