package com.example.doshka.di

import android.content.Context
import androidx.room.Room
import com.example.doshka.data.local.DoshkaDatabase
import com.example.doshka.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DoshkaDatabase {
        return Room.databaseBuilder(
            context,
            DoshkaDatabase::class.java,
            DoshkaDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(database: DoshkaDatabase): UserDao = database.userDao()

    @Provides
    fun provideTeamDao(database: DoshkaDatabase): TeamDao = database.teamDao()

    @Provides
    fun provideBoardDao(database: DoshkaDatabase): BoardDao = database.boardDao()

    @Provides
    fun provideColumnDao(database: DoshkaDatabase): ColumnDao = database.columnDao()

    @Provides
    fun provideTaskDao(database: DoshkaDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideLabelDao(database: DoshkaDatabase): LabelDao = database.labelDao()

    @Provides
    fun provideMessageDao(database: DoshkaDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideAttachmentDao(database: DoshkaDatabase): AttachmentDao = database.attachmentDao()

    @Provides
    fun providePendingOperationDao(database: DoshkaDatabase): PendingOperationDao = database.pendingOperationDao()
}
