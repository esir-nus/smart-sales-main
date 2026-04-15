package com.smartsales.prism.ui.sim

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.ui.components.connectivity.ConnectionState
import java.time.Instant
import kotlin.math.abs

private const val SIM_AUDIO_BROWSE_GRIP_VISUAL_TRANSLATION_FACTOR = 0.35f

@Composable
internal fun SimAudioDrawerContent(
    entries: List<SimAudioEntry>,
    viewModel: SimAudioDrawerViewModel,
    mode: RuntimeAudioDrawerMode,
    expandedAudioIds: Set<String>,
    currentChatAudioId: String?,
    onDismiss: () -> Unit,
    connectionState: ConnectionState,
    isSyncing: Boolean,
    syncFeedback: SimAudioSyncFeedback?,
    lastSyncTimestamp: Instant?,
    onSyncFromBadge: () -> Unit,
    onOpenConnectivity: () -> Unit,
    onArtifactOpened: (String, String) -> Unit,
    onAskAi: (SimAudioDiscussion) -> Unit,
    onDeleteAudio: (String) -> Unit,
    onSelectForChat: (SimChatAudioSelection) -> Unit,
    onImportTestAudio: () -> Unit,
    onBrowsePullOffsetChanged: (Float) -> Unit,
    onBrowsePullSettled: () -> Unit,
    showTestImportAction: Boolean
) {
    val syncVisualState = resolveSimAudioSyncVisualState(
        connectionState = connectionState,
        isSyncing = isSyncing,
        syncFeedback = syncFeedback
    )
    val showBrowseHelperDeck = shouldShowSimAudioBrowseHelperDeck(entries, mode)

    if (mode == RuntimeAudioDrawerMode.BROWSE) {
        SimAudioBrowseHeader(
            entryCount = entries.size,
            connectionState = connectionState,
            syncVisualState = syncVisualState,
            lastSyncTimestamp = lastSyncTimestamp,
            onDismiss = onDismiss,
            onSyncFromBadge = onSyncFromBadge,
            onOpenConnectivity = onOpenConnectivity,
            onBrowsePullOffsetChanged = onBrowsePullOffsetChanged,
            onBrowsePullSettled = onBrowsePullSettled
        )
    } else {
        SimDrawerHandle(
            dismissDirection = SimVerticalGestureDirection.DOWN,
            onDismiss = onDismiss,
            testTag = SIM_AUDIO_HANDLE_TEST_TAG,
            dismissOnTap = true
        )
        Text(
            text = "选择要讨论的录音",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            color = SimDrawerTextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        Text(
            text = "点击录音卡片切换当前聊天",
            color = SimDrawerTextSecondary,
            fontSize = 11.sp,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp, bottom = 10.dp)
        )
    }

    HorizontalDivider(color = SimDrawerDivider)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        if (showBrowseHelperDeck) {
            item {
                SimAudioBrowseHelperCard(
                    icon = Icons.Filled.North,
                    title = "上拉手柄同步工牌录音",
                    body = "当前演示库存只保留一条内置录音；上拉手柄可练习手动同步。"
                )
            }
            item {
                SimAudioBrowseHelperCard(
                    icon = Icons.Filled.Delete,
                    title = "左滑卡片可删除录音",
                    body = "教学辅助只在内置演示录音独占库存时显示，不会删除真实同步数据。"
                )
            }
        }

        items(entries, key = { it.item.id }) { entry ->
            SimAudioCard(
                entry = entry,
                viewModel = viewModel,
                mode = mode,
                expanded = expandedAudioIds.contains(entry.item.id),
                currentChatAudioId = currentChatAudioId,
                onToggleExpanded = { viewModel.toggleExpanded(entry.item.id) },
                onToggleStar = { viewModel.toggleStar(entry.item.id) },
                onTranscribe = { viewModel.startTranscription(entry.item.id) },
                onDelete = { onDeleteAudio(entry.item.id) },
                onArtifactOpened = onArtifactOpened,
                onAskAi = {
                    viewModel.createDiscussion(entry.item.id)?.let(onAskAi)
                },
                onSelectForChat = {
                    onSelectForChat(
                        SimChatAudioSelection(
                            audioId = entry.item.id,
                            title = entry.item.filename,
                            summary = entry.item.summary ?: entry.preview,
                            status = entry.item.status,
                            localAvailability = entry.localAvailability
                        )
                    )
                }
            )
        }

        if (showTestImportAction && mode == RuntimeAudioDrawerMode.BROWSE) {
            item {
                SimTestImportButton(onClick = onImportTestAudio)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SimAudioBrowseHeader(
    entryCount: Int,
    connectionState: ConnectionState,
    syncVisualState: SimAudioSyncVisualState,
    lastSyncTimestamp: Instant?,
    onDismiss: () -> Unit,
    onSyncFromBadge: () -> Unit,
    onOpenConnectivity: () -> Unit,
    onBrowsePullOffsetChanged: (Float) -> Unit,
    onBrowsePullSettled: () -> Unit
) {
    SimAudioBrowseGrip(
        syncVisualState = syncVisualState,
        onDismiss = onDismiss,
        onSyncFromBadge = onSyncFromBadge,
        onBrowsePullOffsetChanged = onBrowsePullOffsetChanged,
        onBrowsePullSettled = onBrowsePullSettled
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "录音笔记",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp
                ),
                color = SimDrawerTextPrimary
            )
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = "$entryCount 项",
                    color = SimDrawerTextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SimAudioSmartCapsule(
                visualState = syncVisualState,
                connectionState = connectionState,
                lastSyncTimestamp = lastSyncTimestamp,
                onSyncFromBadge = onSyncFromBadge,
                onOpenConnectivity = onOpenConnectivity
            )
        }
    }
}

@Composable
internal fun SimAudioBrowseGrip(
    syncVisualState: SimAudioSyncVisualState,
    onDismiss: () -> Unit,
    onSyncFromBadge: () -> Unit,
    onBrowsePullOffsetChanged: (Float) -> Unit,
    onBrowsePullSettled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pullThresholdPx = with(density) { 55.dp.toPx() }
    val maxTravelPx = with(density) { 90.dp.toPx() }
    val dismissThresholdPx = with(density) { 56.dp.toPx() }
    val touchSlopPx = with(density) { 8.dp.toPx() }
    val canTriggerSync = canTriggerSimAudioSync(syncVisualState)
    var gestureOffsetPx by remember { mutableFloatStateOf(0f) }
    var thresholdReached by remember { mutableFloatStateOf(0f) }
    var deniedTick by remember { mutableIntStateOf(0) }
    val shakeOffset = remember { Animatable(0f) }
    val helperAlpha by animateFloatAsState(
        targetValue = if (thresholdReached > 0f) 1f else 0f,
        label = "sim_audio_grip_helper_alpha"
    )
    val helperOffsetY by animateDpAsState(
        targetValue = if (thresholdReached > 0f) 0.dp else 10.dp,
        label = "sim_audio_grip_helper_offset"
    )
    val gripWidth by animateDpAsState(
        targetValue = if (thresholdReached > 0f) 44.dp else 36.dp,
        label = "sim_audio_grip_width"
    )
    val gripHeight by animateDpAsState(
        targetValue = if (thresholdReached > 0f) 6.dp else 4.dp,
        label = "sim_audio_grip_height"
    )

    LaunchedEffect(deniedTick) {
        if (deniedTick == 0) return@LaunchedEffect
        shakeOffset.snapTo(0f)
        shakeOffset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 360
                0f at 0
                (-10f) at 50
                10f at 110
                (-8f) at 170
                6f at 230
                0f at 320
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(36.dp)
                .graphicsLayer { translationX = shakeOffset.value }
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Transparent)
                .pointerInput(syncVisualState) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPosition = down.position
                        val touchSlop = viewConfiguration.touchSlop
                        var activePointerId = down.id
                        var dragLocked = false
                        var rejected = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == activePointerId }
                                ?: event.changes.firstOrNull()
                                ?: break

                            activePointerId = change.id

                            if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                                val shouldDismiss =
                                    gestureOffsetPx <= -dismissThresholdPx && thresholdReached == 0f
                                val shouldSync = thresholdReached > 0f
                                gestureOffsetPx = 0f
                                thresholdReached = 0f
                                onBrowsePullSettled()
                                when {
                                    shouldSync && canTriggerSync -> onSyncFromBadge()
                                    shouldSync -> deniedTick += 1
                                    shouldDismiss -> onDismiss()
                                }
                                break
                            }

                            val totalDx = change.position.x - startPosition.x
                            val totalDy = change.position.y - startPosition.y
                            val absDx = abs(totalDx)
                            val absDy = abs(totalDy)

                            if (!dragLocked && !rejected && (absDx > touchSlop || absDy > touchSlop)) {
                                if (absDy >= absDx) {
                                    dragLocked = true
                                } else {
                                    rejected = true
                                }
                            }

                            if (dragLocked) {
                                change.consume()
                                val updatedGestureOffsetPx =
                                    (-totalDy).coerceIn(-maxTravelPx, maxTravelPx)
                                gestureOffsetPx = updatedGestureOffsetPx
                                val upwardPullPx = updatedGestureOffsetPx.coerceAtLeast(0f)
                                val translatedPullPx =
                                    (upwardPullPx * SIM_AUDIO_BROWSE_GRIP_VISUAL_TRANSLATION_FACTOR)
                                        .coerceAtMost(maxTravelPx)
                                // 阈值跟随真实上拉距离，视觉位移继续保留橡皮筋手感。
                                thresholdReached = if (upwardPullPx >= pullThresholdPx) 1f else 0f
                                onBrowsePullOffsetChanged(translatedPullPx)
                            }
                        }
                    }
                }
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .graphicsLayer { translationY = with(density) { helperOffsetY.toPx() } }
                    .alpha(helperAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    tint = SimDrawerAccent,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "松开同步",
                    color = SimDrawerTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .size(width = gripWidth, height = gripHeight)
                    .align(Alignment.Center)
                    .background(
                        when {
                            thresholdReached > 0f -> SimDrawerAccent
                            gestureOffsetPx > touchSlopPx -> SimDrawerAccent.copy(alpha = 0.72f)
                            else -> Color.White.copy(alpha = 0.2f)
                        },
                        RoundedCornerShape(999.dp)
                    )
            )
        }
    }
}

@Composable
private fun SimAudioSmartCapsule(
    visualState: SimAudioSyncVisualState,
    connectionState: ConnectionState,
    lastSyncTimestamp: Instant?,
    onSyncFromBadge: () -> Unit,
    onOpenConnectivity: () -> Unit
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    val syncRelativeLabel = resolveSimAudioSyncRelativeLabel(visualState, lastSyncTimestamp)
    val syncActionEnabled = canTriggerSimAudioSync(visualState)

    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SimDrawerDividerStrong)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 左侧：连接状态（点击打开连接管理器）
            val connInteraction = remember { MutableInteractionSource() }
            val connPressed by connInteraction.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = if (connPressed) 0.96f else 1f
                        scaleY = if (connPressed) 0.96f else 1f
                    }
                    .clickable(
                        interactionSource = connInteraction,
                        indication = null,
                        onClick = onOpenConnectivity
                    )
                    .padding(start = 10.dp, end = 8.dp, top = 7.dp, bottom = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Filled.Link else Icons.Filled.LinkOff,
                        contentDescription = null,
                        tint = if (isConnected) SimDrawerAccentSuccess else SimDrawerBlockedText,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "徽章管理",
                        color = if (isConnected) SimDrawerTextPrimary else SimDrawerBlockedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 右侧：同步状态（仅在已连接时显示；点击触发手动同步）
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .background(
                            color = SimDrawerDivider,
                            shape = RoundedCornerShape(0.dp)
                        )
                        .size(width = 1.dp, height = 18.dp)
                )

                val syncInteraction = remember { MutableInteractionSource() }
                val syncPressed by syncInteraction.collectIsPressedAsState()
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = if (syncPressed) 0.96f else 1f
                            scaleY = if (syncPressed) 0.96f else 1f
                        }
                        .clickable(
                            interactionSource = syncInteraction,
                            indication = null,
                            onClick = { if (syncActionEnabled) onSyncFromBadge() }
                        )
                        .padding(start = 8.dp, end = 10.dp, top = 7.dp, bottom = 7.dp)
                ) {
                    AnimatedContent(
                        targetState = visualState,
                        label = "sim_audio_capsule_sync_state"
                    ) { targetState ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            val syncTint = when (targetState) {
                                SimAudioSyncVisualState.SYNCING -> SimDrawerAccent
                                SimAudioSyncVisualState.SYNCED -> SimDrawerAccentSuccess
                                SimAudioSyncVisualState.ERROR -> SimDrawerDeleteBackground
                                else -> SimDrawerTextSecondary
                            }
                            Icon(
                                imageVector = when (targetState) {
                                    SimAudioSyncVisualState.SYNCED -> Icons.Filled.Check
                                    SimAudioSyncVisualState.ERROR -> Icons.Filled.ErrorOutline
                                    else -> Icons.Filled.Sync
                                },
                                contentDescription = null,
                                tint = syncTint,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = syncRelativeLabel,
                                color = syncTint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimAudioBrowseHelperCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, SimDrawerDivider, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(SimDrawerAccent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SimDrawerAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = SimDrawerTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = body,
                    color = SimDrawerTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun SimDrawerHeaderIconAction(
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
            .border(1.dp, SimDrawerDivider, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun SimTestImportButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.Transparent, RoundedCornerShape(12.dp))
            .border(1.dp, SimDrawerTextFaint, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "+",
                fontSize = 18.sp,
                color = SimDrawerTextPrimary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                text = "导入测试音频",
                fontSize = 13.sp,
                color = SimDrawerTextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
