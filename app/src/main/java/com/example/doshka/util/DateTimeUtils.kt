package com.example.doshka.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Утиліти для роботи з датою та часом
 */
object DateTimeUtils {

    // Форматери без локалізованих назв місяців (числовий формат)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        .withZone(ZoneId.systemDefault())

    private val fullDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())

    private val shortDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")
        .withZone(ZoneId.systemDefault())

    /**
     * Парсить datetime строку з сервера у Instant
     * Сервер повертає формат "2026-02-01T15:25:57.428006" без 'Z',
     * а Instant.parse() очікує ISO-8601 з timezone designator
     */
    fun parseInstant(dateTimeStr: String): Instant {
        return try {
            // Якщо вже є 'Z' або timezone — парсимо напряму
            if (dateTimeStr.endsWith("Z") ||
                dateTimeStr.contains("+") ||
                (dateTimeStr.length > 10 && dateTimeStr.substring(10).contains("-"))) {
                Instant.parse(dateTimeStr)
            } else {
                // Додаємо 'Z' (UTC timezone)
                Instant.parse(dateTimeStr + "Z")
            }
        } catch (e: Exception) {
            // Fallback — поточний час
            Instant.now()
        }
    }

    /**
     * Парсить datetime строку у мілісекунди (epoch millis)
     */
    fun parseToEpochMilli(dateTimeStr: String): Long {
        return parseInstant(dateTimeStr).toEpochMilli()
    }

    /**
     * Парсить nullable datetime строку у Instant
     */
    fun parseInstantOrNull(dateTimeStr: String?): Instant? {
        return dateTimeStr?.let { parseInstant(it) }
    }

    /**
     * Парсить nullable datetime строку у мілісекунди
     */
    fun parseToEpochMilliOrNull(dateTimeStr: String?): Long? {
        return dateTimeStr?.let { parseToEpochMilli(it) }
    }

    /**
     * Форматує Instant у короткий формат дати (dd.MM.yyyy)
     */
    fun formatShortDate(instant: Instant?): String {
        return instant?.let { shortDateFormatter.format(it) } ?: "—"
    }

    /**
     * Форматує Instant у повний формат з часом (dd.MM.yyyy HH:mm)
     */
    fun formatFullDateTime(instant: Instant?): String {
        return instant?.let { fullDateTimeFormatter.format(it) } ?: "—"
    }

    /**
     * Форматує Instant у скорочений формат з часом (dd.MM HH:mm)
     */
    fun formatShortDateTime(instant: Instant?): String {
        return instant?.let { shortDateTimeFormatter.format(it) } ?: "—"
    }

    /**
     * Форматує epoch millis у короткий формат дати (dd.MM.yyyy)
     */
    fun formatShortDateFromMillis(millis: Long?): String {
        return millis?.let { formatShortDate(Instant.ofEpochMilli(it)) } ?: "—"
    }
}
