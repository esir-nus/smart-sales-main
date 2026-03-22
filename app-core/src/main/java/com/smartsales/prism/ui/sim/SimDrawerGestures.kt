package com.smartsales.prism.ui.sim

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.BoxScope
import com.smartsales.prism.ui.PrismElevation

internal const val SIM_SCHEDULER_EDGE_ZONE_TEST_TAG = "sim_scheduler_edge_zone"
internal const val SIM_AUDIO_EDGE_ZONE_TEST_TAG = "sim_audio_edge_zone"
internal const val SIM_AUDIO_HANDLE_TEST_TAG = "sim_audio_handle"

internal enum class SimVerticalGestureDirection {
    UP,
    DOWN
}

internal fun canOpenSimSchedulerFromEdge(state: SimShellState): Boolean =
    state.activeDrawer == null &&
        !state.showHistory &&
        state.activeConnectivitySurface == null &&
        !state.showSettings

internal fun canOpenSimAudioFromEdge(
    state: SimShellState,
    isImeVisible: Boolean
): Boolean = canOpenSimSchedulerFromEdge(state) && !isImeVisible

@Composable
internal fun rememberSimImeVisibility(): Boolean {
    val density = LocalDensity.current
    return WindowInsets.ime.getBottom(density) > 0
}

@Composable
internal fun BoxScope.SimDrawerEdgeGestureLayer(
    state: SimShellState,
    isImeVisible: Boolean,
    onOpenScheduler: () -> Unit,
    onOpenAudioBrowse: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (canOpenSimSchedulerFromEdge(state)) {
        SimVerticalDragTrigger(
            modifier = modifier
                .zIndex(PrismElevation.Handles)
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
                .width(220.dp)
                .height(56.dp)
                .testTag(SIM_SCHEDULER_EDGE_ZONE_TEST_TAG),
            direction = SimVerticalGestureDirection.DOWN,
            threshold = 64.dp,
            onTriggered = onOpenScheduler
        )
    }

    if (canOpenSimAudioFromEdge(state, isImeVisible)) {
        SimVerticalDragTrigger(
            modifier = modifier
                .zIndex(PrismElevation.Handles)
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(32.dp)
                .testTag(SIM_AUDIO_EDGE_ZONE_TEST_TAG),
            direction = SimVerticalGestureDirection.UP,
            threshold = 64.dp,
            onTriggered = onOpenAudioBrowse
        )
    }
}

@Composable
internal fun SimDrawerHandle(
    dismissDirection: SimVerticalGestureDirection,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    dismissOnTap: Boolean = false
) {
    val baseModifier = if (testTag != null) {
        modifier.testTag(testTag)
    } else {
        modifier
    }

    Box(
        modifier = baseModifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        SimVerticalDragTrigger(
            modifier = Modifier
                .width(72.dp)
                .height(28.dp)
                .then(
                    if (dismissOnTap) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                    } else {
                        Modifier
                    }
                ),
            direction = dismissDirection,
            threshold = 56.dp,
            onTriggered = onDismiss
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun SimVerticalDragTrigger(
    modifier: Modifier = Modifier,
    direction: SimVerticalGestureDirection,
    threshold: Dp,
    onTriggered: () -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val thresholdPx = remember(threshold, density) { with(density) { threshold.toPx() } }
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = modifier.pointerInput(direction, thresholdPx) {
            var accumulatedDrag = 0f
            var hasTriggered = false

            detectVerticalDragGestures(
                onDragStart = {
                    accumulatedDrag = 0f
                    hasTriggered = false
                },
                onDragEnd = {
                    accumulatedDrag = 0f
                    hasTriggered = false
                },
                onDragCancel = {
                    accumulatedDrag = 0f
                    hasTriggered = false
                }
            ) { change, dragAmount ->
                if (hasTriggered) {
                    change.consume()
                    return@detectVerticalDragGestures
                }

                accumulatedDrag += dragAmount
                val crossed = when (direction) {
                    SimVerticalGestureDirection.UP -> accumulatedDrag <= -thresholdPx
                    SimVerticalGestureDirection.DOWN -> accumulatedDrag >= thresholdPx
                }

                if (crossed) {
                    change.consume()
                    hasTriggered = true
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTriggered()
                }
            }
        },
        contentAlignment = Alignment.Center
    ) {
        content?.invoke()
    }
}
