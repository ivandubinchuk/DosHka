package com.example.doshka.ui.screens.task

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.doshka.data.remote.dto.AttachmentDto
import com.example.doshka.domain.model.Label
import com.example.doshka.domain.model.Task
import com.example.doshka.domain.model.TaskPriority
import com.example.doshka.domain.model.User
import com.example.doshka.ui.components.BrutalAvatar
import com.example.doshka.ui.components.BrutalCard
import com.example.doshka.ui.components.BrutalOutlinedButton
import com.example.doshka.ui.components.BrutalTextButton
import com.example.doshka.ui.components.PriorityStamp
import com.example.doshka.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit = {},
    viewModel: TaskDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Повертаємося якщо задачу видалено
    LaunchedEffect(uiState.taskDeleted) {
        if (uiState.taskDeleted) {
            onNavigateBack()
        }
    }

    // Функція для поширення задачі
    fun shareTask(task: Task) {
        val shareText = buildString {
            append("📋 ${task.title}\n\n")
            if (!task.description.isNullOrBlank()) {
                append("${task.description}\n\n")
            }
            append("⚡ Пріоритет: ${task.priority.name}\n")
            task.deadline?.let {
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ZoneId.systemDefault())
                append("📅 Дедлайн: ${formatter.format(it)}\n")
            }
            task.estimatedHours?.let {
                append("⏱ Оцінка: $it год\n")
            }
            if (task.tags.isNotEmpty()) {
                append("\n🏷 Теги: ${task.tags.joinToString(", ") { "#$it" }}\n")
            }
            append("\n— DOSHKA")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Поділитися задачею"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ЗАДАЧА",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    // Кнопка поширення
                    uiState.task?.let { task ->
                        IconButton(onClick = { shareTask(task) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Поділитися"
                            )
                        }
                    }

                    // Редагування (тільки для менеджера)
                    if (uiState.isManager) {
                        IconButton(onClick = { onNavigateToEdit(taskId) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Редагувати"
                            )
                        }
                    }

                    // Кнопка чату
                    IconButton(onClick = { onNavigateToChat(taskId) }) {
                        BadgedBox(
                            badge = {
                                // Показуємо тільки непрочитані повідомлення
                                if (uiState.unreadMessagesCount > 0) {
                                    Badge(
                                        containerColor = BrutalOrange
                                    ) {
                                        Text(
                                            text = uiState.unreadMessagesCount.toString(),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Чат"
                            )
                        }
                    }

                    // Видалення (тільки для менеджера)
                    if (uiState.isManager) {
                        IconButton(onClick = viewModel::showDeleteConfirm) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Видалити",
                                tint = BrutalRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = BrutalRed,
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text(uiState.error!!)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = BrutalOrange
                    )
                }

                uiState.task == null -> {
                    Text(
                        text = "Задачу не знайдено",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                else -> {
                    TaskDetailsContent(
                        task = uiState.task!!,
                        assignee = uiState.assignee,
                        creator = uiState.creator,
                        messagesCount = uiState.messagesCount,
                        attachments = uiState.attachments,
                        isUploading = uiState.isUploading,
                        isManager = uiState.isManager,
                        onPriorityChange = viewModel::updatePriority,
                        onOpenChat = { onNavigateToChat(taskId) },
                        onUploadAttachment = { uri -> viewModel.uploadAttachment(context, uri) },
                        onDeleteAttachment = viewModel::deleteAttachment
                    )
                }
            }

            // Індикатор збереження
            if (uiState.isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrutalOrange)
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
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Цю дію неможливо скасувати.")
            },
            confirmButton = {
                BrutalTextButton(
                    text = "ВИДАЛИТИ",
                    onClick = viewModel::deleteTask,
                    backgroundColor = BrutalRed
                )
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteConfirm) {
                    Text("СКАСУВАТИ")
                }
            }
        )
    }
}

@Composable
private fun TaskDetailsContent(
    task: Task,
    assignee: User?,
    creator: User?,
    messagesCount: Int,
    attachments: List<AttachmentDto>,
    isUploading: Boolean,
    isManager: Boolean,
    onPriorityChange: (TaskPriority) -> Unit,
    onOpenChat: () -> Unit,
    onUploadAttachment: (Uri) -> Unit,
    onDeleteAttachment: (String) -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    // Лаунчер для вибору файлу
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onUploadAttachment(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок та пріоритет
        BrutalCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    PriorityStamp(priority = task.priority.toString())
                }

                if (!task.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Мітки (labels)
                if (task.labels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        task.labels.forEach { label ->
                            LabelChip(label = label)
                        }
                    }
                }

                // Теги
                if (task.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        task.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Кнопка чату
        BrutalCard(
            onClick = onOpenChat,
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BrutalOrange.copy(alpha = 0.1f),
            borderColor = BrutalOrange
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = BrutalOrange
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "ВІДКРИТИ ЧАТ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrutalOrange
                    )
                }

                if (messagesCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(BrutalOrange)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$messagesCount",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Виконавець
        BrutalCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "ВИКОНАВЕЦЬ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (assignee != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrutalAvatar(name = assignee.fullName, size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = assignee.fullName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = assignee.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Не призначено",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Інформація
        BrutalCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Дедлайн
                task.deadline?.let { deadline ->
                    InfoRow(
                        icon = Icons.Default.Schedule,
                        label = "ДЕДЛАЙН",
                        value = dateFormatter.format(deadline),
                        isOverdue = deadline.isBefore(java.time.Instant.now())
                    )
                }

                // Оцінка часу
                task.estimatedHours?.let { hours ->
                    InfoRow(
                        icon = Icons.Default.Timer,
                        label = "ОЦІНКА",
                        value = "$hours год"
                    )
                }

                // Фактичний час
                task.actualHours?.let { hours ->
                    InfoRow(
                        icon = Icons.Default.HourglassEmpty,
                        label = "ВИТРАЧЕНО",
                        value = "$hours год"
                    )
                }

                // Створено
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "СТВОРЕНО",
                    value = dateFormatter.format(task.createdAt)
                )

                // Автор
                creator?.let {
                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "АВТОР",
                        value = it.fullName
                    )
                }
            }
        }

        // Вкладення
        BrutalCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ВКЛАДЕННЯ (${attachments.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Кнопка додавання файлу
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        enabled = !isUploading
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BrutalOrange
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Додати файл",
                                tint = BrutalOrange
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (attachments.isEmpty()) {
                    Text(
                        text = "Немає вкладень",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    attachments.forEach { attachment ->
                        AttachmentItem(
                            attachment = attachment,
                            onDelete = { onDeleteAttachment(attachment.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Кнопка для додавання
                BrutalOutlinedButton(
                    text = "+ ДОДАТИ ФАЙЛ",
                    onClick = { filePickerLauncher.launch("*/*") },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Зміна пріоритету (тільки для менеджера)
        if (isManager) {
            BrutalCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ЗМІНИТИ ПРІОРИТЕТ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskPriority.entries.forEach { priority ->
                            PriorityButton(
                                priority = priority,
                                isSelected = task.priority == priority,
                                onClick = { onPriorityChange(priority) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isOverdue: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isOverdue) BrutalRed else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isOverdue) BrutalRed else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
        )

        if (isOverdue) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(BrutalRed)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ПРОСТРОЧЕНО",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PriorityButton(
    priority: TaskPriority,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when (priority) {
        TaskPriority.CRITICAL -> PriorityCritical
        TaskPriority.HIGH -> PriorityHigh
        TaskPriority.MEDIUM -> PriorityMedium
        TaskPriority.LOW -> PriorityLow
    }

    Box(
        modifier = modifier
            .background(if (isSelected) color else Color.Transparent)
            .border(2.dp, color)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = priority.name.take(3),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else color
        )
    }
}

/**
 * Компонент для відображення вкладення
 */
@Composable
private fun AttachmentItem(
    attachment: AttachmentDto,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .clickable {
                // Відкриваємо файл у браузері
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Ігноруємо помилку
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Іконка типу файлу
        val icon = when {
            attachment.fileType.startsWith("image/") -> Icons.Default.Image
            attachment.fileType.startsWith("video/") -> Icons.Default.VideoFile
            attachment.fileType.startsWith("audio/") -> Icons.Default.AudioFile
            attachment.fileType == "application/pdf" -> Icons.Default.PictureAsPdf
            else -> Icons.AutoMirrored.Filled.InsertDriveFile
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrutalOrange,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatFileSize(attachment.fileSize),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Кнопка видалення
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Видалити",
                tint = BrutalRed
            )
        }
    }
}

/**
 * Форматує розмір файлу
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

/**
 * Компонент для відображення кольорової мітки
 */
@Composable
private fun LabelChip(label: Label) {
    val labelColor = try {
        Color(label.color.toColorInt())
    } catch (e: Exception) {
        BrutalOrange
    }

    Box(
        modifier = Modifier
            .background(labelColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .border(1.dp, labelColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(labelColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = labelColor
            )
        }
    }
}
