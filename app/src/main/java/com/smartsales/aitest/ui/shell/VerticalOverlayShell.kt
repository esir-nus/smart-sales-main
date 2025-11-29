package com.smartsales.aitest.ui.shell

// 文件：app/src/main/java/com/smartsales/aitest/ui/shell/VerticalOverlayShell.kt
// 模块：:app
// 说明：Home/AudiFiles/DeviceManager 三层垂直 overlay 容器，支持拖拽、遮罩与手柄点击
// 作者：创建于 2025-11-29

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.AiFeatureTestTags
import kotlin.math.abs
import kotlin.math.roundToInt

enum class HomeOverlayLayer { Audio, Home, Device }
private enum class DragDirection { Up, Down }

@Composable
fun VerticalOverlayShell(
    currentLayer: HomeOverlayLayer,
    onLayerChange: (HomeOverlayLayer) -> Unit,
    enableDrag: Boolean,
    modifier: Modifier = Modifier,
    homeContent: @Composable () -> Unit,
    audioContent: @Composable () -> Unit,
    deviceContent: @Composable () -> Unit,
) {
    BackHandler(enabled = currentLayer != HomeOverlayLayer.Home) {
        onLayerChange(HomeOverlayLayer.Home)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag(AiFeatureTestTags.OVERLAY_SHELL)
    ) {
        val heightPx = with(LocalDensity.current) { constraints.maxHeight.toFloat() }
        val targetForLayer: (HomeOverlayLayer) -> Float = { layer ->
            when (layer) {
                HomeOverlayLayer.Home -> 0f
                HomeOverlayLayer.Audio -> heightPx
                HomeOverlayLayer.Device -> -heightPx
            }
        }
        val dragOffset = remember { mutableStateOf(targetForLayer(currentLayer)) }
        val animatedOffset = animateFloatAsState(
            targetValue = dragOffset.value,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "overlay_offset"
        )

        LaunchedEffect(currentLayer, heightPx) {
            dragOffset.value = targetForLayer(currentLayer)
        }

        val threshold = heightPx * 0.18f
        val backdropAlpha = (abs(animatedOffset.value) / heightPx).coerceIn(0f, 0.35f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { base ->
                    if (!enableDrag) return@let base
                    base.pointerInput(heightPx) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragOffset.value = animatedOffset.value
                            },
                            onVerticalDrag = { _, dragAmount ->
                                val next = (dragOffset.value + dragAmount).coerceIn(-heightPx, heightPx)
                                dragOffset.value = next
                            },
                            onDragEnd = {
                                val value = dragOffset.value
                                val nextLayer = when {
                                    value > threshold -> HomeOverlayLayer.Audio
                                    value < -threshold -> HomeOverlayLayer.Device
                                    else -> HomeOverlayLayer.Home
                                }
                                dragOffset.value = targetForLayer(nextLayer)
                                if (nextLayer != currentLayer) onLayerChange(nextLayer)
                            },
                            onDragCancel = {
                                dragOffset.value = targetForLayer(currentLayer)
                            }
                        )
                    }
                }
        ) {
            // Middle layer (Home) counter-moves for static feel
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, (-animatedOffset.value).roundToInt()) }
                    .testTag(AiFeatureTestTags.OVERLAY_HOME_LAYER),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    homeContent()
                    OverlayHandle(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .testTag(AiFeatureTestTags.OVERLAY_AUDIO_HANDLE),
                        onClick = { onLayerChange(HomeOverlayLayer.Audio) },
                        triggerDragDirection = DragDirection.Down,
                    )
                    OverlayHandle(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .testTag(AiFeatureTestTags.OVERLAY_DEVICE_HANDLE),
                        onClick = { onLayerChange(HomeOverlayLayer.Device) },
                        triggerDragDirection = DragDirection.Up,
                    )
                }
            }

            if (backdropAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = backdropAlpha))
                        .clickable { onLayerChange(HomeOverlayLayer.Home) }
                        .testTag(AiFeatureTestTags.OVERLAY_BACKDROP),
                )
            }

            // Top drawer (Audio)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, (animatedOffset.value - heightPx).roundToInt()) }
                    .padding(horizontal = 12.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                    .testTag(AiFeatureTestTags.OVERLAY_AUDIO_LAYER),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    audioContent()
                    OverlayHandle(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp),
                        onClick = { onLayerChange(HomeOverlayLayer.Home) },
                    )
                }
            }

            // Bottom drawer (Device)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, (animatedOffset.value + heightPx).roundToInt()) }
                    .padding(horizontal = 12.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .testTag(AiFeatureTestTags.OVERLAY_DEVICE_LAYER),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    deviceContent()
                    OverlayHandle(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp),
                        onClick = { onLayerChange(HomeOverlayLayer.Home) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayHandle(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    triggerDragDirection: DragDirection? = null,
) {
    val dragAccumulator = remember { mutableStateOf(0f) }
    val dragThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    val dragModifier = triggerDragDirection?.let { direction ->
        Modifier.pointerInput(direction) {
            detectVerticalDragGestures(
                onVerticalDrag = { _, dragAmount ->
                    dragAccumulator.value = dragAccumulator.value + dragAmount
                },
                onDragEnd = {
                    val delta = dragAccumulator.value
                    val passed = when (direction) {
                        DragDirection.Down -> delta > dragThreshold
                        DragDirection.Up -> delta < -dragThreshold
                    }
                    dragAccumulator.value = 0f
                    if (passed) onClick()
                },
                onDragCancel = { dragAccumulator.value = 0f },
            )
        }
    } ?: Modifier

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(dragModifier)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 6.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                ),
        )
    }
}
