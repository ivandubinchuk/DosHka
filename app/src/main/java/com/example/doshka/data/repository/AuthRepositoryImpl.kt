package com.example.doshka.data.repository

import com.example.doshka.data.local.DataStoreManager
import com.example.doshka.data.local.dao.UserDao
import com.example.doshka.data.local.entity.UserEntity
import com.example.doshka.data.remote.api.DoshkaApi
import com.example.doshka.data.remote.dto.LoginRequest
import com.example.doshka.data.remote.dto.RefreshTokenRequest
import com.example.doshka.data.remote.dto.RegisterRequest
import com.example.doshka.data.remote.dto.UserDto
import com.example.doshka.domain.model.User
import com.example.doshka.domain.model.UserRole
import com.example.doshka.domain.repository.AuthRepository
import com.example.doshka.util.DateTimeUtils
import com.example.doshka.util.NetworkResult
import com.example.doshka.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: DoshkaApi,
    private val userDao: UserDao,
    private val dataStoreManager: DataStoreManager
) : AuthRepository {

    override fun observeAuthState(): Flow<Boolean> {
        return dataStoreManager.accessToken.map { !it.isNullOrBlank() }
    }

    override fun observeCurrentUser(): Flow<User?> {
        return dataStoreManager.userId.map { userId ->
            userId?.let { userDao.getUserById(it)?.toDomain() }
        }
    }

    override suspend fun getCurrentUser(): User? {
        val userId = dataStoreManager.userId.first()
        return userId?.let { userDao.getUserById(it)?.toDomain() }
    }

    override suspend fun login(email: String, password: String): NetworkResult<User> {
        return safeApiCall {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val tokens = response.body()!!
                dataStoreManager.saveTokens(
                    tokens.accessToken,
                    tokens.refreshToken,
                    tokens.expiresIn
                )

                // Отримуємо профіль користувача
                val userResponse = api.getCurrentUser()
                if (userResponse.isSuccessful && userResponse.body() != null) {
                    val userDto = userResponse.body()!!
                    val userEntity = userDto.toEntity()
                    userDao.insertUser(userEntity)
                    dataStoreManager.saveUser(
                        userDto.id,
                        userDto.email,
                        userDto.fullName,
                        userDto.role,
                        userDto.teamId
                    )
                    userEntity.toDomain()
                } else {
                    throw Exception("Не вдалося отримати профіль користувача")
                }
            } else {
                throw Exception("Невірний email або пароль")
            }
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        fullName: String,
        role: String
    ): NetworkResult<User> {
        return safeApiCall {
            val request = RegisterRequest(email, password, fullName, role)
            val response = api.register(request)
            if (response.isSuccessful && response.body() != null) {
                val tokens = response.body()!!
                dataStoreManager.saveTokens(
                    tokens.accessToken,
                    tokens.refreshToken,
                    tokens.expiresIn
                )

                val userResponse = api.getCurrentUser()
                if (userResponse.isSuccessful && userResponse.body() != null) {
                    val userDto = userResponse.body()!!
                    val userEntity = userDto.toEntity()
                    userDao.insertUser(userEntity)
                    dataStoreManager.saveUser(
                        userDto.id,
                        userDto.email,
                        userDto.fullName,
                        userDto.role,
                        userDto.teamId
                    )
                    userEntity.toDomain()
                } else {
                    throw Exception("Не вдалося отримати профіль користувача")
                }
            } else {
                throw Exception("Не вдалося зареєструватися")
            }
        }
    }

    override suspend fun registerWithInvite(
        email: String,
        password: String,
        fullName: String,
        inviteToken: String
    ): NetworkResult<User> {
        return safeApiCall {
            val request = RegisterRequest(
                email = email,
                password = password,
                fullName = fullName,
                role = "executor",
                inviteToken = inviteToken
            )
            val response = api.registerWithInvite(request)
            if (response.isSuccessful && response.body() != null) {
                val tokens = response.body()!!
                dataStoreManager.saveTokens(
                    tokens.accessToken,
                    tokens.refreshToken,
                    tokens.expiresIn
                )

                val userResponse = api.getCurrentUser()
                if (userResponse.isSuccessful && userResponse.body() != null) {
                    val userDto = userResponse.body()!!
                    val userEntity = userDto.toEntity()
                    userDao.insertUser(userEntity)
                    dataStoreManager.saveUser(
                        userDto.id,
                        userDto.email,
                        userDto.fullName,
                        userDto.role,
                        userDto.teamId
                    )
                    userEntity.toDomain()
                } else {
                    throw Exception("Не вдалося отримати профіль користувача")
                }
            } else {
                throw Exception("Невірний код запрошення")
            }
        }
    }

    override suspend fun logout() {
        try {
            api.logout()
        } catch (_: Exception) {
            // Ігноруємо помилки при виході
        }
        dataStoreManager.clearTokens()
        dataStoreManager.clearUser()
    }

    override suspend fun refreshToken(): NetworkResult<Unit> {
        return safeApiCall {
            val refreshToken = dataStoreManager.refreshToken.first()
                ?: throw Exception("Refresh token не знайдено")

            val response = api.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful && response.body() != null) {
                val tokens = response.body()!!
                dataStoreManager.saveTokens(
                    tokens.accessToken,
                    tokens.refreshToken,
                    tokens.expiresIn
                )
            } else {
                throw Exception("Не вдалося оновити токен")
            }
        }
    }

    override suspend fun isTokenValid(): Boolean {
        val expiry = dataStoreManager.tokenExpiry.first()
        return expiry > System.currentTimeMillis() + 60000 // + 1 хвилина запасу
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStoreManager.setBiometricEnabled(enabled)
    }

    override suspend fun isBiometricEnabled(): Boolean {
        return dataStoreManager.biometricEnabled.first()
    }

    override suspend fun authenticateWithBiometric(): NetworkResult<User> {
        return safeApiCall {
            // Спочатку оновлюємо токен
            val refreshToken = dataStoreManager.refreshToken.first()
                ?: throw Exception("Refresh token не знайдено")

            val response = api.refreshToken(RefreshTokenRequest(refreshToken))
            if (response.isSuccessful && response.body() != null) {
                val tokens = response.body()!!
                dataStoreManager.saveTokens(
                    tokens.accessToken,
                    tokens.refreshToken,
                    tokens.expiresIn
                )

                // Отримуємо профіль користувача
                val userResponse = api.getCurrentUser()
                if (userResponse.isSuccessful && userResponse.body() != null) {
                    val userDto = userResponse.body()!!
                    val userEntity = userDto.toEntity()
                    userDao.insertUser(userEntity)
                    userEntity.toDomain()
                } else {
                    throw Exception("Не вдалося отримати профіль користувача")
                }
            } else {
                throw Exception("Сесія закінчилася. Увійдіть повторно.")
            }
        }
    }

    // Маппери
    private fun UserEntity.toDomain(): User {
        return User(
            id = id,
            email = email,
            fullName = fullName,
            role = UserRole.fromString(role),
            avatarUrl = avatarUrl,
            teamId = teamId,
            isActive = isActive
        )
    }

    private fun UserDto.toEntity(): UserEntity {
        return UserEntity(
            id = id,
            email = email,
            fullName = fullName,
            role = role,
            avatarUrl = avatarUrl,
            teamId = teamId,
            isActive = isActive,
            createdAt = if (createdAt.isNotBlank()) DateTimeUtils.parseToEpochMilli(createdAt) else System.currentTimeMillis(),
            updatedAt = if (updatedAt.isNotBlank()) DateTimeUtils.parseToEpochMilli(updatedAt) else System.currentTimeMillis()
        )
    }
}
