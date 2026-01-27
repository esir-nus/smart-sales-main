package com.smartsales.feature.chat.prism.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.domain.prism.core.Mode

/**
 * 🚧 PRISM SKELETON (MOL-07)
 * Registry Status: ✅ Verified Placeholder
 *
 * THIS IS A PLACEHOLDER.
 * Intended for Phase 1.9 verification only.
 * Awaiting replacement in Phase 3 (Real Implementation).
 *
 * Registry Contract: `currentMode: Mode`, `onModeSwitch: Fn`
 */
@Composable
fun ModeToggleBar(
    currentMode: Mode,
    onModeSwitch: (Mode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Mode.entries.forEach { mode ->
            val isSelected = mode == currentMode
            val (icon, label) = when (mode) {
                Mode.COACH -> "💬" to "Coach"
                Mode.ANALYST -> "🔬" to "Analyst"
                Mode.SCHEDULER -> "📅" to "Scheduler"
            }

            TextButton(
                onClick = { onModeSwitch(mode) },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) Color(0xFF3A3A5A) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
            ) {
                Text(
                    text = "$icon $label",
                    color = if (isSelected) Color.White else Color(0xFF888888),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
