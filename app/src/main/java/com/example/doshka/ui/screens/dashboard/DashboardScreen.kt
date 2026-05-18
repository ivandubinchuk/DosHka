package com.example.doshka.ui.screens.dashboard

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.doshka.data.remote.api.VelocityDto
import com.example.doshka.data.remote.api.WorkloadDto
import com.example.doshka.ui.components.BrutalCard
import com.example.doshka.ui.components.EmptyState
import com.example.doshka.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Обробка експорту CSV
    LaunchedEffect(uiState.exportedCsvPath) {
        uiState.exportedCsvPath?.let { csvContent ->
            try {
                // Створюємо файл у кеші додатку
                val file = File(context.cacheDir, "doshka_tasks_export.csv")
                file.writeText(csvContent)

                // Створюємо Intent для поширення
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Експорт задач"))
                viewModel.clearExport()
            } catch (e: Exception) {
                Toast.makeText(context, "Помилка експорту: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ДАШБОРД",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
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
                    // Кнопка експорту CSV
                    if (uiState.hasTeam) {
                        IconButton(
                            onClick = viewModel::exportToCsv,
                            enabled = !uiState.isExporting
                        ) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = BrutalOrange
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Експорт CSV"
                                )
                            }
                        }
                    }

                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Оновити"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrutalOrange)
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error!!,
                            color = BrutalRed,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::refresh) {
                            Text("СПРОБУВАТИ ЗНОВУ")
                        }
                    }
                }
            }

            !uiState.hasTeam -> {
                EmptyState(
                    title = "НЕМАЄ КОМАНДИ",
                    message = "Створіть команду або приєднайтесь до існуючої, щоб бачити статистику",
                    modifier = Modifier.padding(paddingValues)
                )
            }

            else -> {
                val stats = uiState.stats
                val totalTasks = stats?.totalTasks ?: 0
                val completedTasks = stats?.completedTasks ?: 0
                val inProgressTasks = stats?.inProgressTasks ?: 0
                val overdueTasks = stats?.overdueTasks ?: 0

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Статистичні плитки
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatTile(
                            value = totalTasks,
                            label = "УСЬОГО",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            value = overdueTasks,
                            label = "ПРОСТРОЧЕНО",
                            isAlert = overdueTasks > 0,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatTile(
                            value = inProgressTasks,
                            label = "В РОБОТІ",
                            color = BrutalAmber,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            value = completedTasks,
                            label = "ЗАВЕРШЕНО",
                            color = BrutalGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Прогрес виконання
                    if (totalTasks > 0) {
                        BrutalCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "ПРОГРЕС ВИКОНАННЯ",
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val progress = completedTasks.toFloat() / totalTasks
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .fillMaxHeight()
                                            .background(BrutalGreen)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "${(progress * 100).toInt()}% ЗАВЕРШЕНО",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BrutalGreen
                                )
                            }
                        }
                    }

                    // Графік Velocity
                    BrutalCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "ШВИДКІСТЬ КОМАНДИ",
                                style = MaterialTheme.typography.labelLarge
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (uiState.velocityData.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "НЕМАЄ ДАНИХ\nЗавершіть задачі для аналітики",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                VelocityChart(
                                    data = uiState.velocityData,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                )
                            }
                        }
                    }

                    // Ефективність команди
                    uiState.efficiencyData?.let { efficiency ->
                        BrutalCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "ЕФЕКТИВНІСТЬ КОМАНДИ",
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Вчасно виконані
                                    EfficiencyItem(
                                        value = "${efficiency.onTimeCompletionRate.toInt()}%",
                                        label = "ВЧАСНО",
                                        sublabel = "${efficiency.onTimeTasks}/${efficiency.totalWithDeadline}",
                                        color = if (efficiency.onTimeCompletionRate >= 80) BrutalGreen else BrutalAmber
                                    )

                                    // Точність оцінок
                                    if (efficiency.estimationAccuracy != null) {
                                        EfficiencyItem(
                                            value = "${efficiency.estimationAccuracy.toInt()}%",
                                            label = "ОЦІНКИ",
                                            sublabel = "${efficiency.totalEstimatedHours.toInt()}/${efficiency.totalActualHours.toInt()}год",
                                            color = if (efficiency.estimationAccuracy in 80f..120f) BrutalGreen else BrutalAmber
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Навантаження виконавців
                    if (uiState.workloadData.isNotEmpty()) {
                        BrutalCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "НАВАНТАЖЕННЯ ВИКОНАВЦІВ",
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                WorkloadChart(
                                    data = uiState.workloadData,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                        }
                    }

                    // Розподіл по статусах
                    uiState.efficiencyData?.statusDistribution?.let { distribution ->
                        if (distribution.isNotEmpty()) {
                            BrutalCard {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "РОЗПОДІЛ ПО КОЛОНКАХ",
                                        style = MaterialTheme.typography.labelLarge
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    distribution.forEach { item ->
                                        StatusDistributionRow(
                                            name = item.columnName,
                                            count = item.taskCount,
                                            total = distribution.sumOf { it.taskCount }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Cycle Time - показуємо тільки якщо є дані
                    if (uiState.cycleTimeData.isNotEmpty()) {
                        BrutalCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "СЕРЕДНІЙ ЧАС ВИКОНАННЯ",
                                    style = MaterialTheme.typography.labelLarge
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    uiState.cycleTimeData.take(3).forEach { data ->
                                        CycleTimeItem(
                                            name = data.userName,
                                            hours = data.averageCycleTimeHours
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = BrutalOrange,
    isAlert: Boolean = false
) {
    BrutalCard(
        backgroundColor = if (isAlert) BrutalRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
        borderColor = if (isAlert) BrutalRed else MaterialTheme.colorScheme.outline,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = if (isAlert) BrutalRed else color,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CycleTimeItem(
    name: String,
    hours: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format("%.1f", hours),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = BrutalOrange
        )
        Text(
            text = "год",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Елемент метрики ефективності
 */
@Composable
private fun EfficiencyItem(
    value: String,
    label: String,
    sublabel: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = sublabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Графік навантаження виконавців
 */
@Composable
private fun WorkloadChart(
    data: List<WorkloadDto>,
    modifier: Modifier = Modifier
) {
    val maxTasks = data.maxOfOrNull { it.activeTasks + it.completedTasks } ?: 1

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.take(6).forEach { workload ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ім'я виконавця
                Text(
                    text = workload.userName.split(" ").firstOrNull() ?: workload.userName,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(80.dp),
                    maxLines = 1
                )

                // Стовпчики
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                ) {
                    val total = workload.activeTasks + workload.completedTasks
                    val widthFraction = if (maxTasks > 0) total.toFloat() / maxTasks else 0f

                    // Завершені
                    if (workload.completedTasks > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(widthFraction * (workload.completedTasks.toFloat() / total.coerceAtLeast(1)))
                                .fillMaxHeight()
                                .background(BrutalGreen)
                        )
                    }

                    // Активні (без прострочених)
                    val normalActive = (workload.activeTasks - workload.overdueTasks).coerceAtLeast(0)
                    if (normalActive > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(widthFraction * (normalActive.toFloat() / total.coerceAtLeast(1)))
                                .fillMaxHeight()
                                .background(BrutalOrange)
                        )
                    }

                    // Прострочені
                    if (workload.overdueTasks > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(widthFraction * (workload.overdueTasks.toFloat() / total.coerceAtLeast(1)))
                                .fillMaxHeight()
                                .background(BrutalRed)
                        )
                    }
                }

                // Числа
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${workload.activeTasks}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (workload.overdueTasks > 0) BrutalRed else BrutalOrange
                )
            }
        }

        // Легенда
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(color = BrutalGreen, label = "Завершені")
            LegendItem(color = BrutalOrange, label = "Активні")
            LegendItem(color = BrutalRed, label = "Прострочені")
        }
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Рядок розподілу по статусу
 */
@Composable
private fun StatusDistributionRow(
    name: String,
    count: Int,
    total: Int
) {
    val fraction = if (total > 0) count.toFloat() / total else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "$count (${(fraction * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(BrutalOrange)
            )
        }
    }
}

/**
 * Брутальний графік Velocity (стовпчикова діаграма)
 */
@Composable
private fun VelocityChart(
    data: List<VelocityDto>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.completedTasks } ?: 1

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.takeLast(8).forEach { week ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Значення зверху
                Text(
                    text = week.completedTasks.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Стовпчик
                val height = if (maxValue > 0) {
                    (week.completedTasks.toFloat() / maxValue * 80).dp
                } else 0.dp

                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(height.coerceAtLeast(4.dp))
                        .background(BrutalOrange)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Номер тижня
                Text(
                    text = "T${week.weekNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
