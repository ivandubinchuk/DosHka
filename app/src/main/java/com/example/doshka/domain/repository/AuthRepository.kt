package com.example.doshka.domain.repository

import com.example.doshka.domain.model.User
import com.example.doshka.util.NetworkResult
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторій для аутентифікації
 */
interface AuthRepository {

    /**
     * Спостерігає за станом авторизації
     */
    fun observeAuthState(): Flow<Boolean>

    /**
     * Спостерігає за поточним користувачем
     */
    fun observeCurrentUser(): Flow<User?>

    /**
     * Отримує поточного користувача
     */
    suspend fun getCurrentUser(): User?

    /**
     * Вхід по email та паролю
     */
    suspend fun login(email: String, password: String): NetworkResult<User>

    /**
     * Реєстрація нового користувача
     */
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        role: String = "executor"
    ): NetworkResult<User>

    /**
     * Реєстрація за запрошенням
     */
    suspend fun registerWithInvite(
        email: String,
        password: String,
        fullName: String,
        inviteToken: String
    ): NetworkResult<User>

    /**
     * Вихід з системи
     */
    suspend fun logout()

    /**
     * Оновлення токена
     */
    suspend fun refreshToken(): NetworkResult<Unit>

    /**
     * Перевірка чи токен валідний
     */
    suspend fun isTokenValid(): Boolean

    /**
     * Зберігає налаштування біометрії
     */
    suspend fun setBiometricEnabled(enabled: Boolean)

    /**
     * Перевіряє чи біометрія увімкнена
     */
    suspend fun isBiometricEnabled(): Boolean

    /**
     * Виконує аутентифікацію за допомогою збереженого refresh токена (після біометрії)
     */
    suspend fun authenticateWithBiometric(): NetworkResult<User>
}
