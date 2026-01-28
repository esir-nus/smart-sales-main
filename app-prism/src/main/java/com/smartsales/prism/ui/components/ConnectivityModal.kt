package com.smartsales.prism.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Connectivity Modal States
 */
private enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    UPDATE_FOUND,
    UPDATING,
    WIFI_MISMATCH // v2.5
}

/**
 * Connectivity Modal (Truncated Onboarding)
 * @see prism-ui-ux-contract.md §1.6.1
 */
@Composable
fun ConnectivityModal(
    onDismiss: () -> Unit
) {
    // Fake State Machine for Visual Verification
    var state by remember { mutableStateOf(ConnectionState.CONNECTED) }
    var batteryLevel by remember { mutableIntStateOf(85) }
    
    // Toggle for verification: Reconnect -> Mismatch -> Connected -> Mismatch...
    var reconnectAttempts by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {}, // Intercept clicks
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dynamic Content based on State
                AnimatedContent(targetState = state, label = "StateTransition") { currentState ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        when (currentState) {
                            ConnectionState.CONNECTED -> ConnectedView(
                                battery = batteryLevel,
                                onUnbind = { state = ConnectionState.DISCONNECTED },
                                onCheckUpdate = { state = ConnectionState.UPDATE_FOUND }
                            )
                            ConnectionState.DISCONNECTED -> DisconnectedView(
                                onReconnect = {
                                    reconnectAttempts++
                                    // Debug Cycle: Even -> Connected, Odd -> Mismatch
                                    if (reconnectAttempts % 2 != 0) {
                                        state = ConnectionState.WIFI_MISMATCH
                                    } else {
                                        state = ConnectionState.CONNECTED
                                    }
                                }
                            )
                            ConnectionState.UPDATE_FOUND -> UpdateFoundView(
                                onSync = { state = ConnectionState.UPDATING },
                                onLater = { state = ConnectionState.CONNECTED }
                            )
                            ConnectionState.UPDATING -> UpdatingView(
                                onComplete = { state = ConnectionState.CONNECTED }
                            )
                            ConnectionState.WIFI_MISMATCH -> WifiMismatchView(
                                onUpdate = { state = ConnectionState.CONNECTED },
                                onIgnore = { state = ConnectionState.CONNECTED }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// --- Sub-Views ---

@Composable
private fun ConnectedView(
    battery: Int,
    onUnbind: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    // Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Icon(
        imageVector = Icons.Default.Bluetooth,
        contentDescription = "Connected",
        tint = Color(0xFF4CAF50).copy(alpha = alpha),
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape)
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("SmartBadge Pro", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text("ID: 8842 • 🔋 $battery%", fontSize = 14.sp, color = Color(0xFFAAAAAA))
    Text("v1.2.0", fontSize = 12.sp, color = Color.Gray)

    Spacer(modifier = Modifier.height(32.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(
            onClick = onUnbind,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
        ) {
            Text("⚡ 断开连接")
        }

        Button(
            onClick = onCheckUpdate,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
        ) {
            Text("🔄 检查更新")
        }
    }
}

@Composable
private fun DisconnectedView(
    onReconnect: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.BluetoothDisabled,
        contentDescription = "Disconnected",
        tint = Color.Gray,
        modifier = Modifier
            .size(64.dp)
            .background(Color.Gray.copy(alpha = 0.1f), CircleShape)
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("🔴 离线 (Offline)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text("请确保设备在附近并已开机", fontSize = 14.sp, color = Color(0xFFAAAAAA))

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onReconnect,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text("连接设备 (Reconnect)")
    }
}

@Composable
private fun UpdateFoundView(
    onSync: () -> Unit,
    onLater: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Download,
        contentDescription = "Update",
        tint = Color(0xFFFFC107),
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFFFFC107).copy(alpha = 0.1f), CircleShape)
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("发现新版本 v1.3", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text("包含重要安全修复与性能提升", fontSize = 14.sp, color = Color(0xFFAAAAAA))

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onSync,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Text("立即同步 (Sync Now)")
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    TextButton(onClick = onLater) {
        Text("稍后", color = Color.Gray)
    }
}

@Composable
private fun UpdatingView(
    onComplete: () -> Unit
) {
    // Mock Progress
    var progress by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(50)
            progress += 0.02f
        }
        onComplete()
    }

    CircularProgressIndicator(
        progress = { progress },
        modifier = Modifier.size(64.dp),
        color = Color(0xFF2196F3),
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("正在安装... ${(progress * 100).toInt()}%", fontSize = 16.sp, color = Color.White)
    Text("请保持设备连接", fontSize = 12.sp, color = Color.Gray)
}

@Composable
private fun WifiMismatchView(
    onUpdate: () -> Unit,
    onIgnore: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = "WiFi Mismatch",
        tint = Color(0xFFFFC107), // Amber
        modifier = Modifier
            .size(64.dp)
            .background(Color(0xFFFFC107).copy(alpha = 0.1f), CircleShape)
            .padding(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text("网络环境已变更", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text("检测到徽章 WiFi 与当前网络不匹配", fontSize = 14.sp, color = Color(0xFFAAAAAA))

    Spacer(modifier = Modifier.height(24.dp))

    // State for Inputs
    var ssid by remember { mutableStateOf("Office_5G") }
    var password by remember { mutableStateOf("") }

    // WiFi Inputs
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2B38), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SSID Input
        Column {
            Text("WiFi Name (SSID)", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = ssid,
                onValueChange = { ssid = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = Color.Gray
                )
            )
        }
        
        // Password Input
        Column {
            Text("Password", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                placeholder = { Text("Enter Password", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = Color.Gray
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(
            onClick = onIgnore,
            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
        ) {
            Text("忽略")
        }

        Button(
            onClick = onUpdate,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
            modifier = Modifier.weight(1f)
        ) {
            Text("更新配置 (Update)")
        }
    }
}
