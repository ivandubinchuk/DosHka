package com.example.doshka.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.doshka.ui.theme.*

/**
 * Брутальна кнопка з текстом
 */
@Composable
fun BrutalTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = BrutalOrange,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Вторинна брутальна кнопка (прозорий фон)
 */
@Composable
fun BrutalOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = Color.Unspecified
) {
    val actualBorderColor = if (borderColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurface
    } else borderColor

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = actualBorderColor
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, actualBorderColor),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Брутальна кнопка з контентом (для кастомного вмісту)
 */
@Composable
fun BrutalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = BrutalOrange,
    contentColor: Color = BrutalSurfaceLight,
    borderColor: Color = BrutalBorderLight,
    shadowColor: Color = BrutalShadowLight,
    shadowOffset: Dp = 4.dp,
    borderWidth: Dp = 2.dp,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        content = content
    )
}

/**
 * Брутальна картка з жорсткою тінню
 */
@Composable
fun BrutalCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shadowColor: Color = BrutalShadowLight,
    shadowOffset: Dp = 4.dp,
    borderWidth: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                // Жорстка тінь
                translate(
                    left = shadowOffset.toPx(),
                    top = shadowOffset.toPx()
                ) {
                    drawRect(
                        color = shadowColor,
                        size = size
                    )
                }
            }
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor)
                .border(borderWidth, borderColor)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                ),
            content = content
        )
    }
}

/**
 * Брутальне текстове поле
 */
@Composable
fun BrutalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .drawBehind {
                    translate(left = 3.dp.toPx(), top = 3.dp.toPx()) {
                        drawRect(
                            color = if (isError) BrutalRed else BrutalShadowLight,
                            size = size
                        )
                    }
                }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "> $placeholder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = singleLine,
                maxLines = maxLines,
                isError = isError,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isError) BrutalRed else BrutalOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(0.dp)
            )
        }

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = BrutalRed,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Брутальний заголовок екрану
 */
@Composable
fun BrutalScreenTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Black,
        modifier = modifier
    )
}

/**
 * Штамп пріоритету
 */
@Composable
fun PriorityStamp(
    priority: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, text) = when (priority.lowercase()) {
        "critical" -> PriorityCritical to "КРИТИЧНИЙ"
        "high" -> PriorityHigh to "ВИСОКИЙ"
        "medium" -> PriorityMedium to "СЕРЕДНІЙ"
        "low" -> PriorityLow to "НИЗЬКИЙ"
        else -> PriorityMedium to priority.uppercase()
    }

    // Анімація пульсації для критичного пріоритету
    val infiniteTransition = rememberInfiniteTransition(label = "priority_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (priority.lowercase() == "critical") 0.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .background(backgroundColor.copy(alpha = alpha))
            .border(1.dp, Color.Black)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Прогрес-бар WIP
 */
@Composable
fun WipProgressBar(
    current: Int,
    limit: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (limit > 0) (current.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val progressColor = when {
        progress >= 1f -> BrutalRed
        progress >= 0.8f -> BrutalAmber
        else -> BrutalGreen
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$current/$limit",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.Gray.copy(alpha = 0.3f))
                .border(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(progressColor)
            )
        }
    }
}

/**
 * Індикатор офлайн-режиму
 */
@Composable
fun OfflineBanner(
    timeSinceOffline: Long,
    modifier: Modifier = Modifier
) {
    val minutes = (timeSinceOffline / 60000).toInt()
    val seconds = ((timeSinceOffline % 60000) / 1000).toInt()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BrutalAmber)
            .padding(8.dp)
    ) {
        Text(
            text = "OFFLINE MODE  ${String.format("%02d:%02d", minutes, seconds)}",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Аватар користувача (квадратний, брутальний)
 */
@Composable
fun BrutalAvatar(
    name: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    size: Dp = 40.dp
) {
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    val backgroundColor = remember(name) {
        val colors = listOf(BrutalOrange, BrutalAmber, BrutalGreen, PriorityHigh)
        colors[name.hashCode().mod(colors.size)]
    }

    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor)
            .border(2.dp, BrutalBorderLight),
        contentAlignment = Alignment.Center
    ) {
        // TODO: Додати Coil для завантаження imageUrl
        Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Пустий стан (немає даних)
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ASCII арт порожньої дошки
        Text(
            text = """
                ┌───────────────────┐
                │                   │
                │   ПОРОЖНЬО ТУТ   │
                │                   │
                └───────────────────┘
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
