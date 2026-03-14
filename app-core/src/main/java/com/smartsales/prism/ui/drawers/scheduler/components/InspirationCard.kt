package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem
import com.smartsales.prism.ui.components.PrismButton
import com.smartsales.prism.ui.components.PrismButtonStyle
import com.smartsales.prism.ui.theme.TextPrimary

@Composable
fun InspirationCard(
    state: TimelineItem.Inspiration,
    onAskAI: () -> Unit,
    onToggleSelection: () -> Unit = {}
) {
    val purpleColor = Color(0xFFAF52DE)
    
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(if (state.isSelected) purpleColor else Color.Transparent, RoundedCornerShape(10.dp))
                        .border(1.dp, if (state.isSelected) Color.Transparent else purpleColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isSelected) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            } else {
                Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = purpleColor, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(text = state.title, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
            
            if (!state.isSelectionMode) {
                PrismButton(text = "Ask AI", onClick = onAskAI, style = PrismButtonStyle.GHOST, modifier = Modifier.height(32.dp))
            }
        }
    }
}
