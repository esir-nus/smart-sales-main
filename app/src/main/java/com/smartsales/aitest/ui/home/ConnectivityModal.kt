package com.smartsales.aitest.ui.home

// 文件：app/src/main/java/com/smartsales/aitest/ui/home/ConnectivityModal.kt
// 模块：:app
// 说明：连接状态弹窗 — "Executive Control" 风格
// 作者：创建于 2026-01-30 (Chapter 5 VI Guide)

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.smartsales.aitest.ui.components.PrismButton
import com.smartsales.aitest.ui.components.PrismButtonSecondary
import com.smartsales.aitest.ui.components.PrismSurface
import com.smartsales.aitest.ui.theme.AccentGreen
import com.smartsales.aitest.ui.theme.AccentRed
import com.smartsales.aitest.ui.theme.BackgroundSurface
import com.smartsales.aitest.ui.theme.TextMuted
import com.smartsales.aitest.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectivityModal(
    isConnected: Boolean,
    onDismissRequest: () -> Unit,
    onConnectClick: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PrismSurface(
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp),
            backgroundColor = BackgroundSurface.copy(alpha = 0.98f),
            elevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                ConnectionStatusIcon(isConnected)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status Text
                Text(
                    text = if (isConnected) "System Online" else "Connection Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isConnected) 
                        "Data link active. AI Core initialized." 
                    else 
                        "Connect to Prism Core to enable AI features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    PrismButtonSecondary(
                        text = "Close",
                        onClick = onDismissRequest
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    if (!isConnected) {
                        PrismButton(
                            text = "Connect",
                            onClick = onConnectClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusIcon(isConnected: Boolean) {
    val icon = if (isConnected) Icons.Default.CheckCircle else Icons.Rounded.Wifi
    val color = if (isConnected) AccentGreen else AccentRed
    
    PrismSurface(
        modifier = Modifier.size(80.dp),
        backgroundColor = color.copy(alpha = 0.1f),
        shape = com.smartsales.aitest.ui.theme.GlassCardShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}
