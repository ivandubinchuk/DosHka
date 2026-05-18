package com.example.doshka.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doshka.domain.model.User
import com.example.doshka.domain.repository.AuthRepository
import com.example.doshka.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Стан екрану авторизації
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val biometricEnabled: Boolean = false,
    val showBiometricPrompt: Boolean = false
)

/**
 * Стан форми входу
 */
data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null
)

/**
 * Стан форми реєстрації
 */
data class RegisterFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val role: String = "executor",
    val inviteToken: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val fullNameError: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _loginForm = MutableStateFlow(LoginFormState())
    val loginForm: StateFlow<LoginFormState> = _loginForm.asStateFlow()

    private val _registerForm = MutableStateFlow(RegisterFormState())
    val registerForm: StateFlow<RegisterFormState> = _registerForm.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { isLoggedIn ->
                val biometricEnabled = authRepository.isBiometricEnabled()
                _uiState.update {
                    it.copy(
                        isLoggedIn = isLoggedIn,
                        biometricEnabled = biometricEnabled,
                        showBiometricPrompt = isLoggedIn && biometricEnabled
                    )
                }

                if (isLoggedIn) {
                    loadUser()
                }
            }
        }
    }

    private suspend fun loadUser() {
        val user = authRepository.getCurrentUser()
        _uiState.update { it.copy(user = user) }
    }

    // Оновлення форми входу
    fun updateLoginEmail(email: String) {
        _loginForm.update { it.copy(email = email, emailError = null) }
    }

    fun updateLoginPassword(password: String) {
        _loginForm.update { it.copy(password = password, passwordError = null) }
    }

    // Оновлення форми реєстрації
    fun updateRegisterEmail(email: String) {
        _registerForm.update { it.copy(email = email, emailError = null) }
    }

    fun updateRegisterPassword(password: String) {
        _registerForm.update { it.copy(password = password, passwordError = null) }
    }

    fun updateRegisterConfirmPassword(confirmPassword: String) {
        _registerForm.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun updateRegisterFullName(fullName: String) {
        _registerForm.update { it.copy(fullName = fullName, fullNameError = null) }
    }

    fun updateRegisterRole(role: String) {
        _registerForm.update { it.copy(role = role) }
    }

    fun setInviteToken(token: String?) {
        _registerForm.update { it.copy(inviteToken = token) }
    }

    // Вхід
    fun login() {
        val form = _loginForm.value

        // Валідація
        var hasErrors = false
        if (form.email.isBlank() || !form.email.contains("@")) {
            _loginForm.update { it.copy(emailError = "Введіть коректний email") }
            hasErrors = true
        }
        if (form.password.isBlank() || form.password.length < 6) {
            _loginForm.update { it.copy(passwordError = "Пароль має містити мінімум 6 символів") }
            hasErrors = true
        }

        if (hasErrors) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.login(form.email, form.password)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            user = result.data
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    // Реєстрація
    fun register() {
        val form = _registerForm.value

        // Валідація
        var hasErrors = false
        if (form.email.isBlank() || !form.email.contains("@")) {
            _registerForm.update { it.copy(emailError = "Введіть коректний email") }
            hasErrors = true
        }
        if (form.password.isBlank() || form.password.length < 6) {
            _registerForm.update { it.copy(passwordError = "Пароль має містити мінімум 6 символів") }
            hasErrors = true
        }
        if (form.password != form.confirmPassword) {
            _registerForm.update { it.copy(confirmPasswordError = "Паролі не співпадають") }
            hasErrors = true
        }
        if (form.fullName.isBlank() || form.fullName.length < 2) {
            _registerForm.update { it.copy(fullNameError = "Введіть повне ім'я") }
            hasErrors = true
        }

        if (hasErrors) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = if (form.inviteToken != null) {
                authRepository.registerWithInvite(
                    form.email,
                    form.password,
                    form.fullName,
                    form.inviteToken
                )
            } else {
                authRepository.register(
                    form.email,
                    form.password,
                    form.fullName,
                    form.role
                )
            }

            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            user = result.data
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    // Біометрична аутентифікація
    fun authenticateWithBiometric() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.authenticateWithBiometric()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            user = result.data,
                            showBiometricPrompt = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message,
                            showBiometricPrompt = false
                        )
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun dismissBiometricPrompt() {
        _uiState.update { it.copy(showBiometricPrompt = false) }
    }

    fun enableBiometric(enabled: Boolean) {
        viewModelScope.launch {
            authRepository.setBiometricEnabled(enabled)
            _uiState.update { it.copy(biometricEnabled = enabled) }
        }
    }

    // Вихід
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { AuthUiState() }
            _loginForm.update { LoginFormState() }
            _registerForm.update { RegisterFormState() }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Швидкий вхід з тестовим акаунтом
     */
    fun quickLogin(email: String, password: String) {
        _loginForm.update { it.copy(email = email, password = password) }
        login()
    }
}
