package com.smartsales.aitest.ui

// 文件：app/src/main/java/com/smartsales/aitest/ui/HomeOverlayShell.kt
// 模块：:app
// 说明：Home 垂直叠层容器，托管 Home、音频库与设备管理页面并支持拖拽切换
// 作者：创建于 2025-11-26

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.testTag

enum class OverlayPage { Home, Audio, Device }

object HomeOverlayTestTags {
    const val HOME_LAYER = "overlay_home_layer"
    const val AUDIO_LAYER = "overlay_audio_layer"
    const val DEVICE_LAYER = "overlay_device_layer"
    const val ROOT = "overlay_shell_root"
}

@Composable
fun HomeOverlayShell(
    currentPage: OverlayPage,
    onPageChange: (OverlayPage) -> Unit,
    homeContent: @Composable () -> Unit,
    audioContent: @Composable () -> Unit,
    deviceManagerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 覆盖层根标签在 Activity 调用处下发，此处仅负责内部层级标签
    BoxWithConstraints(
        modifier = modifier
    ) {
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        var dragOffset by remember { mutableStateOf(0f) }
        val targetBase = when (currentPage) {
            OverlayPage.Home -> 0f
            OverlayPage.Audio -> containerHeightPx
            OverlayPage.Device -> -containerHeightPx
        }
        val animatedBase by animateFloatAsState(targetValue = targetBase, label = "overlayBase")
        val density = LocalDensity.current
        val threshold = containerHeightPx / 3f

        fun settlePage(offset: Float) {
            val next = when (currentPage) {
                OverlayPage.Home -> when {
                    offset > threshold -> OverlayPage.Audio
                    offset < -threshold -> OverlayPage.Device
                    else -> OverlayPage.Home
                }
                OverlayPage.Audio -> if (offset < -threshold) OverlayPage.Home else OverlayPage.Audio
                OverlayPage.Device -> if (offset > threshold) OverlayPage.Home else OverlayPage.Device
            }
            dragOffset = 0f
            if (next != currentPage) {
                onPageChange(next)
            }
        }

        val totalBase = animatedBase + dragOffset
        val homeOffset = with(density) { totalBase.toDp() }
        val audioOffset = with(density) { (totalBase - containerHeightPx).toDp() }
        val deviceOffset = with(density) { (totalBase + containerHeightPx).toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentPage) {
                    detectDragGestures(
                        onDragEnd = { settlePage(dragOffset) },
                        onDragCancel = { dragOffset = 0f },
                    ) { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y
                    }
                }
        ) {
            OverlayLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(HomeOverlayTestTags.AUDIO_LAYER)
                    .zIndex(if (currentPage == OverlayPage.Audio) 1f else 0f)
                    .offset(y = audioOffset),
                isActive = currentPage == OverlayPage.Audio
            ) {
                audioContent()
            }
            OverlayLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(HomeOverlayTestTags.HOME_LAYER)
                    .zIndex(if (currentPage == OverlayPage.Home) 2f else 0f)
                    .offset(y = homeOffset),
                isActive = currentPage == OverlayPage.Home
            ) {
                homeContent()
            }
            OverlayLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(HomeOverlayTestTags.DEVICE_LAYER)
                    .zIndex(if (currentPage == OverlayPage.Device) 1f else 0f)
                    .offset(y = deviceOffset),
                isActive = currentPage == OverlayPage.Device
            ) {
                deviceManagerContent()
            }
        }
    }
}

/** 单层容器，增加柔和背景、抓手与模糊效果，贴近 React 叠层视觉。 */
@Composable
private fun OverlayLayer(
    modifier: Modifier,
    isActive: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isActive) {
                        listOf(Color(0xFFF7FAFF), Color.White)
                    } else {
                        listOf(Color(0xFFF3F5FB), Color(0xFFE9EDF5))
                    }
                )
            )
            .then(if (isActive) Modifier else Modifier.blur(10.dp)),
    ) {
        OverlayHandle(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            isActive = isActive
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 18.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            content()
        }
    }
}

/** 顶部抓手条，方便用户感知拖拽切换。 */
@Composable
private fun OverlayHandle(
    modifier: Modifier = Modifier,
    isActive: Boolean
) {
    Box(
        modifier = modifier
            .width(54.dp)
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isActive) {
                        listOf(Color(0xFF4B7BEC).copy(alpha = 0.35f), Color(0xFF4B7BEC).copy(alpha = 0.18f))
                    } else {
                        listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.06f))
                    }
                )
            )
    )
}
