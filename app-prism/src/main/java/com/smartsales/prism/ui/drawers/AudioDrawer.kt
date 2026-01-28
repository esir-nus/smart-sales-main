package com.smartsales.prism.ui.drawers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons // ADDED IMPORT
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind

/**
 * Audio Drawer — Bottom-Up Sheet
 * @see prism-ui-ux-contract.md §1.8
 */
@Composable
fun AudioDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Height: Leave 5% gap at top (approx 95vh)
    // For skeleton, we use a fixed high percentage or dp
    
    // Sample Data Mock (Matching Screenshot)
    val sampleItems = remember {
        listOf(
            AudioItemState(
                id = "1",
                filename = "Q4_年度预算会议.wav",
                timeDisplay = "1 day",
                source = AudioSource.SMARTBADGE,
                status = AudioStatus.TRANSCRIBED,
                isStarred = true,
                summary = "财务部关于Q4预算的最终审核意见，重点..."
            ),
            AudioItemState(
                id = "2",
                filename = "meeting_notes.wav",
                timeDisplay = "10:15",
                source = AudioSource.PHONE,
                status = AudioStatus.TRANSCRIBING,
                progress = 0.45f
            ),
            AudioItemState(
                id = "3",
                filename = "客户拜访_张总_20260124.wav",
                timeDisplay = "14:30",
                source = AudioSource.SMARTBADGE,
                status = AudioStatus.PENDING
            )
        )
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(
            initialOffsetY = { it }, // Slide up from bottom
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy, // 0.65-0.75 range
                stiffness = Spring.StiffnessMedium // Approx 200 stiffness
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { onDismiss() }
            )

            // Drawer Content
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f) // 95% height
                    .background(
                        Color(0xFFF7F8FA), // Light gray bg for drawer
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            ) {
                // 1. Drag Handle Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount > 20) onDismiss() // Simple drag down logic
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color(0xFFDDDDDD), RoundedCornerShape(2.dp))
                    )
                }

                // 2. Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "录音文件",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF333333)
                    )
                    
                    // Sync Status Pill (Green)
                    Surface(
                        color = Color(0xFFE8F5E9), // Light Green
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Smartphone, // Placeholder for badge icon
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SmartBadge 已同步",
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 3. List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(items = sampleItems, key = { it.id }) { item ->
                        AudioCard(item = item, onClick = { /* TODO */ })
                    }
                    
                    // 4. Footer Upload Button as last item
                    item {
                        UploadButton()
                    }
                    
                    // Bottom Spacer
                    item { Spacer(modifier = Modifier.height(30.dp)) }
                }
            }
        }
    }
}

@Composable
private fun UploadButton() {
    val stroke = Stroke(
        width = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFDDDDDD),
                    style = stroke,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                )
            }
            .background(Color.Transparent)
            .clickable { /* TODO */ },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "+",
                fontSize = 18.sp,
                color = Color(0xFF888888),
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "上传本地音频",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
