package com.smartsales.aitest.setup

// 文件：app/src/main/java/com/smartsales/aitest/setup/DeviceSetupRoute.kt
// 模块：:app
// 说明：为 DeviceSetup 标签提供占位 Compose 视图，待真实流程合入时补充
// 作者：创建于 2025-11-20

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun DeviceSetupRoute(
    modifier: Modifier = Modifier,
    onCompleted: () -> Unit
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "设备配网功能待补齐",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "当前仅作为占位条目，在完成配网流程后将替换为正式界面。",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            Button(
                onClick = onCompleted,
                modifier = Modifier.testTag(DeviceSetupTestTags.COMPLETE_BUTTON)
            ) {
                Text(text = "返回主页")
            }
        }
    }
}

object DeviceSetupTestTags {
    const val COMPLETE_BUTTON = "device_setup_complete"
}
