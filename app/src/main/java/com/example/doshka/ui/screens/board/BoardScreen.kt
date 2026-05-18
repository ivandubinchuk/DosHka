package com.example.doshka.ui.screens.board

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.doshka.domain.model.Task
import com.example.doshka.domain.model.Column
import com.example.doshka.ui.components.*
import com.example.doshka.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToTaskDetails: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: BoardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            val isManagerTopBar = uiState.currentUser?.role == com.example.doshka.domain.model.UserRole.MANAGER
            BoardTopBar(
                boardName = uiState.board?.name ?: "DOSHKA",
                isOffline = uiState.isOffline,
                isSyncing = uiState.isSyncing,
                isManager = isManagerTopBar,
                onSync = viewModel::syncBoard,
                onSettings = onNavigateToSettings,
                onDashboard = onNavigateToDashboard,
                onLogout = {
                    viewModel.logout()
                    onLogout()
                }
            )
        },
        floatingActionButton = {
            // FAB для швидкого створення задачі (тільки для менеджерів)
            val isManager = uiState.currentUser?.role == com.example.doshka.domain.model.UserRole.MANAGER
            if (uiState.columns.isNotEmpty() && isManager) {
                FloatingActionButton(
                    onClick = {
                        // Беремо першу колонку для створення задачі
                        uiState.columns.firstOrNull()?.column?.id?.let { columnId ->
                            viewModel.showCreateTaskDialog(columnId)
                        }
                    },
                    containerColor = BrutalOrange,
                    contentColor = BrutalSurfaceLight
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Нова задача"
                    )
                }
            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Офлайн банер
            if (uiState.isOffline && uiState.offlineSince != null) {
                OfflineBanner(
                    timeSinceOffline = System.currentTimeMillis() - uiState.offlineSince!!
                )
            }

            // Основний контент
            val isManagerContent = uiState.currentUser?.role == com.example.doshka.domain.model.UserRole.MANAGER
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                !uiState.hasTeam -> {
                    // Немає команди - тільки менеджер може створити
                    if (isManagerContent) {
                        NoTeamState(
                            onCreateTeam = viewModel::showCreateTeamDialog
                        )
                    } else {
                        ExecutorNoTeamState()
                    }
                }
                !uiState.hasBoard -> {
                    // Немає дошки - тільки менеджер може створити
                    if (isManagerContent) {
                        NoBoardState(
                            onCreateBoard = viewModel::showCreateBoardDialog
                        )
                    } else {
                        ExecutorNoBoardState()
                    }
                }
                uiState.columns.isEmpty() -> {
                    // Колонки створюються автоматично при створенні дошки
                    LoadingState()
                }
                else -> {
                    val isManagerUser = uiState.currentUser?.role == com.example.doshka.domain.model.UserRole.MANAGER
                    KanbanBoard(
                        columns = uiState.columns,
                        onTaskClick = { task -> onNavigateToTaskDetails(task.id) },
                        onTaskLongClick = viewModel::selectTask,
                        onAddTask = if (isManagerUser) viewModel::showCreateTaskDialog else null,
                        onMoveTaskToNextColumn = if (isManagerUser) { task, nextColumnId ->
                            viewModel.moveTask(task.id, nextColumnId, 0)
                        } else null
                    )
                }
            }
        }
    }

    // Діалог переміщення задачі (при long press) - тільки для менеджерів
    val isManager = uiState.currentUser?.role == com.example.doshka.domain.model.UserRole.MANAGER
    if (uiState.showTaskDetails && uiState.selectedTask != null) {
        if (isManager) {
            MoveTaskDialog(
                task = uiState.selectedTask!!,
                columns = uiState.columns.map { it.column },
                onDismiss = viewModel::hideTaskDetails,
                onMoveToColumn = { columnId ->
                    viewModel.moveTask(uiState.selectedTask!!.id, columnId, 0)
                    viewModel.hideTaskDetails()
                },
                onDelete = { viewModel.deleteTask(uiState.selectedTask!!.id) },
                onOpenDetails = {
                    viewModel.hideTaskDetails()
                    onNavigateToTaskDetails(uiState.selectedTask!!.id)
                }
            )
        } else {
            // Виконавець бачить тільки кнопку переходу в чат
            ExecutorTaskDialog(
                task = uiState.selectedTask!!,
                onDismiss = viewModel::hideTaskDetails,
                onOpenChat = {
                    viewModel.hideTaskDetails()
                    onNavigateToTaskDetails(uiState.selectedTask!!.id)
                }
            )
        }
    }

    // Діалог створення задачі
    if (uiState.showCreateTask) {
        CreateTaskDialog(
            viewModel = viewModel,
            onDismiss = viewModel::hideCreateTaskDialog
        )
    }

    // Діалог створення команди
    if (uiState.showCreateTeamDialog) {
        CreateTeamDialog(
            viewModel = viewModel,
            onDismiss = viewModel::hideCreateTeamDialog
        )
    }

    // Діалог створення дошки
    if (uiState.showCreateBoardDialog) {
        CreateBoardDialog(
            viewModel = viewModel,
            onDismiss = viewModel::hideCreateBoardDialog
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoardTopBar(
    boardName: String,
    isOffline: Boolean,
    isSyncing: Boolean,
    isManager: Boolean,
    onSync: () -> Unit,
    onSettings: () -> Unit,
    onDashboard: () -> Unit,
    onLogout: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = boardName.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isOffline) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Офлайн",
                        tint = BrutalAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        actions = {
            // Синхронізація
            IconButton(
                onClick = onSync,
                enabled = !isSyncing
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Синхронізувати"
                    )
                }
            }

            // Дашборд (тільки для менеджерів)
            if (isManager) {
                IconButton(onClick = onDashboard) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = "Дашборд"
                    )
                }
            }

            // Меню
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню"
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Налаштування") },
                        onClick = {
                            showMenu = false
                            onSettings()
                        },
                        leadingIcon = { Icon(Icons.Default.Settings, null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Вийти", color = BrutalRed) },
                        onClick = {
                            showMenu = false
                            onLogout()
                        },
                        leadingIcon = { Icon(Icons.Default.Logout, null, tint = BrutalRed) }
                    )
                }
            }

        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = BrutalOrange,
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ЗАВАНТАЖЕННЯ...",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun NoTeamState(
    onCreateTeam: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = BrutalOrange
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "НЕМАЄ КОМАНДИ",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Створіть команду, щоб почати роботу з Канбан-дошкою",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            BrutalTextButton(
                text = "СТВОРИТИ КОМАНДУ",
                onClick = onCreateTeam
            )
        }
    }
}

@Composable
private fun NoBoardState(
    onCreateBoard: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = BrutalOrange
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "НЕМАЄ ДОШКИ",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Створіть першу Канбан-дошку для вашої команди",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            BrutalTextButton(
                text = "СТВОРИТИ ДОШКУ",
                onClick = onCreateBoard
            )
        }
    }
}

@Composable
private fun ExecutorNoTeamState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ОЧІКУЙТЕ ЗАПРОШЕННЯ",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Менеджер повинен додати вас до команди",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ExecutorNoBoardState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Dashboard,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ДОШКА НЕ СТВОРЕНА",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Менеджер ще не створив дошку",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyBoardState(
    onCreateColumn: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ViewColumn,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = BrutalOrange
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ДОШКА ПОРОЖНЯ",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Створіть першу колонку, щоб почати роботу",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            BrutalTextButton(
                text = "СТВОРИТИ КОЛОНКУ",
                onClick = onCreateColumn
            )
        }
    }
}

@Composable
private fun KanbanBoard(
    columns: List<ColumnWithTasks>,
    onTaskClick: (Task) -> Unit,
    onTaskLongClick: (Task) -> Unit,
    onAddTask: ((String) -> Unit)?,
    onMoveTaskToNextColumn: ((Task, String) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        columns.forEachIndexed { index, columnWithTasks ->
            val isLastColumn = index == columns.lastIndex
            val nextColumnId = if (!isLastColumn) columns[index + 1].column.id else null

            KanbanColumn(
                column = columnWithTasks.column,
                tasks = columnWithTasks.tasks,
                onTaskClick = onTaskClick,
                onTaskLongClick = onTaskLongClick,
                onAddTaskClick = onAddTask?.let { { it(columnWithTasks.column.id) } },
                onMoveTaskToNext = if (onMoveTaskToNextColumn != null && nextColumnId != null) {
                    { task -> onMoveTaskToNextColumn(task, nextColumnId) }
                } else null,
                isLastColumn = isLastColumn
            )
        }
    }
}

/**
 * Діалог переміщення задачі між колонками
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveTaskDialog(
    task: Task,
    columns: List<Column>,
    onDismiss: () -> Unit,
    onMoveToColumn: (String) -> Unit,
    onDelete: () -> Unit,
    onOpenDetails: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                PriorityStamp(priority = task.priority.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Опис (якщо є)
            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Секція переміщення
            Text(
                text = "ПЕРЕМІСТИТИ В КОЛОНКУ",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопки колонок
            columns.forEach { column ->
                val isCurrentColumn = column.id == task.columnId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrentColumn) BrutalOrange.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable(enabled = !isCurrentColumn) { onMoveToColumn(column.id) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = column.name.uppercase(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrentColumn) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrentColumn) BrutalOrange else MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrentColumn) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = BrutalOrange
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки дій
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Відкрити деталі
                BrutalOutlinedButton(
                    text = "ДЕТАЛІ",
                    onClick = onOpenDetails,
                    modifier = Modifier.weight(1f)
                )
                // Видалити
                BrutalButton(
                    onClick = onDelete,
                    backgroundColor = BrutalRed,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "ВИДАЛИТИ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskDialog(
    viewModel: BoardViewModel,
    onDismiss: () -> Unit
) {
    val taskForm by viewModel.taskForm.collectAsStateWithLifecycle()
    val teamMembers by viewModel.teamMembers.collectAsStateWithLifecycle()
    var showAssigneeDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Завантажуємо учасників команди
    LaunchedEffect(Unit) {
        viewModel.loadTeamMembers()
    }

    // DatePicker стан
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = taskForm.deadline
    )

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
                            datePickerState.selectedDateMillis?.let { millis ->
                                viewModel.updateTaskDeadline(millis)
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("ОК")
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "НОВА ЗАДАЧА",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            androidx.compose.foundation.layout.Column {
                BrutalTextField(
                    value = taskForm.title,
                    onValueChange = viewModel::updateTaskTitle,
                    label = "НАЗВА",
                    placeholder = "Що потрібно зробити?",
                    isError = taskForm.titleError != null,
                    errorMessage = taskForm.titleError
                )

                Spacer(modifier = Modifier.height(16.dp))

                BrutalTextField(
                    value = taskForm.description,
                    onValueChange = viewModel::updateTaskDescription,
                    label = "ОПИС",
                    placeholder = "Деталі задачі...",
                    singleLine = false,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Пріоритет
                Text(
                    text = "ПРІОРИТЕТ",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.doshka.domain.model.TaskPriority.entries.forEach { priority ->
                        FilterChip(
                            selected = taskForm.priority == priority,
                            onClick = { viewModel.updateTaskPriority(priority) },
                            label = {
                                Text(
                                    text = when (priority) {
                                        com.example.doshka.domain.model.TaskPriority.CRITICAL -> "КРИТ"
                                        com.example.doshka.domain.model.TaskPriority.HIGH -> "ВИС"
                                        com.example.doshka.domain.model.TaskPriority.MEDIUM -> "СЕР"
                                        com.example.doshka.domain.model.TaskPriority.LOW -> "НИЗ"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (priority) {
                                    com.example.doshka.domain.model.TaskPriority.CRITICAL -> PriorityCritical
                                    com.example.doshka.domain.model.TaskPriority.HIGH -> PriorityHigh
                                    com.example.doshka.domain.model.TaskPriority.MEDIUM -> PriorityMedium
                                    com.example.doshka.domain.model.TaskPriority.LOW -> PriorityLow
                                }
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Дедлайн
                Text(
                    text = "ДЕДЛАЙН",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showDatePicker = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (taskForm.deadline != null) BrutalOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (taskForm.deadline != null) {
                                com.example.doshka.util.DateTimeUtils.formatShortDateFromMillis(taskForm.deadline)
                            } else {
                                "Без дедлайну"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (taskForm.deadline != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (taskForm.deadline != null) {
                        IconButton(
                            onClick = { viewModel.updateTaskDeadline(null) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Очистити",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Виконавець
                Text(
                    text = "ВИКОНАВЕЦЬ",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    val selectedAssignee = teamMembers.find { it.id == taskForm.assigneeId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showAssigneeDropdown = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (selectedAssignee != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BrutalAvatar(name = selectedAssignee.fullName, size = 24.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedAssignee.fullName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Text(
                                text = "Не призначено",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = showAssigneeDropdown,
                        onDismissRequest = { showAssigneeDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Не призначено") },
                            onClick = {
                                viewModel.updateTaskAssignee(null)
                                showAssigneeDropdown = false
                            }
                        )
                        teamMembers.forEach { user ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        BrutalAvatar(name = user.fullName, size = 24.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(user.fullName)
                                    }
                                },
                                onClick = {
                                    viewModel.updateTaskAssignee(user.id)
                                    showAssigneeDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            BrutalTextButton(
                text = "СТВОРИТИ",
                onClick = viewModel::createTask
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("СКАСУВАТИ")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun CreateTeamDialog(
    viewModel: BoardViewModel,
    onDismiss: () -> Unit
) {
    val teamName by viewModel.teamName.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "НОВА КОМАНДА",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            BrutalTextField(
                value = teamName,
                onValueChange = viewModel::updateTeamName,
                label = "НАЗВА КОМАНДИ",
                placeholder = "Моя команда"
            )
        },
        confirmButton = {
            BrutalTextButton(
                text = "СТВОРИТИ",
                onClick = viewModel::createTeam,
                enabled = teamName.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("СКАСУВАТИ")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun CreateBoardDialog(
    viewModel: BoardViewModel,
    onDismiss: () -> Unit
) {
    val boardName by viewModel.boardName.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "НОВА ДОШКА",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            BrutalTextField(
                value = boardName,
                onValueChange = viewModel::updateBoardName,
                label = "НАЗВА ДОШКИ",
                placeholder = "Мій проєкт"
            )
        },
        confirmButton = {
            BrutalTextButton(
                text = "СТВОРИТИ",
                onClick = viewModel::createBoard,
                enabled = boardName.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("СКАСУВАТИ")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun CreateColumnDialog(
    viewModel: BoardViewModel,
    onDismiss: () -> Unit
) {
    val columnName by viewModel.columnName.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "НОВА КОЛОНКА",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            BrutalTextField(
                value = columnName,
                onValueChange = viewModel::updateColumnName,
                label = "НАЗВА КОЛОНКИ",
                placeholder = "В роботі"
            )
        },
        confirmButton = {
            BrutalTextButton(
                text = "СТВОРИТИ",
                onClick = viewModel::createColumn,
                enabled = columnName.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("СКАСУВАТИ")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * Діалог для виконавця - тільки перегляд задачі та перехід в чат
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExecutorTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onOpenChat: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                PriorityStamp(priority = task.priority.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Опис (якщо є)
            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Дедлайн
            if (task.deadline != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = BrutalOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Дедлайн: ${com.example.doshka.util.DateTimeUtils.formatShortDate(task.deadline)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка переходу в чат
            BrutalTextButton(
                text = "ВІДКРИТИ ЧАТ",
                onClick = onOpenChat,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
