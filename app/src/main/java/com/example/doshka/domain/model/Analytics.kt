package com.example.doshka.domain.model

import java.time.LocalDate

/**
 * Статистика дашборду
 */
data class DashboardStats(
    val totalTasks: Int,
    val completedTasks: Int,
    val inProgressTasks: Int,
    val overdueTasks: Int,
    val completionRate: Float, // відсоток завершених задач
    val averageCycleTime: Long? // середній час виконання в мілісекундах
)

/**
 * Швидкість команди (velocity)
 */
data class VelocityData(
    val weekNumber: Int,
    val year: Int,
    val completedTasks: Int,
    val startDate: LocalDate,
    val endDate: LocalDate
)

/**
 * Дані для Cumulative Flow Diagram
 */
data class CumulativeFlowData(
    val date: LocalDate,
    val columnCounts: Map<String, Int> // columnId -> count
)

/**
 * Cycle Time по виконавцю
 */
data class CycleTimeData(
    val userId: String,
    val userName: String,
    val averageCycleTimeHours: Float,
    val tasksCompleted: Int
)

/**
 * Дані для Heatmap активності
 */
data class ActivityHeatmapData(
    val userId: String,
    val userName: String,
    val dayOfWeek: Int, // 1 = понеділок, 7 = неділя
    val actionsCount: Int
)

/**
 * Дані для Burndown chart
 */
data class BurndownData(
    val date: LocalDate,
    val remainingTasks: Int,
    val idealRemaining: Int
)
