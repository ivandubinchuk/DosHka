package com.example.doshka.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class TaskTest {

    @Test
    fun `isOverdue returns true when deadline is past and not completed`() {
        val task = Task(
            id = "1",
            title = "Test",
            columnId = "col-1",
            position = 0,
            deadline = Instant.now().minus(1, ChronoUnit.DAYS),
            completedAt = null
        )

        assertTrue(task.isOverdue)
    }

    @Test
    fun `isOverdue returns false when deadline is in future`() {
        val task = Task(
            id = "1",
            title = "Test",
            columnId = "col-1",
            position = 0,
            deadline = Instant.now().plus(1, ChronoUnit.DAYS),
            completedAt = null
        )

        assertFalse(task.isOverdue)
    }

    @Test
    fun `isOverdue returns false when task is completed`() {
        val task = Task(
            id = "1",
            title = "Test",
            columnId = "col-1",
            position = 0,
            deadline = Instant.now().minus(1, ChronoUnit.DAYS),
            completedAt = Instant.now()
        )

        assertFalse(task.isOverdue)
    }

    @Test
    fun `isOverdue returns false when no deadline`() {
        val task = Task(
            id = "1",
            title = "Test",
            columnId = "col-1",
            position = 0,
            deadline = null
        )

        assertFalse(task.isOverdue)
    }

    @Test
    fun `isDeadlineSoon returns true when deadline is within 24 hours`() {
        val task = Task(
            id = "1",
            title = "Test",
            columnId = "col-1",
            position = 0,
            deadline = Instant.now().plus(12, ChronoUnit.HOURS),
            completedAt = null
        )

        assertTrue(task.isDeadlineSoon)
    }

    @Test
    fun `isDeadlineSoon returns false when deadline is more than 24 hours away`() {
        val task = Task(
            id = "1",
            title = "Test",
            columnId = "col-1",
            position = 0,
            deadline = Instant.now().plus(2, ChronoUnit.DAYS),
            completedAt = null
        )

        assertFalse(task.isDeadlineSoon)
    }

    @Test
    fun `isDeadlineSoon returns false when already overdue`() {
        val task = Task(
            id = "1",
            title = "Test",
            columnId = "col-1",
            position = 0,
            deadline = Instant.now().minus(1, ChronoUnit.HOURS),
            completedAt = null
        )

        assertFalse(task.isDeadlineSoon)
    }

    @Test
    fun `isDeadlineSoon returns false when completed`() {
        val task = Task(
            id = "1",
            title = "Test",
            columnId = "col-1",
            position = 0,
            deadline = Instant.now().plus(12, ChronoUnit.HOURS),
            completedAt = Instant.now()
        )

        assertFalse(task.isDeadlineSoon)
    }
}

class TaskPriorityTest {

    @Test
    fun `fromString returns correct priority for valid input`() {
        assertEquals(TaskPriority.CRITICAL, TaskPriority.fromString("critical"))
        assertEquals(TaskPriority.HIGH, TaskPriority.fromString("high"))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromString("medium"))
        assertEquals(TaskPriority.LOW, TaskPriority.fromString("low"))
    }

    @Test
    fun `fromString is case insensitive`() {
        assertEquals(TaskPriority.CRITICAL, TaskPriority.fromString("CRITICAL"))
        assertEquals(TaskPriority.HIGH, TaskPriority.fromString("High"))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromString("MeDiUm"))
    }

    @Test
    fun `fromString returns MEDIUM for unknown input`() {
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromString("unknown"))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromString(""))
        assertEquals(TaskPriority.MEDIUM, TaskPriority.fromString("invalid"))
    }

    @Test
    fun `toString returns lowercase value`() {
        assertEquals("critical", TaskPriority.CRITICAL.toString())
        assertEquals("high", TaskPriority.HIGH.toString())
        assertEquals("medium", TaskPriority.MEDIUM.toString())
        assertEquals("low", TaskPriority.LOW.toString())
    }
}
