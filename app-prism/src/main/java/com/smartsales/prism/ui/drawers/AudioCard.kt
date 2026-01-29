package com.smartsales.prism.ui.drawers

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Audio Card Hub System
 * Facade switching between Compact (List) and Expanded (Hub) views.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AudioCard(
    item: AudioItemState,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onStarClick: (String) -> Unit,
    onAskAi: (String) -> Unit,
    onTranscribe: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String) -> Unit
) {
    // 1. Shake Animation State
    val shakeAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // 2. Expansion Logic: Only expand if Transcribed
    val isExpandable = item.status == AudioStatus.TRANSCRIBED
    
    // 3. Swipe Logic (Only active when NOT expanded)
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.25f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { // L->R: Transcribe
                    if (item.status == AudioStatus.PENDING) {
                        onTranscribe(item.id)
                        false // Snap back after triggering action
                    } else {
                        false
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> { // R->L: Delete
                    onDelete(item.id)
                    false // Let parent handle removal/confirmation dialog, don't dismiss visually instantly
                }
                else -> false
            }
        }
    )
    
    // Swipe Wrapper
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isExpanded && item.status == AudioStatus.PENDING, // Only allow transcribe swipe if pending
        enableDismissFromEndToStart = !isExpanded, // Always allow delete swipe if collapsed
        backgroundContent = {
           val direction = dismissState.dismissDirection
           val color = when (direction) {
               SwipeToDismissBoxValue.StartToEnd -> Color(0xFFE8F5E9) // Green (Transcribe)
               SwipeToDismissBoxValue.EndToStart -> Color(0xFFFFEBEE) // Red (Delete)
               else -> Color.Transparent
           }
           val alignment = when (direction) {
               SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
               SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
               else -> Alignment.Center
           }
           val icon = when (direction) {
               SwipeToDismissBoxValue.StartToEnd -> Icons.Outlined.AutoAwesome // Magic/Transcribe
               SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
               else -> Icons.Default.Delete
           }
           val tint = when (direction) {
               SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
               SwipeToDismissBoxValue.EndToStart -> Color(0xFFD32F2F)
               else -> Color.Transparent
           }
           
           Box(
               modifier = Modifier
                   .fillMaxSize()
                   .background(color, RoundedCornerShape(12.dp))
                   .padding(horizontal = 20.dp),
               contentAlignment = alignment
           ) {
               Icon(
                   imageVector = icon,
                   contentDescription = null,
                   tint = tint
               )
           }
        },
        content = {
            // Shared Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = shakeAnim.value.dp) // Apply Shake
                    .shadow(
                        elevation = if (isExpanded) 8.dp else 2.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = Color(0x1A000000)
                    )
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    .combinedClickable(
                        onClick = {
                            if (isExpandable) {
                                onClick()
                            } else {
                                // Trigger Shake (Reject)
                                scope.launch {
                                    shakeAnim.snapTo(0f)
                                    shakeAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = androidx.compose.animation.core.keyframes {
                                            durationMillis = 400
                                            0f at 0
                                            (-10f) at 50
                                            10f at 100
                                            (-10f) at 150
                                            5f at 200
                                            0f at 250
                                        }
                                    )
                                }
                            }
                        },
                        onLongClick = { onRename(item.id) } // Long press -> Context Menu (Rename for now)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                if (isExpanded) {
                    ExpandedAudioHub(item, onAskAi, onClick)
                } else {
                    CompactAudioCard(item, onStarClick)
                }
            }
        }
    )
}

/**
 * Compact View (List Item)
 * Minimalist: 2 Lines. Title + Truncated Summary.
 */
@Composable
private fun CompactAudioCard(
    item: AudioItemState,
    onStarClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Row 1: Icon, Filename, Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AudioIcon(item = item, onClick = { onStarClick(item.id) })
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.filename,
                color = Color(0xFF333333),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.timeDisplay,
                color = Color(0xFF888888),
                fontSize = 12.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Row 2: Status-Based Content
        when (item.status) {
            AudioStatus.PENDING -> ShimmerSwipePrompt()
            AudioStatus.TRANSCRIBING -> TranscribingProgressBar(item.progress ?: 0f)
            AudioStatus.TRANSCRIBED -> {
                Text(
                    text = item.summary ?: "无摘要內容...",
                    color = Color(0xFF888888),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 52.dp)
                )
            }
        }
    }
}

@Composable
private fun ShimmerSwipePrompt() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier.padding(start = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "右滑开始转写 >>>",
            color = Color(0xFF2196F3).copy(alpha = alpha),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TranscribingProgressBar(progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "正在转写...",
                color = Color(0xFF4CAF50),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (progress > 0f) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { if (progress > 0f) progress else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFFE8F5E9)
        )
    }
}

/**
 * Expanded View (The Hub)
 * Consumption Focus: Player Top, Portal Button, Accordions.
 */
@Composable
private fun ExpandedAudioHub(
    item: AudioItemState,
    onAskAi: (String) -> Unit,
    onCollapse: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        
        // 1. Collapse Handle (Visual Cue)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCollapse() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                 imageVector = Icons.Default.ExpandLess,
                 contentDescription = "Collapse",
                 tint = Color(0xFFDDDDDD)
             )
        }

        // 2. Player Header (Pinned Top)
        // Mock Player Visual
        Surface(
            color = Color(0xFFF5F5F5),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                 Icon(
                     imageVector = Icons.Default.PlayArrow,
                     contentDescription = "Play",
                     tint = Color(0xFF2196F3)
                 )
                 Spacer(modifier = Modifier.width(8.dp))
                 Text(
                     text = "00:12 / ${item.timeDisplay}",
                     fontSize = 12.sp,
                     color = Color(0xFF666666),
                     modifier = Modifier.weight(1f)
                 )
                 // Fake Waveform
                 Text(
                     text = "|||||·|||||||·||||",
                     fontSize = 10.sp,
                     color = Color(0xFFBDBDBD),
                     letterSpacing = 2.sp
                 )
                 Spacer(modifier = Modifier.width(8.dp))
                 Icon(
                     imageVector = Icons.Filled.VolumeUp, // Using VolumeUp as speaker substitute
                     contentDescription = null,
                     tint = Color(0xFF666666),
                     modifier = Modifier.size(16.dp)
                 )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Portal Button (Ask AI)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Button(
                onClick = { onAskAi(item.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE3F2FD), // Light Blue
                    contentColor = Color(0xFF1976D2)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "问AI (Ask AI)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Accordions (Summary, Transcript, Chapters, Highlights)
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            AudioCardAccordion(
                title = "摘要 (Summary)",
                icon = "📝",
                content = item.summary ?: "暂无摘要",
                initiallyExpanded = true // Auto-expand summary
            )
            Divider(color = Color(0xFFEEEEEE))
            AudioCardAccordion(
                title = "转写 (Transcription)",
                icon = "🗣️",
                content = "这里是详细的转写内容占位符...\n(Lazy Loaded Content)",
                initiallyExpanded = false
            )
            Divider(color = Color(0xFFEEEEEE))
            AudioCardAccordion(
                title = "章节 (Chapters)",
                icon = "📑",
                content = "1. 开场 (00:00)\n2. 讨论预算 (05:20)\n3. 总结 (12:00)",
                initiallyExpanded = false
            )
            Divider(color = Color(0xFFEEEEEE))
            AudioCardAccordion(
                title = "重点 (Highlights)",
                icon = "🖍️",
                content = "• 确认Q4预算\n• 李总提到SaaS成本",
                initiallyExpanded = false
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AudioCardAccordion(
    title: String,
    icon: String,
    content: String,
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFCCCCCC)
            )
        }
        
        AnimatedVisibility(visible = expanded) {
            Text(
                text = content,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 12.dp, start = 24.dp)
            )
        }
    }
}

@Composable
private fun AudioIcon(item: AudioItemState, onClick: () -> Unit) {
    val bgColor = if (item.isStarred) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
    val iconTint = if (item.isStarred) Color(0xFF2196F3) else Color(0xFF9E9E9E)
    
    // Container for Star + Source Icon
    Box(
        modifier = Modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Star Circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(bgColor, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Star, 
                contentDescription = if (item.isStarred) "Starred" else "Unstarred",
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // 2. Source Badge (Below Star, strictly separate)
        val sourceIcon = when (item.source) {
            AudioSource.PHONE -> Icons.Outlined.Smartphone
            AudioSource.SMARTBADGE -> Icons.Outlined.Cloud
        }
        
        Icon(
            imageVector = sourceIcon,
            contentDescription = null,
            tint = Color(0xFF9E9E9E), // Muted Gray for source
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.BottomEnd) // Position relative to parent Box
                .offset(x = (-4).dp, y = (4).dp) // Nudge inside or just below?
                // User said "below the star icon", could mean below the CIRCLE or inside?
                // "don'e overlap... below the star icon"
                // Let's interpret as: Below the Main Star Icon, essentially Bottom Right of the CIRCLE.
                // The previous impl was BottomEnd of Circle.
                // Re-reading: "cloud/local icon with the start icon... below the star icon"
                // Let's try to put it at BottomRight of the container (Circle).
                // To avoid overlap with Star (Center), BottomRight (Corner) is safe.
        )
    }
}
