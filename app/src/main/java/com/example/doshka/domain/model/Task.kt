package com.example.doshka.domain.model

import java.time.Instant

/**
 * Доменна модель задачі
 */
data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val columnId: String,
    val position: Int,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val assignee: User? = null,
    val creator: User? = null,
    val deadline: Instant? = null,
    val estimatedHours: Float? = null,
    val actualHours: Float? = null,
    val tags: List<String> = emptyList(),
    val labels: List<Label> = emptyList(),
    val attachmentsCount: Int = 0,
    val commentsCount: Int = 0,
    val isDeleted: Boolean = false,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val isSynced: Boolean = true // Статус синхронізації
) {
    /**
     * Перевіряє, чи задача прострочена
     */
    val isOverdue: Boolean
        get() = deadline != null && deadline.isBefore(Instant.now()) && completedAt == null

    /**
     * Перевіряє, чи наближається дедлайн (менше 24 годин)
     */
    val isDeadlineSoon: Boolean
        get() {
            if (deadline == null || completedAt != null) return false
            val dayFromNow = Instant.now().plusSeconds(24 * 60 * 60)
            return deadline.isBefore(dayFromNow) && !deadline.isBefore(Instant.now())
        }
}

/**
 * Пріоритет задачі
 */
enum class TaskPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        fun fromString(value: String): TaskPriority = when (value.lowercase()) {
            "critical" -> CRITICAL
            "high" -> HIGH
            "medium" -> MEDIUM
            "low" -> LOW
            else -> MEDIUM
        }
    }

    override fun toString(): String = name.lowercase()
}
