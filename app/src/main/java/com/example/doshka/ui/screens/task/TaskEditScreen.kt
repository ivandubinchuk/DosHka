package com.example.doshka.ui.screens.task

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.doshka.domain.model.TaskPriority
import com.example.doshka.domain.model.User
import com.example.doshka.ui.components.*
import com.example.doshka.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Екран редагування задачі
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: TaskEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()

    // Завантажуємо задачу при створенні
    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    // Повертаємось якщо задачу видалено
    LaunchedEffect(uiState.taskDeleted) {
        if (uiState.taskDeleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isLoading) "ЗАВАНТАЖЕННЯ..." else "РЕДАГУВАННЯ",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Кнопка чату
                    IconButton(onClick = { onNavigateToChat(taskId) }) {
                        BadgedBox(
                            badge = {
                                if (uiState.messagesCount > 0) {
                                    Badge { Text("${uiState.messagesCount}") }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Чат")
                        }
                    }
                    // Кнопка видалення
                    IconButton(onClick = viewModel::showDeleteConfirm) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Видалити",
                            tint = BrutalRed
                        )
                    }
                }
            )
        },
        snackbarHost = {
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = BrutalRed,
                    contentColor = BrutalSurfaceLight,
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("OK", color = BrutalSurfaceLight)
                        }
                    }
                ) {
                    Text(uiState.error!!)
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrutalOrange)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Назва задачі
                item {
                    BrutalTextField(
                        value = formState.title,
                        onValueChange = viewModel::updateTitle,
                        label = "НАЗВА ЗАДАЧІ",
                        placeholder = "Що потрібно зробити?"
                    )
                }

                // Опис
                item {
                    BrutalTextField(
                        value = formState.description,
                        onValueChange = viewModel::updateDescription,
                        label = "ОПИС",
                        placeholder = "Детальний опис задачі...",
                        singleLine = false,
                        maxLines = 6
                    )
                }

                // Пріоритет
                item {
                    Text(
                        text = "ПРІОРИТЕТ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskPriority.entries.forEach { priority ->
                            val isSelected = formState.priority == priority
                            val color = when (priority) {
                                TaskPriority.CRITICAL -> PriorityCritical
                                TaskPriority.HIGH -> PriorityHigh
                                TaskPriority.MEDIUM -> PriorityMedium
                                TaskPriority.LOW -> PriorityLow
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updatePriority(priority) },
                                label = {
                                    Text(
                                        text = when (priority) {
                                            TaskPriority.CRITICAL -> "КРИТИЧНИЙ"
                                            TaskPriority.HIGH -> "ВИСОКИЙ"
                                            TaskPriority.MEDIUM -> "СЕРЕДНІЙ"
                                            TaskPriority.LOW -> "НИЗЬКИЙ"
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color,
                                    selectedLabelColor = BrutalSurfaceLight
                                )
                            )
                        }
                    }
                }

                // Виконавець
                item {
                    Text(
                        text = "ВИКОНАВЕЦЬ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AssigneeSelector(
                        selectedUserId = formState.assigneeId,
                        teamMembers = teamMembers,
                        onSelectUser = viewModel::updateAssignee
                    )
                }

                // Переміщення між колонками
                item {
                    Text(
                        text = "КОЛОНКА",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ColumnSelector(
                        columns = uiState.columns,
                        selectedColumnId = formState.columnId,
                        onSelectColumn = viewModel::moveToColumn
                    )
                }

                // Дедлайн
                item {
                    DeadlinePicker(
                        deadline = formState.deadline,
                        onDeadlineChange = viewModel::updateDeadline
                    )
                }

                // Кнопка збереження
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    BrutalTextButton(
                        text = if (uiState.isSaving) "ЗБЕРЕЖЕННЯ..." else "ЗБЕРЕГТИ ЗМІНИ",
                        onClick = viewModel::saveChanges,
                        enabled = !uiState.isSaving && formState.hasChanges,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Діалог підтвердження видалення
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteConfirm,
            title = {
                Text(
                    text = "ВИДАЛИТИ ЗАДАЧУ?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Цю дію неможливо скасувати. Задачу буде видалено назавжди.")
            },
            confirmButton = {
                BrutalTextButton(
                    text = "ВИДАЛИТИ",
                    onClick = viewModel::deleteTask
                )
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirm) {
                    Text("СКАСУВАТИ")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Селектор виконавця
 */
@Composable
private fun AssigneeSelector(
    selectedUserId: String?,
    teamMembers: List<User>,
    onSelectUser: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedUser = teamMembers.find { it.id == selectedUserId }

    Column {
        // Поточний вибір
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.outline)
                .clickable { expanded = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (selectedUser != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrutalAvatar(name = selectedUser.fullName, size = 32.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedUser.fullName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Text(
                    text = "Не призначено",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        // Список для вибору
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            // Опція "Не призначено"
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Не призначено",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                onClick = {
                    onSelectUser(null)
                    expanded = false
                },
                leadingIcon = {
                    Icon(Icons.Default.PersonOff, contentDescription = null)
                }
            )

            HorizontalDivider()

            // Список учасників команди
            teamMembers.forEach { user ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrutalAvatar(name = user.fullName, size = 28.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = user.fullName)
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelectUser(user.id)
                        expanded = false
                    },
                    trailingIcon = {
                        if (user.id == selectedUserId) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = BrutalOrange
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * Селектор колонки
 */
@Composable
private fun ColumnSelector(
    columns: List<com.example.doshka.domain.model.Column>,
    selectedColumnId: String?,
    onSelectColumn: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        columns.forEach { column ->
            val isSelected = column.id == selectedColumnId
            FilterChip(
                selected = isSelected,
                onClick = { onSelectColumn(column.id) },
                label = {
                    Text(
                        text = column.name.uppercase(),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrutalOrange,
                    selectedLabelColor = BrutalSurfaceLight
                )
            )
        }
    }
}

/**
 * Вибір дедлайну
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlinePicker(
    deadline: Instant?,
    onDeadlineChange: (Instant?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = deadline?.toEpochMilli()
    )

    Column {
        Text(
            text = "ДЕДЛАЙН",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.outline)
                .clickable { showDatePicker = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = if (deadline != null) BrutalOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                if (deadline != null) {
                    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                        .withZone(ZoneId.systemDefault())
                    Text(
                        text = formatter.format(deadline),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        text = "Без дедлайну",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (deadline != null) {
                IconButton(onClick = { onDeadlineChange(null) }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Очистити",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        // Створюємо контекст з англійською локаллю
        val context = LocalContext.current
        val englishContext = remember {
            val config = Configuration(context.resources.configuration)
            config.setLocale(Locale.ENGLISH)
            context.createConfigurationContext(config)
        }
        CompositionLocalProvider(LocalContext provides englishContext) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                onDeadlineChange(Instant.ofEpochMilli(it))
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("СКАСУВАТИ")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
