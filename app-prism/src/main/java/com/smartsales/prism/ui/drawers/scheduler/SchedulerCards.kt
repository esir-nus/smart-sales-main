package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.foundation.background
import androidx.compose.foundation.border // Added import
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.QueryBuilder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Task Card Component
 * @see prism-ui-ux-contract.md §1.3 "Task Card"
 * 
 * ASCII Spec:
 * ┌───────────────────────────────────────────────────────────┐
 * │  ☐ 与张总会议 (A3项目)                           [⏰]    │
 * └───────────────────────────────────────────────────────────┘
 */
@Composable
fun TaskCard(
    state: TimelineItem.Task,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox (Visual only for now)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        if (state.isDone) Color(0xFFE0E0E0) else Color.Transparent, 
                        RoundedCornerShape(4.dp)
                    )
                    .border(
                        1.dp, 
                        if (state.isDone) Color.Transparent else Color(0xFFBDBDBD), 
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state.isDone) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Title
            Text(
                text = state.title,
                fontSize = 14.sp,
                textDecoration = if (state.isDone) TextDecoration.LineThrough else null,
                color = if (state.isDone) Color(0xFF999999) else Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
            
            // Alarm Icon
            if (state.hasAlarm) {
                Icon(
                    imageVector = Icons.Outlined.QueryBuilder,
                    contentDescription = "Alarm",
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Inspiration Card Component
 * @see prism-ui-ux-contract.md §1.3 "Inspiration Card"
 * 
 * ASCII Spec:
 * ┌─────────────────────────────────────────────────────────────┐
 * │  💡 灵感：研究竞品报价策略                        [问AI]    │
 * └─────────────────────────────────────────────────────────────┘
 * (Purple Tint for Analyst Mode)
 */
@Composable
fun InspirationCard(
    state: TimelineItem.Inspiration,
    onAskAI: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)), // Light Purple tint
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox (if in mode) or Bulb Icon
            if (state.isSelectionMode) {
                 Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            if (state.isSelected) Color(0xFF7B1FA2) else Color.Transparent, 
                            RoundedCornerShape(10.dp) // Circle for select
                        )
                        .border(
                            1.dp, 
                            if (state.isSelected) Color.Transparent else Color(0xFF7B1FA2), 
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF7B1FA2), // Purple
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Title
            Text(
                text = state.title,
                fontSize = 14.sp,
                color = Color(0xFF5D4037),
                modifier = Modifier.weight(1f)
            )
            
            // Ask AI Button
            if (!state.isSelectionMode) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.White,
                    modifier = Modifier
                        .clickable { onAskAI() }
                ) {
                    Text(
                        text = "问AI",
                        fontSize = 12.sp,
                        color = Color(0xFF7B1FA2),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

