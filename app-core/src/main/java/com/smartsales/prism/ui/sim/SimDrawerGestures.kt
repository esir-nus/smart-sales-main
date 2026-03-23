package com.smartsales.prism.ui.sim

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.BoxScope
import com.smartsales.prism.ui.PrismElevation
import kotlin.math.abs

internal const val SIM_SCHEDULER_EDGE_ZONE_TEST_TAG = "sim_scheduler_edge_zone"
internal const val SIM_AUDIO_EDGE_ZONE_TEST_TAG = "sim_audio_edge_zone"
internal const val SIM_AUDIO_HANDLE_TEST_TAG = "sim_audio_handle"

private val SIM_SCHEDULER_EDGE_BAND_FALLBACK_HEIGHT = 88.dp
private val SIM_AUDIO_EDGE_BAND_FALLBACK_HEIGHT = 28.dp
private val SIM_SCHEDULER_EDGE_BLEED = 24.dp
private val SIM_AUDIO_EDGE_LEAD = 12.dp
private val SIM_DRAWER_OPEN_DISTANCE_THRESHOLD = 40.dp
private val SIM_DRAWER_OPEN_VELOCITY_THRESHOLD = 1100.dp
private const val SIM_DRAWER_VERTICAL_DOMINANCE_RATIO = 1.35f

internal data class SimGestureAnchors(
    val headerBottomPx: Float,
    val composerTopPx: Float,
    val rootHeightPx: Float
)

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

internal fun buildSimGestureAnchors(
    headerBottomPx: Float?,
    composerTopPx: Float?,
    rootHeightPx: Float
): SimGestureAnchors? {
    val headerBottom = headerBottomPx ?: return null
    val composerTop = composerTopPx ?: return null
    if (rootHeightPx <= 0f) return null

    return SimGestureAnchors(
        headerBottomPx = headerBottom.coerceIn(0f, rootHeightPx),
        composerTopPx = composerTop.coerceIn(0f, rootHeightPx),
        rootHeightPx = rootHeightPx
    )
}

@Composable
internal fun rememberSimImeVisibility(): Boolean {
    val density = LocalDensity.current
    return WindowInsets.ime.getBottom(density) > 0
}

@Composable
internal fun BoxScope.SimDrawerEdgeGestureLayer(
    state: SimShellState,
    isImeVisible: Boolean,
    gestureAnchors: SimGestureAnchors? = null,
    onOpenScheduler: () -> Unit,
    onOpenAudioBrowse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val fallbackSchedulerHeightDp = SIM_SCHEDULER_EDGE_BAND_FALLBACK_HEIGHT
    val fallbackAudioHeightDp = SIM_AUDIO_EDGE_BAND_FALLBACK_HEIGHT
    val schedulerBleedPx = with(density) { SIM_SCHEDULER_EDGE_BLEED.toPx() }
    val audioLeadPx = with(density) { SIM_AUDIO_EDGE_LEAD.toPx() }
    val schedulerHeightDp = remember(gestureAnchors, density) {
        val zoneHeightPx = gestureAnchors
            ?.let { anchors ->
                (anchors.headerBottomPx + schedulerBleedPx)
                    .coerceAtLeast(with(density) { fallbackSchedulerHeightDp.toPx() })
                    .coerceAtMost(anchors.rootHeightPx)
            }
        zoneHeightPx?.let { with(density) { it.toDp() } } ?: fallbackSchedulerHeightDp
    }
    val audioZoneStartPx = remember(gestureAnchors, density) {
        gestureAnchors?.let { anchors ->
            val schedulerZoneBottomPx = (anchors.headerBottomPx + schedulerBleedPx)
                .coerceIn(0f, anchors.rootHeightPx)
            (anchors.composerTopPx - audioLeadPx)
                .coerceAtLeast(schedulerZoneBottomPx)
                .coerceIn(0f, anchors.rootHeightPx)
        }
    }
    val audioHeightDp = remember(audioZoneStartPx, gestureAnchors, density) {
        val zoneHeightPx = if (gestureAnchors != null && audioZoneStartPx != null) {
            (gestureAnchors.rootHeightPx - audioZoneStartPx).coerceAtLeast(0f)
        } else {
            with(density) { fallbackAudioHeightDp.toPx() }
        }
        with(density) { zoneHeightPx.toDp() }
    }

    if (canOpenSimSchedulerFromEdge(state)) {
        SimVerticalDragTrigger(
            modifier = modifier
                .zIndex(PrismElevation.Handles)
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(schedulerHeightDp)
                .testTag(SIM_SCHEDULER_EDGE_ZONE_TEST_TAG),
            direction = SimVerticalGestureDirection.DOWN,
            threshold = SIM_DRAWER_OPEN_DISTANCE_THRESHOLD,
            velocityThreshold = SIM_DRAWER_OPEN_VELOCITY_THRESHOLD,
            onTriggered = onOpenScheduler
        )
    }

    if (canOpenSimAudioFromEdge(state, isImeVisible)) {
        SimVerticalDragTrigger(
            modifier = modifier
                .zIndex(PrismElevation.Handles)
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(audioHeightDp)
                .testTag(SIM_AUDIO_EDGE_ZONE_TEST_TAG),
            direction = SimVerticalGestureDirection.UP,
            threshold = SIM_DRAWER_OPEN_DISTANCE_THRESHOLD,
            velocityThreshold = SIM_DRAWER_OPEN_VELOCITY_THRESHOLD,
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
            velocityThreshold = 1200.dp,
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
    velocityThreshold: Dp,
    onTriggered: () -> Unit,
    content: @Composable (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val thresholdPx = remember(threshold, density) { with(density) { threshold.toPx() } }
    val velocityThresholdPx = remember(velocityThreshold, density) {
        with(density) { velocityThreshold.toPx() }
    }
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = modifier.pointerInput(direction, thresholdPx, velocityThresholdPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var activePointerId: PointerId = down.id
                val velocityTracker = VelocityTracker().apply {
                    addPosition(down.uptimeMillis, down.position)
                }
                val startPosition = down.position
                val touchSlop = viewConfiguration.touchSlop
                var directionLocked = false
                var rejected = false
                var lastTotalDy = 0f

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == activePointerId }
                        ?: event.changes.firstOrNull()
                        ?: break

                    activePointerId = change.id
                    velocityTracker.addPosition(change.uptimeMillis, change.position)

                    if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                        if (!rejected) {
                            val velocityY = velocityTracker.calculateVelocity().y
                            if (shouldTriggerSimVerticalGesture(direction, lastTotalDy, velocityY, velocityThresholdPx)) {
                                triggerSimVerticalGesture(hapticFeedback, onTriggered)
                            }
                        }
                        break
                    }

                    val totalDx = change.position.x - startPosition.x
                    val totalDy = change.position.y - startPosition.y
                    lastTotalDy = totalDy
                    val absDx = abs(totalDx)
                    val absDy = abs(totalDy)

                    if (!directionLocked && !rejected && (absDx > touchSlop || absDy > touchSlop)) {
                        val matchesDirection = when (direction) {
                            SimVerticalGestureDirection.UP -> totalDy < 0f
                            SimVerticalGestureDirection.DOWN -> totalDy > 0f
                        }
                        val verticalDominant = absDy > absDx * SIM_DRAWER_VERTICAL_DOMINANCE_RATIO

                        if (matchesDirection && verticalDominant) {
                            directionLocked = true
                        } else {
                            rejected = true
                        }
                    }

                    if (directionLocked) {
                        change.consume()
                        val velocityY = velocityTracker.calculateVelocity().y
                        if (shouldTriggerSimVerticalGesture(direction, totalDy, velocityY, velocityThresholdPx) ||
                            crossedSimVerticalGestureDistance(direction, totalDy, thresholdPx)
                        ) {
                            triggerSimVerticalGesture(hapticFeedback, onTriggered)
                            break
                        }
                    }
                }
            }
        },
        contentAlignment = Alignment.Center
    ) {
        content?.invoke()
    }
}

private fun crossedSimVerticalGestureDistance(
    direction: SimVerticalGestureDirection,
    totalDy: Float,
    thresholdPx: Float
): Boolean = when (direction) {
    SimVerticalGestureDirection.UP -> totalDy <= -thresholdPx
    SimVerticalGestureDirection.DOWN -> totalDy >= thresholdPx
}

internal fun shouldTriggerSimVerticalGesture(
    direction: SimVerticalGestureDirection,
    totalDy: Float,
    velocityY: Float,
    velocityThresholdPx: Float
): Boolean = when (direction) {
    SimVerticalGestureDirection.UP ->
        totalDy < 0f && velocityY <= -velocityThresholdPx
    SimVerticalGestureDirection.DOWN ->
        totalDy > 0f && velocityY >= velocityThresholdPx
}

private fun triggerSimVerticalGesture(
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onTriggered: () -> Unit
) {
    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    onTriggered()
}
