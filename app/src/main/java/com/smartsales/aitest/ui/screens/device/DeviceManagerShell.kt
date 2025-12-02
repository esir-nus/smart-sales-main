package com.smartsales.aitest.ui.screens.device

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/device/DeviceManagerShell.kt
// 模块：:app
// 说明：底部导航设备管理壳，直接承载真实 DeviceManagerRoute
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.smartsales.aitest.AiFeatureTestTags
import com.smartsales.aitest.devicemanager.DeviceManagerRoute

@Composable
fun DeviceManagerShell() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER)
    ) {
        DeviceManagerRoute(
            modifier = Modifier.fillMaxSize()
        )
    }
}
