package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.ui.components.PrismButton
import com.smartsales.prism.ui.components.PrismButtonStyle
import com.smartsales.prism.ui.theme.*

/**
 * 灵感箱可折叠面板
 * 
 * - 折叠时：仅显示标题栏 "💡 灵感箱 (N) [▼]"
 * - 展开时：显示所有灵感卡片
 * - 空时：完全隐藏
 */
@Composable
fun CollapsibleInspirationShelf(
    items: List<TimelineItem.Inspiration>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: (String) -> Unit,
    onAskAI: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    // 空时隐藏
    if (items.isEmpty()) return
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BackgroundSurfaceMuted.copy(alpha = 0.5f))
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💡 灵感箱 (${items.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Content (animated)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    SwipeableShelfItem(
                        itemId = item.id,
                        onDelete = { onDelete(item.id) }
                    ) {
                        InspirationShelfCard(
                            title = item.title,
                            onAskAI = onAskAI?.let { callback -> { callback(item.title) } }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 灵感架卡片 (简化版，无多选状态)
 */
@Composable
private fun InspirationShelfCard(
    title: String,
    onAskAI: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GlassCardShape)
            .background(BackgroundSurface.copy(alpha = 0.8f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💡",
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (onAskAI != null) {
            Spacer(modifier = Modifier.width(12.dp))
            PrismButton(
                text = "Ask AI",
                onClick = onAskAI,
                style = PrismButtonStyle.GHOST,
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

/**
 * 灵感架滑动删除包装器
 */
@Composable
private fun SwipeableShelfItem(
    itemId: String,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.SwipeToDismissBox(
        state = androidx.compose.material3.rememberSwipeToDismissBoxState(
            positionalThreshold = { it * 0.25f },
            confirmValueChange = { value ->
                if (value == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd) {
                    onDelete()
                    true
                } else {
                    false
                }
            }
        ),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AccentDanger.copy(alpha = 0.1f), GlassCardShape)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = AccentDanger
                )
            }
        },
        content = { content() }
    )
}
