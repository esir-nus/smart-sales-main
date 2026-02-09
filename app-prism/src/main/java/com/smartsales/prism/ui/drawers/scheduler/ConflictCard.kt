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
import androidx.compose.material.icons.outlined.ExpandLess
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
import com.smartsales.prism.ui.components.PrismCard
import com.smartsales.prism.ui.theme.*
import com.smartsales.prism.domain.memory.ScheduleItem
import com.smartsales.prism.domain.scheduler.ConflictResolution
import com.smartsales.prism.data.scheduler.RealConflictResolver
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors
import com.smartsales.prism.di.HiltComponentProvider

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
 * Conflict Card Component (Rewrite with Real Context)
 * @see prism-ui-ux-contract.md §1.3 \"Conflict Card\"
 */
@Composable
fun ConflictCard(
    taskA: ScheduleItem,
    taskB: ScheduleItem,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onResolve: (ConflictResolution) -> Unit
) {
    // Access RealConflictResolver via Hilt
    val context = LocalContext.current
    val resolver = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            HiltComponentProvider::class.java
        ).conflictResolver()
    }
    
    // Local state needed for interaction
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    // Initialize chat on expand
    LaunchedEffect(isExpanded) {
        if (isExpanded && messages.isEmpty()) {
            messages = listOf(
                ChatMessage(
                    text = "发现日程重叠：'${taskA.title}' (${formatTime(taskA.scheduledAt)}) 和 '${taskB.title}' (${formatTime(taskB.scheduledAt)})。如果两个都要保留也没问题，或者你想调整哪一个？",
                    isSystem = true
                )
            )
        }
    }

    // Breathing Animation for Expanded State
    val infiniteTransition = rememberInfiniteTransition()
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val borderColor = if (isExpanded) AccentDanger.copy(alpha = breathingAlpha) else AccentDanger

    PrismCard(
        onClick = onExpandToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = GlassCardShape // 20.dp
    ) {
        // Red/Orange Tint Background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(AccentDanger.copy(alpha = 0.08f)) // Subtle tint
        )

        // Red Border Overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, borderColor, GlassCardShape)
        )

        Column {
            // Header Row (Always Visible)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Row 1: Warning Icon + Label + Expand Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = AccentDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "时间重叠",
                        fontSize = 12.sp,
                        color = AccentDanger,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = AccentDanger.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Row 2: Task A Details
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = taskA.title,
                        fontSize = 15.sp, // Slightly larger
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${formatTime(taskA.scheduledAt)} (${taskA.durationMinutes}m)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Row 3: Task B Details
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = taskB.title,
                        fontSize = 15.sp, // Slightly larger
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${formatTime(taskB.scheduledAt)} (${taskB.durationMinutes}m)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                // Row 4: Overlap Duration (Calculated)
                val startA = taskA.scheduledAt
                val endA = startA + taskA.durationMinutes * 60_000L
                val startB = taskB.scheduledAt
                val endB = startB + taskB.durationMinutes * 60_000L
                val overlapStart = maxOf(startA, startB)
                val overlapEnd = minOf(endA, endB)
                val overlapMillis = maxOf(0L, overlapEnd - overlapStart)
                val overlapMinutes = overlapMillis / 60_000L
                
                if (overlapMinutes > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "重叠 $overlapMinutes 分钟",
                        fontSize = 12.sp,
                        color = AccentDanger,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Expanded Content: Mini Chat
            if (isExpanded) {
                 HorizontalDivider(color = AccentDanger.copy(alpha = 0.2f))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    messages.forEach { msg ->
                        ChatBubble(message = msg)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    if (isResolving) {
                         Text(
                            text = "Processing...",
                            fontSize = 12.sp,
                            color = AccentDanger.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .border(1.dp, AccentDanger.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty()) {
                                    Text("Reply...", color = TextMuted, fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = if (inputText.isEmpty()) Icons.Default.Mic else Icons.Default.Send,
                            contentDescription = "Send",
                            tint = AccentDanger,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    if (inputText.isNotEmpty()) {
                                        val userText = inputText
                                        inputText = ""
                                        
                                        messages = messages + ChatMessage(userText, isSystem = false)
                                        isResolving = true
                                        
                                        scope.launch {
                                            val resolution = resolver.resolve(userText, taskA, taskB)
                                            messages = messages + ChatMessage(resolution.reply, isSystem = true)
                                            isResolving = false
                                            
                                            // Execute all actions via callback
                                            onResolve(resolution)
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
            color = if (message.isSystem) BackgroundSurface else AccentDanger, 
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(10.dp),
                fontSize = 13.sp,
                color = if (message.isSystem) TextPrimary else Color.White
            )
        }
    }
}

/**
 * 格式化时间戳为可读格式
 */
private fun formatTime(epochMillis: Long): String {
    val instant = java.time.Instant.ofEpochMilli(epochMillis)
    val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        .withZone(java.time.ZoneId.systemDefault())
    return formatter.format(instant)
}
