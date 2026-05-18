package com.example.doshka.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.doshka.domain.model.Task
import com.example.doshka.domain.model.TaskPriority
import com.example.doshka.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Картка задачі в брутальному стилі
 */
@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    onMoveToNext: (() -> Unit)? = null,
    isLastColumn: Boolean = false
) {
    val priorityColor = when (task.priority) {
        TaskPriority.CRITICAL -> PriorityCritical
        TaskPriority.HIGH -> PriorityHigh
        TaskPriority.MEDIUM -> PriorityMedium
        TaskPriority.LOW -> PriorityLow
    }

    // Анімація пульсації для критичного пріоритету
    val infiniteTransition = rememberInfiniteTransition(label = "critical_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (task.priority == TaskPriority.CRITICAL) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    // Масштаб при перетягуванні
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "drag_scale"
    )

    val shadowOffset = if (isDragging) 8.dp else 4.dp

    Box(
        modifier = modifier
            .scale(scale)
            .drawBehind {
                // Жорстка тінь
                translate(
                    left = shadowOffset.toPx(),
                    top = shadowOffset.toPx()
                ) {
                    drawRect(
                        color = BrutalShadowLight,
                        size = size
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha))
                .clickable(onClick = onClick)
        ) {
            // Вертикальна смуга пріоритету
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(priorityColor.copy(alpha = borderAlpha))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                // Верхній рядок: ID, статус синхронізації та пріоритет
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "■ ${task.id.take(8).uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Індикатор несинхронізованої задачі
                        if (!task.isSynced) {
                            Spacer(modifier = Modifier.width(4.dp))
                            TaskSyncIndicator(isSynced = false)
                        }
                    }

                    PriorityStamp(priority = task.priority.toString())
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Заголовок задачі
                Text(
                    text = task.title.uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Виконавець та дедлайн
                if (task.assignee != null || task.deadline != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Аватар виконавця
                        task.assignee?.let { assignee ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                BrutalAvatar(
                                    name = assignee.fullName,
                                    size = 28.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = assignee.fullName,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Дедлайн
                        task.deadline?.let { deadline ->
                            DeadlineChip(
                                deadline = deadline,
                                isOverdue = task.isOverdue
                            )
                        }
                    }
                }

                // Нижній рядок: коментарі, вкладення, теги
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Коментарі
                    if (task.commentsCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${task.commentsCount}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Вкладення
                    if (task.attachmentsCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${task.attachmentsCount}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Теги
                    if (task.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            task.tags.take(2).forEach { tag ->
                                TagChip(tag = tag)
                            }
                            if (task.tags.size > 2) {
                                Text(
                                    text = "+${task.tags.size - 2}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Кнопка переміщення на наступний етап
                    if (onMoveToNext != null && !isLastColumn) {
                        IconButton(
                            onClick = onMoveToNext,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Наступний етап",
                                tint = BrutalOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Чіп дедлайну
 */
@Composable
fun DeadlineChip(
    deadline: Instant,
    isOverdue: Boolean,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM")
        .withZone(ZoneId.systemDefault())
    val formattedDate = formatter.format(deadline)

    val backgroundColor = if (isOverdue) BrutalRed else Color.Transparent
    val textColor = if (isOverdue) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .background(backgroundColor)
            .border(1.dp, if (isOverdue) BrutalRed else MaterialTheme.colorScheme.outline)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = textColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isOverdue) "ПРОСТРОЧЕНО $formattedDate" else "до $formattedDate",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Чіп тегу
 */
@Composable
fun TagChip(
    tag: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "#${tag}",
        style = MaterialTheme.typography.labelSmall,
        color = BrutalOrange,
        modifier = modifier
    )
}

/**
 * Компактна картка задачі (для списків)
 */
@Composable
fun CompactTaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityColor = when (task.priority) {
        TaskPriority.CRITICAL -> PriorityCritical
        TaskPriority.HIGH -> PriorityHigh
        TaskPriority.MEDIUM -> PriorityMedium
        TaskPriority.LOW -> PriorityLow
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Індикатор пріоритету
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(priorityColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Заголовок
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (task.deadline != null) {
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    .withZone(ZoneId.systemDefault())
                Text(
                    text = "до ${formatter.format(task.deadline)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.isOverdue) BrutalRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Аватар виконавця
        task.assignee?.let { assignee ->
            BrutalAvatar(
                name = assignee.fullName,
                size = 28.dp
            )
        }
    }
}
