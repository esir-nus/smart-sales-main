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
import androidx.compose.material.icons.filled.Mic // Added
import androidx.compose.material.icons.filled.Send // Added
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // Added
import androidx.compose.runtime.mutableStateOf // Added
import androidx.compose.runtime.remember // Added
import androidx.compose.runtime.setValue // Added
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // Added
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex // Added

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
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit,
    onClick: () -> Unit,
    onReschedule: (String) -> Unit = {}, // Added callback
    onToggleDone: () -> Unit = {}
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                onExpandToggle()
                onClick() 
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Header Row (Always Visible)
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onToggleDone() }
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
                
                // Alarm Icon or Chevron
                if (state.hasAlarm) {
                    Icon(
                        imageVector = Icons.Outlined.QueryBuilder,
                        contentDescription = "Alarm",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                     // Show chevron if expandable and no alarm (or always show chevron if expanded?)
                     // For now, keep alarm logic but maybe add rotation if needed. 
                     // Let's just keep alarm if present.
                }
            }
            
            // Expanded Content
            androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp, end = 12.dp, bottom = 12.dp) // Align with text start
                ) {
                    Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(bottom = 8.dp))
                    
                    // Date
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("📅 ", fontSize = 12.sp)
                        Text(
                            text = "日期: ${state.dateRange}", 
                            fontSize = 13.sp, 
                            color = Color(0xFF666666)
                        )
                    }
                    
                    // Location
                    state.location?.let { loc ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("📍 ", fontSize = 12.sp)
                            Text(
                                text = "地点: $loc", 
                                fontSize = 13.sp, 
                                color = Color(0xFF666666)
                            )
                        }
                    }
                    
                    // Notes
                    state.notes?.let { note ->
                         Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("📝 ", fontSize = 12.sp)
                            Text(
                                text = "备注: $note", 
                                fontSize = 13.sp, 
                                color = Color(0xFF666666)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Chat Input Placeholder (Visual Mock for now, pending full Chat Interface)
                    // Chat Input (Real UI) - Only show if pending
                    if (!state.isDone) {
                        var inputText by remember { mutableStateOf("") }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (inputText.isEmpty()) {
                                        Text("Reply...", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
    
                            Icon(
                                imageVector = if (inputText.isEmpty()) Icons.Default.Mic else Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color(0xFF1976D2), // Publisher Blue
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        if (inputText.isNotEmpty()) {
                                            onReschedule(inputText)
                                            inputText = ""
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
            
            // Processing Overlay
            if (state.processingStatus != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.8f))
                        .zIndex(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.processingStatus ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFF1976D2),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
    onAskAI: () -> Unit,
    onToggleSelection: () -> Unit = {} // New callback for selection toggle
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (state.isSelectionMode) {
                    onToggleSelection()
                }
            },
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
            
            // Ask AI Button (Only in normal mode)
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

