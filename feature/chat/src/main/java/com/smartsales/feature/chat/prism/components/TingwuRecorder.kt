package com.smartsales.feature.chat.prism.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 🚧 PRISM SKELETON (MOL-08)
 * Registry Status: ✅ Verified Placeholder
 *
 * THIS IS A PLACEHOLDER.
 * Intended for Phase 1.9 verification only.
 * Awaiting replacement in Phase 3 (Real Implementation).
 *
 * Registry Contract: `isRecording: Bool`, `amplitude: Float`, `onStart: Fn`, `onStop: Fn`
 */
@Composable
fun TingwuRecorder(
    isRecording: Boolean,
    amplitude: Float,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    if (isRecording) {
        // Recording State
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2A2A40), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mock Waveform (Amplitude Visual)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.Red, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Listening... (Amp: $amplitude)", color = Color.White, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444))
            ) {
                Text("Stop")
            }
        }
    } else {
        // Idle State
        IconButton(onClick = onStart) {
            Text("🎙️", fontSize = 24.sp) // Visual Placeholder
        }
    }
}
