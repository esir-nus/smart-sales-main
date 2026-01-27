package com.smartsales.feature.chat.prism.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 🚧 PRISM SKELETON (MOL-10)
 * Registry Status: ✅ Verified Placeholder
 *
 * THIS IS A PLACEHOLDER.
 * Intended for Phase 1.9 verification only.
 * Awaiting replacement in Phase 3 (Real Implementation).
 *
 * Registry Contract: `isConnected: Bool`, `deviceName: String?`, `onConnect: Fn`
 */
@Composable
fun BleStatusIndicator(
    isConnected: Boolean,
    deviceName: String?,
    onConnect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                if (isConnected) Color(0xFF1E3A2A) else Color(0xFF3A1E1E), 
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (isConnected) "🟢 $deviceName" else "🔴 Disconnected",
            color = Color.White,
            fontSize = 12.sp
        )
        
        if (!isConnected) {
            TextButton(onClick = onConnect) {
                Text("Connect", fontSize = 12.sp, color = Color(0xFF4FC3F7))
            }
        }
    }
}
