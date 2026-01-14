package com.smartsales.feature.media.audio

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audio/SwipeableRecordingCard.kt
// 模块：:feature:media
// 说明：V17 Streamlined card with swipe-to-reveal action tray
// 作者：创建于 2026-01-14

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * V17 Streamlined Recording Card with swipe-to-reveal action tray.
 *
 * Key features:
 * - Tap → opens transcript view
 * - Swipe left → reveals action tray (play/transcribe/delete)
 * - Long press → enters multi-select mode
 * - Star icon → toggle flag
 * - showHint → one-time nudge animation to teach swipe
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableRecordingCard(
    recording: AudioRecordingUi,
    onTap: () -> Unit,
    onPlayClicked: () -> Unit,
    onTranscribeClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onFlagToggle: () -> Unit,
    onLongPress: () -> Unit,
    showHint: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { false } // Never auto-dismiss, just reveal
    )

    // Hint animation: subtle left nudge
    val hintOffset by animateFloatAsState(
        targetValue = if (showHint) -30f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 400,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "hint_offset"
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            SwipeActionTray(
                onPlayClicked = onPlayClicked,
                onTranscribeClicked = onTranscribeClicked,
                onDeleteClicked = onDeleteClicked
            )
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(hintOffset.toInt(), 0) }
                .combinedClickable(
                    onClick = onTap,
                    onLongClick = onLongPress
                )
                .testTag("swipeable_card_${recording.id}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row: Star + Title + Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Star Flag
                    Icon(
                        imageVector = if (recording.isFlagged) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (recording.isFlagged) "已标记" else "标记",
                        tint = if (recording.isFlagged) Color(0xFFFFD60A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onFlagToggle() }
                            .testTag("star_${recording.id}")
                    )

                    // Title
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Duration / Time
                    Text(
                        text = recording.createdAtText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Summary / Status Hint
                when (recording.transcriptionStatus) {
                    TranscriptionStatus.IN_PROGRESS -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "正在转写...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            androidx.compose.material3.LinearProgressIndicator(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    TranscriptionStatus.DONE -> {
                        Text(
                            text = recording.transcriptPreview ?: "已转写完成",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TranscriptionStatus.ERROR -> {
                        Text(
                            text = "转写失败，滑动重试",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        Text(
                            text = "← 滑动转写",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Action tray revealed on swipe left.
 */
@Composable
private fun SwipeActionTray(
    onPlayClicked: () -> Unit,
    onTranscribeClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        // Play Button
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .background(Color(0xFF007AFF))
                .clickable { onPlayClicked() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "播放",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "播放",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // Transcribe Button
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .background(Color(0xFF34C759))
                .clickable { onTranscribeClicked() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TextSnippet,
                    contentDescription = "转写",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "转写",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // Delete Button
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .background(Color(0xFFFF3B30))
                .clickable { onDeleteClicked() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "删除",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}
