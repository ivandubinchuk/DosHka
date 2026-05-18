package com.example.doshka.ui.screens.settings

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.doshka.ui.components.BrutalCard
import com.example.doshka.ui.components.BrutalTextButton
import com.example.doshka.ui.screens.auth.AuthViewModel
import com.example.doshka.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "НАЛАШТУВАННЯ",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Сервер
            BrutalCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "СЕРВЕР",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = settingsState.serverUrl,
                        onValueChange = { settingsViewModel.updateServerUrl(it) },
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
                            when (settingsState.connectionStatus) {
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { settingsViewModel.testConnection() },
                            enabled = !settingsState.isTestingConnection,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ПЕРЕВІРИТИ")
                        }

                        Button(
                            onClick = { settingsViewModel.saveServerUrl() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrutalOrange),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ЗБЕРЕГТИ")
                        }
                    }

                    when (settingsState.connectionStatus) {
                        ConnectionStatus.Success -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✓ З'єднання встановлено",
                                color = BrutalGreen,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        ConnectionStatus.Failed -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✗ Не вдалося підключитись",
                                color = BrutalRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Для емулятора: http://10.0.2.2:8000\nДля реального пристрою: http://<IP комп'ютера>:8000",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Профіль
            BrutalCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ПРОФІЛЬ",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    uiState.user?.let { user ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Аватар
                            com.example.doshka.ui.components.BrutalAvatar(
                                name = user.fullName,
                                size = 64.dp
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = user.fullName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (user.role.name.lowercase() == "manager") "МЕНЕДЖЕР" else "ВИКОНАВЕЦЬ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrutalOrange
                                )
                            }
                        }
                    }
                }
            }

            // Команда (тільки для менеджерів)
            if (uiState.user?.role?.name?.lowercase() == "manager") {
                BrutalCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "КОМАНДА",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { settingsViewModel.generateInviteQR() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrutalOrange),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !settingsState.isGeneratingQR
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (settingsState.isGeneratingQR) "ГЕНЕРАЦІЯ..." else "QR-ЗАПРОШЕННЯ"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Покажіть QR-код новому члену команди для швидкої реєстрації",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // QR-код діалог
            if (settingsState.showQRDialog && settingsState.qrCodeBase64 != null) {
                QRCodeDialog(
                    qrCodeBase64 = settingsState.qrCodeBase64!!,
                    expiresAt = settingsState.qrExpiresAt,
                    onDismiss = { settingsViewModel.dismissQRDialog() }
                )
            }

            // Безпека
            BrutalCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "БЕЗПЕКА",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Біометрія
                    SettingsSwitch(
                        icon = Icons.Default.Fingerprint,
                        title = "ВХІД ПО ВІДБИТКУ",
                        description = "Швидкий вхід без пароля",
                        checked = uiState.biometricEnabled,
                        onCheckedChange = { authViewModel.enableBiometric(it) }
                    )
                }
            }

            // Оформлення
            BrutalCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ОФОРМЛЕННЯ",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSwitch(
                        icon = Icons.Default.DarkMode,
                        title = "ТЕМНА ТЕМА",
                        description = "Вугілля замість бетону",
                        checked = settingsState.darkTheme,
                        onCheckedChange = { settingsViewModel.setDarkTheme(it) }
                    )
                }
            }

            // Сповіщення
            BrutalCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "СПОВІЩЕННЯ",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSwitch(
                        icon = Icons.Default.Notifications,
                        title = "PUSH-СПОВІЩЕННЯ",
                        description = "Нові задачі, повідомлення, дедлайни",
                        checked = settingsState.notificationsEnabled,
                        onCheckedChange = { settingsViewModel.setNotificationsEnabled(it) }
                    )
                }
            }

            // Про додаток
            BrutalCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ПРО ДОДАТОК",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "DOSHKA",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrutalOrange
                    )
                    Text(
                        text = "Версія 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "«Постав. Контролюй. Закрий.»",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка виходу
            BrutalTextButton(
                text = "ВИЙТИ",
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                backgroundColor = BrutalRed,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrutalOrange,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BrutalSurfaceLight,
                checkedTrackColor = BrutalOrange,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun QRCodeDialog(
    qrCodeBase64: String,
    expiresAt: String?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        BrutalCard {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "QR-ЗАПРОШЕННЯ",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Декодуємо та показуємо QR-код
                val bitmap = remember(qrCodeBase64) {
                    try {
                        val decodedBytes = Base64.decode(qrCodeBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR-код запрошення",
                        modifier = Modifier
                            .size(200.dp)
                            .border(2.dp, BrutalBorderLight)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .border(2.dp, BrutalRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Помилка завантаження",
                            color = BrutalRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Покажіть цей код новому члену команди",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (expiresAt != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Дійсний 24 години",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrutalAmber
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                BrutalTextButton(
                    text = "ЗАКРИТИ",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
