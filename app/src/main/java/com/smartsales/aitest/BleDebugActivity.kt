package com.smartsales.aitest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartsales.feature.connectivity.BleTrafficEvent
import com.smartsales.feature.connectivity.BleTrafficObserver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Standalone BLE Debug HUD Activity
 * 
 * Launch via:
 *   adb shell am start -n com.smartsales.aitest/.BleDebugActivity
 * 
 * Or add launcher shortcut via manifest.
 * 
 * Features:
 * - Real-time TX/RX traffic monitoring
 * - Toggle between concise/verbose display
 * - Export logs as .txt via system share sheet
 */
class BleDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BleDebugScreen()
                }
            }
        }
    }
}

@Composable
fun BleDebugScreen() {
    val context = LocalContext.current
    val trafficEvents = remember { mutableStateListOf<BleTrafficEvent>() }
    var verboseMode by remember { mutableStateOf(false) }
    var isCollecting by remember { mutableStateOf(true) }

    // Collect BLE traffic events
    LaunchedEffect(isCollecting) {
        if (isCollecting) {
            BleTrafficObserver.events.collect { event ->
                trafficEvents.add(0, event)
                if (trafficEvents.size > 200) {
                    trafficEvents.removeAt(trafficEvents.lastIndex)
                }
            }
        }
    }

    // Export function for hardware collaboration
    fun exportLogs() {
        if (trafficEvents.isEmpty()) return
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val header = buildString {
            appendLine("=== SmartSales BLE 流量日志 ===")
            appendLine("导出时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("记录数量: ${trafficEvents.size}")
            appendLine("=============================")
            appendLine()
        }
        
        val body = trafficEvents.reversed().joinToString("\n\n") { it.formatVerbose() }
        val fullText = header + body
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "BLE_Traffic_$timestamp.txt")
            putExtra(Intent.EXTRA_TEXT, fullText)
        }
        context.startActivity(Intent.createChooser(intent, "导出 BLE 日志"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "BLE 流量调试",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "实时查看 TX/RX 数据包 · 支持导出",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Controls Row 1: Pause/Resume + Filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isCollecting = !isCollecting },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isCollecting) "暂停" else "继续")
            }
            OutlinedButton(
                onClick = { verboseMode = !verboseMode }
            ) {
                Text(if (verboseMode) "简洁" else "详细")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Controls Row 2: Clear + Export
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { trafficEvents.clear() },
                modifier = Modifier.weight(1f)
            ) {
                Text("清空")
            }
            Button(
                onClick = { exportLogs() },
                enabled = trafficEvents.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("导出 .txt")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Stats
        Text(
            text = "共 ${trafficEvents.size} 条记录 · ${if (isCollecting) "实时监听中..." else "已暂停"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Traffic Log
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (trafficEvents.isEmpty()) {
                    Text(
                        text = "等待 BLE 通信...\n\n" +
                            "在主应用中执行以下操作以查看流量：\n" +
                            "• 发送 WiFi 凭据 (SD#/PD#)\n" +
                            "• 查询设备网络状态\n" +
                            "• 读取热点信息\n\n" +
                            "点击「导出 .txt」可分享日志给硬件同事",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val displayText = trafficEvents.joinToString("\n") { event ->
                        if (verboseMode) event.formatVerbose() else event.formatForHud()
                    }
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Legend
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "图例",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "→ TX (App → Device)  |  ← RX (Device → App)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}
