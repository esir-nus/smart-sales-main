package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ExpandLess // Added
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

// ==========================================
// State Models
// ==========================================

data class ChatMessage(
    val text: String,
    val isSystem: Boolean // true=AI, false=User
)

// ==========================================
// UI Components
// ==========================================

/**
 * Conflict Card Component (Natural Language Resolution)
 * @see prism-ui-ux-contract.md §1.3 "Conflict Card"
 */
@Composable

fun ConflictCard(
    state: TimelineItem.Conflict,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onRemove: () -> Unit = {},
    viewModel: ConflictViewModel = hiltViewModel()
) {
    // Local state for the mock interaction
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Mock Logic: Initialize chat on expand
    LaunchedEffect(isExpanded) {
        if (isExpanded && messages.isEmpty()) {
            // Initial AI Message
            messages = listOf(
                ChatMessage(
                    text = "发现日程冲突。'团队午餐' (12:00) 优先级较低。建议保留 '审查预算' (12:00)。是否自动调整午餐时间？",
                    isSystem = true
                )
            )
        }
    }

    // Breathing Animation for Expanded State
    val infiniteTransition = rememberInfiniteTransition()
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val glowColor = if (isExpanded) Color(0xFFD32F2F).copy(alpha = breathingAlpha) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { 
                onExpandToggle() // Allow toggle (Expand/Collapse)
            }
            .border(
                width = if (isExpanded) 2.dp else 0.dp,
                color = glowColor,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)), // Light Red tint
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Header Row (Always Visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F), // Red
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = state.conflictText,
                    fontSize = 14.sp,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFFD32F2F)
                )
            }
            
            // Expanded Content: Mini Chat
            if (isExpanded) {
                Divider(color = Color(0xFFFFCDD2).copy(alpha = 0.5f))
                
                // Chat Area (Column, NOT LazyColumn to avoid nested scroll)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Messages
                    messages.forEach { msg ->
                        ChatBubble(message = msg)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    if (isResolving) {
                         Text(
                            text = "Processing...",
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F).copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
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

                        // Send / Mic Button
                        Icon(
                            imageVector = if (inputText.isEmpty()) Icons.Default.Mic else Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    if (inputText.isNotEmpty()) {
                                        val userText = inputText
                                        inputText = ""
                                        
                                        // 1. Add User Message
                                        messages = messages + ChatMessage(userText, isSystem = false)
                                        isResolving = true
                                        
                                        // 2. Fake I/O: 延迟在FakeConflictResolutionService
                                        scope.launch {
                                            val result = viewModel.resolveConflict(userText)
                                            messages = messages + ChatMessage(result.reply, isSystem = true)
                                            isResolving = false
                                            
                                            if (result.resolved) {
                                                if (isExpanded) onExpandToggle()
                                                onRemove()
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isSystem) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (message.isSystem) 2.dp else 12.dp,
                bottomEnd = if (message.isSystem) 12.dp else 2.dp
            ),
            color = if (message.isSystem) Color.White else Color(0xFFD32F2F), // White for AI, Red for User (on Pink bg)
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(10.dp),
                fontSize = 13.sp,
                color = if (message.isSystem) Color.Black else Color.White
            )
        }
    }
}
