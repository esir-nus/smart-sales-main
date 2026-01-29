package com.smartsales.prism.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Thinking Box (Accordion)
 * 
 * Collapses to a minimal pulse when streaming.
 * Expands to show chain-of-thought.
 */
@Composable
fun ThinkingBox(
    content: String,
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    // Default to Expanded if thinking, collapsed if complete? 
    // Spec says: "Collapse is enough... user can unfold"
    // So start expanded while thinking, then collapse? Or always collapsible.
    var isExpanded by remember { mutableStateOf(!isComplete) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(Color(0xFF2A2A3D), RoundedCornerShape(12.dp))
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .clickable { isExpanded = !isExpanded }
            .padding(12.dp)
    ) {
        // Header / Insight Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isComplete) "✨ 洞察已生成" else "🧠 深度思考中...",
                color = Color.Cyan,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Text(
                text = if (isExpanded) "▼" else "▶", // Simple Arrow
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        // Expanded Content
        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace, // Code font for thought process
                lineHeight = 18.sp
            )
        }
    }
}
