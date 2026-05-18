package com.example.doshka.domain.model

/**
 * Доменна модель кольорової мітки
 */
data class Label(
    val id: String,
    val name: String,
    val color: String, // HEX колір, наприклад "#FF5722"
    val teamId: String
)
