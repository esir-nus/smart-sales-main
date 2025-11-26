package com.smartsales.aitest.ui

// 文件：app/src/main/java/com/smartsales/aitest/ui/HomeOverlayShell.kt
// 模块：:app
// 说明：Home 垂直叠层容器，托管 Home、音频库与设备管理页面并支持拖拽切换
// 作者：创建于 2025-11-26

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

enum class OverlayPage { Home, Audio, Device }

object HomeOverlayTestTags {
    const val HOME_LAYER = "overlay_home_layer"
    const val AUDIO_LAYER = "overlay_audio_layer"
    const val DEVICE_LAYER = "overlay_device_layer"
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
    BoxWithConstraints(modifier = modifier) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTag = HomeOverlayTestTags.AUDIO_LAYER }
                    .zIndex(if (currentPage == OverlayPage.Audio) 1f else 0f)
                    .offset(y = audioOffset),
            ) {
                audioContent()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTag = HomeOverlayTestTags.HOME_LAYER }
                    .zIndex(if (currentPage == OverlayPage.Home) 2f else 0f)
                    .offset(y = homeOffset),
            ) {
                homeContent()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTag = HomeOverlayTestTags.DEVICE_LAYER }
                    .zIndex(if (currentPage == OverlayPage.Device) 1f else 0f)
                    .offset(y = deviceOffset),
            ) {
                deviceManagerContent()
            }
        }
    }
}
