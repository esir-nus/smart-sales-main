package com.smartsales.aitest.ui.shell

// 文件：app/src/main/java/com/smartsales/aitest/ui/shell/VerticalOverlayShell.kt
// 模块：:app
// 说明：Home/AudiFiles/DeviceManager 三层垂直 overlay 容器，支持拖拽、遮罩与手柄点击
// 作者：创建于 2025-11-29

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.AiFeatureTestTags

enum class HomeOverlayLayer { Audio, Home, Device }
private enum class DragDirection { Up, Down }

@Composable
fun VerticalOverlayShell(
    currentLayer: HomeOverlayLayer,
    onLayerChange: (HomeOverlayLayer) -> Unit,
    modifier: Modifier = Modifier,
    homeContent: @Composable () -> Unit,
    audioContent: @Composable () -> Unit,
    deviceContent: @Composable () -> Unit,
) {
    BackHandler(enabled = currentLayer != HomeOverlayLayer.Home) {
        onLayerChange(HomeOverlayLayer.Home)
    }

    BoxWithConstraints(modifier = modifier.testTag(AiFeatureTestTags.OVERLAY_SHELL)) {
        // Home 层始终存在，overlay 通过遮罩与 AnimatedVisibility 覆盖其上
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(AiFeatureTestTags.OVERLAY_HOME_LAYER),
        ) {
            homeContent()
            if (currentLayer == HomeOverlayLayer.Home) {
                OverlayHandle(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                .testTag(AiFeatureTestTags.OVERLAY_AUDIO_HANDLE),
                    onClick = { onLayerChange(HomeOverlayLayer.Audio) },
                    triggerDragDirection = DragDirection.Down,
                )
                OverlayHandle(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .testTag(AiFeatureTestTags.OVERLAY_DEVICE_HANDLE),
                    onClick = { onLayerChange(HomeOverlayLayer.Device) },
                    triggerDragDirection = DragDirection.Up,
                )
            }
        }

        val hasOverlay = currentLayer != HomeOverlayLayer.Home
        AnimatedVisibility(
            visible = hasOverlay,
            enter = slideInVertically(
                initialOffsetY = { 0 },
                animationSpec = tween(durationMillis = 150),
            ),
            exit = slideOutVertically(
                targetOffsetY = { 0 },
                animationSpec = tween(durationMillis = 150),
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable { onLayerChange(HomeOverlayLayer.Home) }
                    .testTag(AiFeatureTestTags.OVERLAY_BACKDROP),
            )
        }

        AnimatedVisibility(
            visible = currentLayer == HomeOverlayLayer.Audio,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 220),
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 200),
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .testTag(AiFeatureTestTags.OVERLAY_AUDIO_LAYER),
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        audioContent()
                        OverlayHandle(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            onClick = { onLayerChange(HomeOverlayLayer.Home) },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = currentLayer == HomeOverlayLayer.Device,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 220),
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 200),
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .testTag(AiFeatureTestTags.OVERLAY_DEVICE_LAYER),
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        deviceContent()
                        OverlayHandle(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp),
                            onClick = { onLayerChange(HomeOverlayLayer.Home) },
                        )
                    }
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
