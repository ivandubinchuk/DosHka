package com.example.doshka.ui.screens.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.doshka.domain.model.Message
import com.example.doshka.domain.model.MessageType
import com.example.doshka.ui.components.BrutalAvatar
import com.example.doshka.ui.components.BrutalCard
import com.example.doshka.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskChatScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    viewModel: TaskChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messageText by viewModel.messageText.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Скрол до останнього повідомлення
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ЧАТ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.taskTitle.isNotBlank()) {
                            Text(
                                text = uiState.taskTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                    IconButton(onClick = viewModel::loadMessages) {
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
        },
        snackbarHost = {
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = BrutalRed,
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text(uiState.error!!)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Список повідомлень
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BrutalOrange)
                        }
                    }
                } else if (uiState.messages.isEmpty()) {
                    item {
                        EmptyChatState()
                    }
                } else {
                    items(uiState.messages) { message ->
                        ChatMessageItem(
                            message = message,
                            isCurrentUser = message.sender?.id == uiState.currentUserId
                        )
                    }
                }
            }

            // Поле введення
            MessageInputField(
                value = messageText,
                onValueChange = viewModel::updateMessageText,
                onSend = viewModel::sendMessage,
                onSendVoice = viewModel::sendVoiceMessage,
                isSending = uiState.isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: Message,
    isCurrentUser: Boolean
) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    when (message.type) {
        MessageType.SYSTEM -> {
            // Системне повідомлення — по центру, моноширинний шрифт
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "[${formatter.format(message.createdAt)}] ${message.content.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        else -> {
            // Звичайне повідомлення
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
            ) {
                if (!isCurrentUser && message.sender != null) {
                    BrutalAvatar(
                        name = message.sender.fullName,
                        size = 32.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(
                    horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    // Ім'я відправника (тільки для чужих повідомлень)
                    if (!isCurrentUser && message.sender != null) {
                        Text(
                            text = message.sender.fullName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Бульбашка повідомлення
                    Box(
                        modifier = Modifier
                            .background(
                                if (isCurrentUser) BrutalOrange else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 2.dp,
                                color = if (isCurrentUser) BrutalOrangeDark else MaterialTheme.colorScheme.outline
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface
                            )

                            // Голосове повідомлення
                            if (message.type == MessageType.VOICE && message.voiceDuration != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🎤 ${message.voiceDuration}с",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrentUser) Color.White.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Час
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatter.format(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isCurrentUser) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendVoice: (File, Int) -> Unit,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // Таймер запису
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (isRecording) {
                delay(1000)
                recordingDuration++
                // Максимум 60 секунд
                if (recordingDuration >= 60) {
                    stopRecording(mediaRecorder)
                    mediaRecorder = null
                    isRecording = false
                    audioFile?.let { file ->
                        onSendVoice(file, recordingDuration)
                    }
                }
            }
        }
    }

    // Анімація пульсації під час запису
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кнопка мікрофона
        IconButton(
            onClick = {
                if (!hasPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@IconButton
                }

                if (isRecording) {
                    // Зупиняємо запис
                    stopRecording(mediaRecorder)
                    mediaRecorder = null
                    isRecording = false
                    audioFile?.let { file ->
                        onSendVoice(file, recordingDuration)
                    }
                } else {
                    // Починаємо запис
                    val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                    audioFile = file
                    mediaRecorder = createMediaRecorder(context, file)
                    try {
                        mediaRecorder?.start()
                        isRecording = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isRecording) BrutalRed.copy(alpha = alpha) else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(2.dp, if (isRecording) BrutalRed else BrutalBorderLight)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Зупинити" else "Записати голос",
                tint = if (isRecording) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isRecording) {
            // Показуємо тривалість запису
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(BrutalRed.copy(alpha = 0.1f))
                    .border(2.dp, BrutalRed)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "●",
                        color = BrutalRed.copy(alpha = alpha),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ЗАПИС: ${formatDuration(recordingDuration)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrutalRed
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "> введіть повідомлення...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = false,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrutalOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Кнопка відправки
        IconButton(
            onClick = onSend,
            enabled = value.isNotBlank() && !isSending && !isRecording,
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (value.isNotBlank() && !isRecording) BrutalOrange else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(2.dp, BrutalBorderLight)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Надіслати",
                    tint = if (value.isNotBlank() && !isRecording) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun createMediaRecorder(context: android.content.Context, file: File): MediaRecorder {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
    } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
    }.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(128000)
        setAudioSamplingRate(44100)
        setOutputFile(file.absolutePath)
        prepare()
    }
}

private fun stopRecording(recorder: MediaRecorder?) {
    try {
        recorder?.stop()
        recorder?.release()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

@Composable
private fun EmptyChatState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = """
                ┌───────────────────┐
                │                   │
                │   ПОВІДОМЛЕНЬ    │
                │      НЕМАЄ       │
                │                   │
                └───────────────────┘
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Напишіть перше повідомлення",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
