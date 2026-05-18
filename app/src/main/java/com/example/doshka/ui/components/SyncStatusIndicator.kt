package com.example.doshka.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.doshka.data.sync.SyncState
import com.example.doshka.ui.theme.BrutalOrange
import com.example.doshka.ui.theme.BrutalYellow

/**
 * Індикатор статусу синхронізації
 * Відображає поточний стан з'єднання та синхронізації
 */
@Composable
fun SyncStatusIndicator(
    syncState: SyncState,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    val showBanner = syncState !is SyncState.Synced || pendingCount > 0

    AnimatedVisibility(
        visible = showBanner,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        SyncBanner(
            syncState = syncState,
            pendingCount = pendingCount
        )
    }
}

@Composable
private fun SyncBanner(
    syncState: SyncState,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, icon, text) = when (syncState) {
        is SyncState.Syncing -> Triple(
            BrutalYellow,
            Icons.Default.CloudSync,
            "Синхронізація..."
        )
        is SyncState.Offline -> Triple(
            Color(0xFF616161),
            Icons.Default.CloudOff,
            if (pendingCount > 0) "Офлайн · $pendingCount змін в черзі" else "Офлайн режим"
        )
        is SyncState.Error -> Triple(
            Color(0xFFFF1744),
            Icons.Default.Error,
            syncState.message
        )
        is SyncState.Synced -> Triple(
            Color(0xFF4CAF50),
            Icons.Default.CloudDone,
            if (pendingCount > 0) "$pendingCount змін в черзі" else "Синхронізовано"
        )
        is SyncState.Idle -> Triple(
            Color(0xFF757575),
            Icons.Default.CloudSync,
            if (pendingCount > 0) "$pendingCount змін в черзі" else "Очікування"
        )
    }

    // Анімація обертання для синхронізації
    val rotation by rememberInfiniteTransition(label = "sync_rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Surface(
        color = backgroundColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(18.dp)
                    .then(
                        if (syncState is SyncState.Syncing) {
                            Modifier.rotate(rotation)
                        } else {
                            Modifier
                        }
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

/**
 * Маленький індикатор синхронізації для AppBar
 */
@Composable
fun SyncStatusBadge(
    syncState: SyncState,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    val rotation by rememberInfiniteTransition(label = "sync_badge").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = modifier) {
        when (syncState) {
            is SyncState.Syncing -> {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = "Синхронізація",
                    tint = BrutalYellow,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation)
                )
            }
            is SyncState.Offline -> {
                BadgedBox(
                    badge = {
                        if (pendingCount > 0) {
                            Badge(
                                containerColor = BrutalOrange
                            ) {
                                Text(
                                    text = if (pendingCount > 9) "9+" else pendingCount.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Офлайн",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is SyncState.Error -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Помилка синхронізації",
                    tint = Color(0xFFFF1744),
                    modifier = Modifier.size(24.dp)
                )
            }
            is SyncState.Synced -> {
                if (pendingCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = BrutalOrange) {
                                Text(pendingCount.toString())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Є несинхронізовані зміни",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Синхронізовано",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is SyncState.Idle -> {
                // Нічого не показуємо в idle
            }
        }
    }
}

/**
 * Індикатор на картці задачі що показує статус синхронізації
 */
@Composable
fun TaskSyncIndicator(
    isSynced: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isSynced) {
        Box(
            modifier = modifier
                .size(8.dp)
                .background(
                    color = BrutalOrange,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
    }
}
