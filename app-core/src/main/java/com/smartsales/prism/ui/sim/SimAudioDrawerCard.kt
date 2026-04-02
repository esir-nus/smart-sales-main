package com.smartsales.prism.ui.sim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.ui.components.PrismButton
import com.smartsales.prism.ui.drawers.AudioStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun SimAudioCard(
    entry: SimAudioEntry,
    viewModel: SimAudioDrawerViewModel,
    mode: RuntimeAudioDrawerMode,
    expanded: Boolean,
    currentChatAudioId: String?,
    onToggleExpanded: () -> Unit,
    onToggleStar: () -> Unit,
    onTranscribe: () -> Unit,
    onDelete: () -> Unit,
    onArtifactOpened: (String, String) -> Unit,
    onAskAi: () -> Unit,
    onSelectForChat: () -> Unit
) {
    var artifacts by remember(entry.item.id) { mutableStateOf<TingwuJobArtifacts?>(null) }
    var isArtifactLoading by remember(entry.item.id) { mutableStateOf(false) }
    var hasReportedArtifactOpen by remember(entry.item.id, expanded) { mutableStateOf(false) }
    var transcriptPreview by remember(entry.item.id) { mutableStateOf<String?>(null) }
    val isCurrentChatAudio = currentChatAudioId == entry.item.id
    val isBrowseMode = mode == RuntimeAudioDrawerMode.BROWSE
    val canExpandInBrowseMode = isBrowseMode && entry.item.status == AudioStatus.TRANSCRIBED
    val canSwipeToTranscribe = canSwipeRightToTranscribe(
        mode = mode,
        status = entry.item.status,
        expanded = expanded
    )
    val canSwipeToDelete = canSwipeLeftToDelete(
        mode = mode,
        status = entry.item.status,
        expanded = expanded
    )
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.25f },
        confirmValueChange = { value ->
            if (
                value == SwipeToDismissBoxValue.StartToEnd &&
                canSwipeToTranscribe
            ) {
                onTranscribe()
            }
            if (
                value == SwipeToDismissBoxValue.EndToStart &&
                canSwipeToDelete
            ) {
                onDelete()
            }
            false
        }
    )

    LaunchedEffect(entry.item.id, entry.item.status, expanded) {
        if (expanded && entry.item.status == AudioStatus.TRANSCRIBED && isBrowseMode) {
            isArtifactLoading = true
            artifacts = viewModel.getArtifacts(entry.item.id)
            isArtifactLoading = false
        }
    }

    LaunchedEffect(entry.item.id, entry.item.status, mode) {
        if (mode != RuntimeAudioDrawerMode.CHAT_RESELECT || entry.item.status != AudioStatus.TRANSCRIBED) {
            transcriptPreview = null
            return@LaunchedEffect
        }

        transcriptPreview = buildSimAudioTranscriptPreview(viewModel.getArtifacts(entry.item.id))
    }

    LaunchedEffect(expanded, entry.item.status, artifacts) {
        if (!expanded || entry.item.status != AudioStatus.TRANSCRIBED) {
            hasReportedArtifactOpen = false
            return@LaunchedEffect
        }
        if (artifacts != null && !hasReportedArtifactOpen) {
            onArtifactOpened(entry.item.id, entry.item.filename)
            hasReportedArtifactOpen = true
        }
    }

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.item.filename,
                    modifier = Modifier.weight(1f),
                    color = SimDrawerTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (entry.isTestImport && isBrowseMode) {
                    Surface(
                        color = SimDrawerAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "测试",
                            color = SimDrawerAccent,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                entry.item.timeDisplay.takeIf { it.isNotBlank() }?.let { timeDisplay ->
                    Text(
                        text = timeDisplay,
                        color = SimDrawerTextFaint,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (entry.item.isStarred) SimDrawerAccent else SimDrawerTextMuted,
                    modifier = if (mode == RuntimeAudioDrawerMode.BROWSE) {
                        Modifier.clickable(onClick = onToggleStar)
                    } else {
                        Modifier
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (mode == RuntimeAudioDrawerMode.CHAT_RESELECT) {
            SimAudioCompactPreviewRow(
                text = buildSimAudioSelectBodyText(
                    entry = entry,
                    transcriptPreview = transcriptPreview,
                    isCurrentChatAudio = isCurrentChatAudio
                ),
                maxLines = 1
            )

            if (entry.item.status == AudioStatus.TRANSCRIBING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (entry.item.progress ?: 0f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = SimDrawerAccent,
                    trackColor = SimDrawerDivider
                )
            }
        } else if (expanded && entry.item.status == AudioStatus.TRANSCRIBED) {
            Spacer(modifier = Modifier.height(8.dp))

            when {
                isArtifactLoading -> {
                    Text(
                        text = entry.item.summary ?: entry.preview,
                        color = SimDrawerTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在读取转写结果...",
                        color = SimDrawerTextSecondary,
                        fontSize = 12.sp
                    )
                }

                artifacts == null -> {
                    Text(
                        text = entry.item.summary ?: entry.preview,
                        color = SimDrawerTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "当前未读取到转写结果，请稍后重试。",
                        color = SimDrawerTextSecondary,
                        fontSize = 12.sp
                    )
                }

                else -> {
                    SimArtifactOverviewHeader(
                        artifacts = artifacts!!,
                        fallbackOverview = entry.item.summary ?: entry.preview
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SimArtifactContent(artifacts = artifacts!!)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            PrismButton(
                text = "Ask AI",
                onClick = onAskAi,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            when (entry.item.status) {
                AudioStatus.PENDING -> {
                    SimAudioCompactPreviewRow(
                        previewContent = {
                            SimBrowseModeSwipePrompt(
                                transcribeActive = dismissState.dismissDirection ==
                                    SwipeToDismissBoxValue.StartToEnd
                            )
                        }
                    )
                }

                AudioStatus.TRANSCRIBING -> {
                    val progress = entry.item.progress ?: 0f
                    SimAudioCompactPreviewRow(
                        text = buildTransparentStateLabel(progress),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = SimDrawerAccent,
                        trackColor = SimDrawerDivider
                    )
                }

                AudioStatus.TRANSCRIBED -> {
                    SimAudioCompactPreviewRow(
                        text = entry.item.summary ?: entry.preview,
                        maxLines = 1
                    )
                }
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = canSwipeToTranscribe,
        enableDismissFromEndToStart = canSwipeToDelete,
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (canSwipeToDelete) SimDrawerDeleteBackground else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        tint = if (canSwipeToDelete) Color.White else Color.Transparent
                    )
                }
            }
        }
    ) {
        val clickEnabled = if (mode == RuntimeAudioDrawerMode.CHAT_RESELECT) !isCurrentChatAudio else true
        val onCardClick: () -> Unit = {
            when (mode) {
                RuntimeAudioDrawerMode.CHAT_RESELECT -> {
                    if (!isCurrentChatAudio) onSelectForChat()
                }

                RuntimeAudioDrawerMode.BROWSE -> {
                    if (canExpandInBrowseMode) {
                        onToggleExpanded()
                    } else {
                        coroutineScope.launch {
                            offsetX.snapTo(0f)
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = keyframes {
                                    durationMillis = 260
                                    0f at 0
                                    (-8f) at 40
                                    8f at 80
                                    (-6f) at 120
                                    4f at 160
                                    0f at 220
                                }
                            )
                        }
                    }
                }
            }
        }

        if (mode == RuntimeAudioDrawerMode.CHAT_RESELECT) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isCurrentChatAudio) 0.52f else 1f)
                    .background(
                        if (isCurrentChatAudio) SimDrawerCurrentSurface else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = clickEnabled, onClick = onCardClick)
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 14.dp)
                        .offset(x = offsetX.value.dp)
                ) {
                    cardContent()
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (entry.item.status == AudioStatus.TRANSCRIBING) {
                            SimDrawerCardSurfaceStrong
                        } else {
                            SimDrawerCardSurface
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (entry.item.status == AudioStatus.TRANSCRIBING) {
                            SimDrawerCardBorderStrong
                        } else {
                            SimDrawerCardBorder
                        },
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable(enabled = clickEnabled, onClick = onCardClick)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .offset(x = offsetX.value.dp)
                ) {
                    cardContent()
                }
            }
        }
    }
}

internal fun canSwipeRightToTranscribe(
    mode: RuntimeAudioDrawerMode,
    status: AudioStatus,
    expanded: Boolean
): Boolean {
    return mode == RuntimeAudioDrawerMode.BROWSE &&
        !expanded &&
        status == AudioStatus.PENDING
}

internal fun canSwipeLeftToDelete(
    mode: RuntimeAudioDrawerMode,
    status: AudioStatus,
    expanded: Boolean
): Boolean {
    return mode == RuntimeAudioDrawerMode.BROWSE &&
        !expanded &&
        (status == AudioStatus.PENDING || status == AudioStatus.TRANSCRIBED)
}

@Composable
private fun SimAudioCompactPreviewRow(
    text: String? = null,
    maxLines: Int = 2,
    previewContent: @Composable (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        when {
            previewContent != null -> previewContent()
            text != null -> Text(
                text = text,
                color = SimDrawerTextSecondary,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SimBrowseModeSwipePrompt(transcribeActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "sim_audio_swipe_prompt")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sim_audio_swipe_prompt_alpha"
    )

    Text(
        text = if (transcribeActive) ">>> 释放以转写" else "右滑开始转写 >>>",
        color = SimDrawerAccent.copy(alpha = alpha),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}
