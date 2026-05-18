package com.example.doshka.di

import com.example.doshka.data.repository.AuthRepositoryImpl
import com.example.doshka.data.repository.BoardRepositoryImpl
import com.example.doshka.data.repository.TaskRepositoryImpl
import com.example.doshka.domain.repository.AuthRepository
import com.example.doshka.domain.repository.BoardRepository
import com.example.doshka.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindBoardRepository(impl: BoardRepositoryImpl): BoardRepository
}
