package com.smartsales.prism.ui.drawers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Single Audio Card Component
 * Handles 3 states: TRANSCRIBED, TRANSCRIBING, PENDING
 */
@Composable
fun AudioCard(
    item: AudioItemState,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color(0x1A000000)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Row 1: Icon, Filename, Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Icon
                AudioIcon(item)
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Filename
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
                
                // Time
                Text(
                    text = item.timeDisplay,
                    color = Color(0xFF888888),
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Row 2: Source Icon + Content Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Source Icon (Cloud/Phone)
                Icon(
                    imageVector = if (item.source == AudioSource.SMARTBADGE) Icons.Outlined.Cloud else Icons.Outlined.Smartphone,
                    contentDescription = null,
                    tint = Color(0xFF888888), // Consistent gray tint for source
                    modifier = Modifier
                        .size(16.dp)
                        .offset(y = 2.dp) // Optical alignment
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Content Area (Summary / Progress / Prompt)
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    when (item.status) {
                        AudioStatus.TRANSCRIBED -> TranscribedContent(item.summary)
                        AudioStatus.TRANSCRIBING -> TranscribingContent(item.progress)
                        AudioStatus.PENDING -> PendingContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioIcon(item: AudioItemState) {
    // Background color logic
    val bgColor = when {
        item.isStarred -> Color(0xFFE3F2FD) // Light Blue
        item.status == AudioStatus.TRANSCRIBING -> Color(0xFFFFF3E0) // Light Orange
        else -> Color(0xFFF5F5F5) // Light Gray
    }
    
    // Icon tint logic
    val iconTint = when {
        item.isStarred -> Color(0xFF2196F3) // Blue
        item.status == AudioStatus.TRANSCRIBING -> Color(0xFFFF9800) // Orange
        else -> Color(0xFF9E9E9E) // Gray
    }
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(bgColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (item.isStarred) {
            Icon(Icons.Filled.Star, null, tint = iconTint, modifier = Modifier.size(24.dp))
        } else {
            Icon(Icons.Outlined.Headphones, null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun TranscribedContent(summary: String?) {
    Surface(
        color = Color(0xFFF8F9FA), // Very light gray
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = summary ?: "无摘要",
            color = Color(0xFF666666),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TranscribingContent(progress: Float?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = progress ?: 0f,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Color(0xFFFF9800),
            trackColor = Color(0xFFEEEEEE)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "转写中...",
                color = Color(0xFFFF9800),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${((progress ?: 0f) * 100).toInt()}%",
                color = Color(0xFF888888),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PendingContent() {
    Surface(
        color = Color(0xFFF8F9FA),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "右滑转写 >>>",
            color = Color(0xFF888888),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
