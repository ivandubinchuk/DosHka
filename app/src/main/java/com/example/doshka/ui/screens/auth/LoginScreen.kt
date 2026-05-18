package com.example.doshka.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.doshka.ui.components.*
import com.example.doshka.ui.screens.settings.ConnectionStatus
import com.example.doshka.ui.screens.settings.SettingsViewModel
import com.example.doshka.ui.theme.*
import com.example.doshka.util.BiometricHelper
import com.example.doshka.util.DeviceUtils

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loginForm by viewModel.loginForm.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showPassword by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }

    // Біометричний хелпер
    val biometricHelper = remember { BiometricHelper(context) }
    val isBiometricAvailable = remember { biometricHelper.isBiometricAvailable() }

    // Функція для показу біометричного діалогу
    fun showBiometricPrompt() {
        val activity = context as? FragmentActivity
        if (activity == null) {
            Toast.makeText(context, "Біометрія недоступна", Toast.LENGTH_SHORT).show()
            return
        }

        biometricHelper.showBiometricPrompt(
            activity = activity,
            title = "Вхід у DoshKa",
            subtitle = "Використовуйте відбиток пальця",
            negativeButtonText = "Скасувати",
            onSuccess = {
                // Після успішної біометрії - оновлюємо токен
                viewModel.authenticateWithBiometric()
            },
            onError = { _, errString ->
                Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
            },
            onFailed = {
                Toast.makeText(context, "Відбиток не розпізнано", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Автоматично показуємо діалог якщо сервер не знайдено
    LaunchedEffect(settingsState.isServerConfigured, settingsState.isDiscovering) {
        if (!settingsState.isServerConfigured && !settingsState.isDiscovering && !DeviceUtils.isEmulator()) {
            showServerDialog = true
        }
    }

    // Автоматично показуємо діалог при помилці з'єднання
    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (error != null && (error.contains("connect", ignoreCase = true) ||
                    error.contains("connection", ignoreCase = true) ||
                    error.contains("127.0.0.1", ignoreCase = true) ||
                    error.contains("failed", ignoreCase = true))) {
            showServerDialog = true
        }
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    // Діалог налаштування сервера
    if (showServerDialog) {
        ServerSettingsDialog(
            serverUrl = settingsState.serverUrl,
            connectionStatus = settingsState.connectionStatus,
            isTestingConnection = settingsState.isTestingConnection,
            isDiscovering = settingsState.isDiscovering,
            onUrlChange = settingsViewModel::updateServerUrl,
            onTestConnection = settingsViewModel::testConnection,
            onDiscover = settingsViewModel::rediscoverServer,
            onSave = {
                settingsViewModel.saveServerUrl()
                showServerDialog = false
            },
            onDismiss = { showServerDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Статус пошуку сервера
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (settingsState.isDiscovering) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = BrutalOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Пошук сервера...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (settingsState.connectionStatus == ConnectionStatus.Success) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrutalGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Сервер знайдено",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrutalGreen
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            IconButton(onClick = { showServerDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Налаштування сервера",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Логотип та назва
        Text(
            text = "DOSHKA",
            style = MaterialTheme.typography.displayLarge,
            color = BrutalOrange,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "КАНБАН-ДОШКА В КИШЕНІ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Заголовок форми
        BrutalScreenTitle(
            title = "ВХІД",
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Форма входу
        BrutalTextField(
            value = loginForm.email,
            onValueChange = viewModel::updateLoginEmail,
            label = "EMAIL",
            placeholder = "введіть email...",
            isError = loginForm.emailError != null,
            errorMessage = loginForm.emailError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        BrutalTextField(
            value = loginForm.password,
            onValueChange = viewModel::updateLoginPassword,
            label = "ПАРОЛЬ",
            placeholder = "введіть пароль...",
            isError = loginForm.passwordError != null,
            errorMessage = loginForm.passwordError,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showPassword) "Приховати пароль" else "Показати пароль"
                    )
                }
            }
        )

        // Помилка
        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            BrutalCard(
                backgroundColor = BrutalRed.copy(alpha = 0.1f),
                borderColor = BrutalRed
            ) {
                Text(
                    text = uiState.error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrutalRed,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка входу
        BrutalTextButton(
            text = if (uiState.isLoading) "ВХОДИМО..." else "УВІЙТИ",
            onClick = viewModel::login,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        // Біометрія - показуємо якщо є збережені дані і біометрія доступна
        if (uiState.biometricEnabled && isBiometricAvailable) {
            Spacer(modifier = Modifier.height(16.dp))

            BrutalOutlinedButton(
                text = "ВХІД ПО ВІДБИТКУ",
                onClick = { showBiometricPrompt() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Роздільник
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            Text(
                text = "  АБО  ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Швидкий вхід (тестові акаунти)
        Text(
            text = "ШВИДКИЙ ВХІД (ТЕСТ)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickLoginButton(
                label = "👔 МЕНЕДЖЕР",
                onClick = { viewModel.quickLogin("manager@test.com", "123456") },
                modifier = Modifier.weight(1f)
            )
            QuickLoginButton(
                label = "👷 ВИКОНАВЕЦЬ 1",
                onClick = { viewModel.quickLogin("dev1@test.com", "123456") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        QuickLoginButton(
            label = "👷 ВИКОНАВЕЦЬ 2",
            onClick = { viewModel.quickLogin("dev2@test.com", "123456") },
            modifier = Modifier.fillMaxWidth(0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Реєстрація
        Text(
            text = "НЕМАЄ АКАУНТУ?",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        BrutalOutlinedButton(
            text = "ЗАРЕЄСТРУВАТИСЯ",
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Слоган
        Text(
            text = "«Постав. Контролюй. Закрий.»",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    inviteToken: String? = null,
    viewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val registerForm by viewModel.registerForm.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    var showPassword by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }

    // Автоматично показуємо діалог на реальному пристрої, якщо не налаштовано
    LaunchedEffect(settingsState.isServerConfigured) {
        if (!settingsState.isServerConfigured && !DeviceUtils.isEmulator()) {
            showServerDialog = true
        }
    }

    // Автоматично показуємо діалог при помилці з'єднання
    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (error != null && (error.contains("connect", ignoreCase = true) ||
                    error.contains("connection", ignoreCase = true) ||
                    error.contains("127.0.0.1", ignoreCase = true) ||
                    error.contains("failed", ignoreCase = true))) {
            showServerDialog = true
        }
    }

    LaunchedEffect(inviteToken) {
        viewModel.setInviteToken(inviteToken)
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onRegisterSuccess()
        }
    }

    // Діалог налаштування сервера
    if (showServerDialog) {
        ServerSettingsDialog(
            serverUrl = settingsState.serverUrl,
            connectionStatus = settingsState.connectionStatus,
            isTestingConnection = settingsState.isTestingConnection,
            isDiscovering = settingsState.isDiscovering,
            onUrlChange = settingsViewModel::updateServerUrl,
            onTestConnection = settingsViewModel::testConnection,
            onDiscover = settingsViewModel::rediscoverServer,
            onSave = {
                settingsViewModel.saveServerUrl()
                showServerDialog = false
            },
            onDismiss = { showServerDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Кнопка налаштувань сервера
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { showServerDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Налаштування сервера",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Заголовок
        BrutalScreenTitle(
            title = if (inviteToken != null) "ПРИЄДНАТИСЯ" else "РЕЄСТРАЦІЯ",
            modifier = Modifier.align(Alignment.Start)
        )

        if (inviteToken != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Вас запросили до команди",
                style = MaterialTheme.typography.bodyMedium,
                color = BrutalGreen
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Форма реєстрації
        BrutalTextField(
            value = registerForm.fullName,
            onValueChange = viewModel::updateRegisterFullName,
            label = "ПОВНЕ ІМ'Я",
            placeholder = "Іван Петренко",
            isError = registerForm.fullNameError != null,
            errorMessage = registerForm.fullNameError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        BrutalTextField(
            value = registerForm.email,
            onValueChange = viewModel::updateRegisterEmail,
            label = "EMAIL",
            placeholder = "email@example.com",
            isError = registerForm.emailError != null,
            errorMessage = registerForm.emailError,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        BrutalTextField(
            value = registerForm.password,
            onValueChange = viewModel::updateRegisterPassword,
            label = "ПАРОЛЬ",
            placeholder = "мінімум 6 символів",
            isError = registerForm.passwordError != null,
            errorMessage = registerForm.passwordError,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        BrutalTextField(
            value = registerForm.confirmPassword,
            onValueChange = viewModel::updateRegisterConfirmPassword,
            label = "ПІДТВЕРДЖЕННЯ ПАРОЛЯ",
            placeholder = "повторіть пароль",
            isError = registerForm.confirmPasswordError != null,
            errorMessage = registerForm.confirmPasswordError,
            modifier = Modifier.fillMaxWidth()
        )

        // Вибір ролі (тільки якщо не за запрошенням)
        if (inviteToken == null) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "РОЛЬ",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoleOption(
                    title = "МЕНЕДЖЕР",
                    description = "Створюю задачі, керую командою",
                    isSelected = registerForm.role == "manager",
                    onClick = { viewModel.updateRegisterRole("manager") },
                    modifier = Modifier.weight(1f)
                )
                RoleOption(
                    title = "ВИКОНАВЕЦЬ",
                    description = "Виконую задачі, звітую",
                    isSelected = registerForm.role == "executor",
                    onClick = { viewModel.updateRegisterRole("executor") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Помилка
        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            BrutalCard(
                backgroundColor = BrutalRed.copy(alpha = 0.1f),
                borderColor = BrutalRed
            ) {
                Text(
                    text = uiState.error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrutalRed,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка реєстрації
        BrutalTextButton(
            text = if (uiState.isLoading) "РЕЄСТРУЄМО..." else "ЗАРЕЄСТРУВАТИСЯ",
            onClick = viewModel::register,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Назад до входу
        TextButton(onClick = onNavigateToLogin) {
            Text(
                text = "← НАЗАД ДО ВХОДУ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Кнопка швидкого входу для тестових акаунтів
 */
@Composable
private fun QuickLoginButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BrutalOrange
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(BrutalOrange.copy(alpha = 0.5f))
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun RoleOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BrutalCard(
        onClick = onClick,
        backgroundColor = if (isSelected) BrutalOrange.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
        borderColor = if (isSelected) BrutalOrange else MaterialTheme.colorScheme.outline,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) BrutalOrange else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ServerSettingsDialog(
    serverUrl: String,
    connectionStatus: ConnectionStatus,
    isTestingConnection: Boolean,
    isDiscovering: Boolean = false,
    onUrlChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onDiscover: () -> Unit = {},
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        BrutalCard {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "СЕРВЕР",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка автопошуку
                Button(
                    onClick = onDiscover,
                    enabled = !isDiscovering && !isTestingConnection,
                    colors = ButtonDefaults.buttonColors(containerColor = BrutalOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = BrutalSurfaceLight
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ПОШУК СЕРВЕРА...")
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ЗНАЙТИ АВТОМАТИЧНО")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "або вкажіть вручну:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onUrlChange,
                    label = { Text("URL сервера") },
                    placeholder = { Text("http://192.168.1.100:8000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = BrutalOrange
                        )
                    },
                    trailingIcon = {
                        when (connectionStatus) {
                            ConnectionStatus.Success -> Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Підключено",
                                tint = BrutalGreen
                            )
                            ConnectionStatus.Failed -> Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Помилка",
                                tint = BrutalRed
                            )
                            ConnectionStatus.Testing -> CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            else -> {}
                        }
                    }
                )

                when (connectionStatus) {
                    ConnectionStatus.Success -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Сервер знайдено!",
                            color = BrutalGreen,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    ConnectionStatus.Failed -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✗ Сервер не знайдено. Перевірте:\n• Сервер запущено\n• Телефон і комп'ютер в одній WiFi мережі",
                            color = BrutalRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTestConnection,
                        enabled = !isTestingConnection && !isDiscovering && serverUrl.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ПЕРЕВІРИТИ")
                    }

                    Button(
                        onClick = onSave,
                        enabled = connectionStatus == ConnectionStatus.Success,
                        colors = ButtonDefaults.buttonColors(containerColor = BrutalGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ЗБЕРЕГТИ")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("СКАСУВАТИ")
                }
            }
        }
    }
}
