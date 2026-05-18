package com.example.doshka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.doshka.domain.model.Column
import com.example.doshka.domain.model.Task
import com.example.doshka.ui.theme.*

/**
 * Колонка Канбан-дошки в брутальному стилі
 */
@Composable
fun KanbanColumn(
    column: Column,
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onTaskLongClick: (Task) -> Unit,
    onAddTaskClick: (() -> Unit)? = null,
    onMoveTaskToNext: ((Task) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDropTarget: Boolean = false,
    isLastColumn: Boolean = false
) {
    val taskCount = tasks.size
    val wipLimit = column.wipLimit ?: Int.MAX_VALUE
    val isOverWip = taskCount >= wipLimit

    Box(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .drawBehind {
                // Жорстка тінь для колонки
                translate(left = 4.dp.toPx(), top = 4.dp.toPx()) {
                    drawRect(
                        color = BrutalShadowLight.copy(alpha = 0.3f),
                        size = size
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isDropTarget) BrutalOrange.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surface
                )
                .border(
                    width = 2.dp,
                    color = if (isDropTarget) BrutalOrange else MaterialTheme.colorScheme.outline
                )
        ) {
            // Заголовок колонки
            ColumnHeader(
                name = column.name,
                taskCount = taskCount,
                wipLimit = column.wipLimit,
                isOverWip = isOverWip
            )

            // WIP прогрес-бар
            if (column.wipLimit != null) {
                WipProgressBar(
                    current = taskCount,
                    limit = column.wipLimit,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Divider(
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outline
            )

            // Список задач
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onLongClick = { onTaskLongClick(task) },
                        onMoveToNext = onMoveTaskToNext?.let { { it(task) } },
                        isLastColumn = isLastColumn
                    )
                }

                // Зона для drop (при перетягуванні)
                item {
                    DropZone(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )
                }
            }

            // Кнопка додавання задачі (тільки для менеджерів)
            if (!isOverWip && onAddTaskClick != null) {
                AddTaskButton(
                    onClick = onAddTaskClick,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

/**
 * Заголовок колонки
 */
@Composable
private fun ColumnHeader(
    name: String,
    taskCount: Int,
    wipLimit: Int?,
    isOverWip: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isOverWip) BrutalRed.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = taskCount.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isOverWip) BrutalRed else MaterialTheme.colorScheme.onSurface
            )
            if (wipLimit != null) {
                Text(
                    text = "/$wipLimit WIP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Зона для drop (штрихована область)
 */
@Composable
private fun DropZone(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val dashLength = 10.dp.toPx()
                val gapLength = 10.dp.toPx()

                drawRect(
                    color = Color.Gray.copy(alpha = 0.3f),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(dashLength, gapLength)
                        )
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "ПЕРЕТЯГНІТЬ СЮДИ",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
    }
}

/**
 * Кнопка додавання задачі
 */
@Composable
private fun AddTaskButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BrutalOutlinedButton(
        text = "+ ДОДАТИ ЗАДАЧУ",
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Мінімалістична колонка для мобільного перегляду
 */
@Composable
fun CompactKanbanColumn(
    column: Column,
    taskCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wipLimit = column.wipLimit ?: Int.MAX_VALUE
    val isOverWip = taskCount >= wipLimit

    BrutalCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = column.name.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = taskCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isOverWip) BrutalRed else BrutalOrange
                )
                if (column.wipLimit != null) {
                    Text(
                        text = "/$wipLimit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (column.wipLimit != null) {
                Spacer(modifier = Modifier.height(8.dp))
                WipProgressBar(
                    current = taskCount,
                    limit = column.wipLimit
                )
            }
        }
    }
}
