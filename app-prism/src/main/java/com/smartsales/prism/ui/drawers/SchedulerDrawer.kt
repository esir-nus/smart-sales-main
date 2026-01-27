package com.smartsales.prism.ui.drawers

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * 日程抽屉骨架 — 从顶部下拉
 * @see prism-ui-ux-contract.md §1.3
 * 
 * 此为骨架版本：月份轮播 + 日历占位 + 拖拽手柄
 */
@Composable
fun SchedulerDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerHeight = 320.dp
    
    // 使用 AnimatedVisibility 确保关闭后不阻挡点击
    androidx.compose.animation.AnimatedVisibility(
        visible = isOpen,
        enter = androidx.compose.animation.slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = androidx.compose.animation.slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    ) {
        // 全屏容器用于放置遮罩和抽屉
        Box(modifier = Modifier.fillMaxSize()) {
            // 遮罩层 (Scrim) - 点击空白处关闭
            // 只有当抽屉打开时，并且抽屉内容没有占据全屏时，遮罩层才有意义
            // 但此处我们始终放置遮罩层，z-index 低于抽屉内容
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if(isOpen) 0.3f else 0f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )

            // 抽屉内容
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(drawerHeight)
                    .background(
                        Color(0xFF1A1A2E),
                        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    )
            ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 月份轮播占位
                MonthCarouselPlaceholder()
                
                HorizontalDivider(color = Color(0xFF333333))
                
                // 日历网格占位
                CalendarGridPlaceholder()
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 拖拽手柄
                DragHandle(onDismiss = onDismiss)
            }
        }
    }
    }
}

@Composable
private fun MonthCarouselPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", color = Color(0xFF888888), fontSize = 20.sp)
        Spacer(modifier = Modifier.width(16.dp))
        
        listOf("1月", "2月", "3月").forEach { month ->
            val isSelected = month == "1月"
            Text(
                text = month,
                color = if (isSelected) Color.White else Color(0xFF666666),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        Text("›", color = Color(0xFF888888), fontSize = 20.sp)
    }
}

@Composable
private fun CalendarGridPlaceholder() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // 星期头部
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                Text(
                    text = day,
                    color = Color(0xFF666666),
                    fontSize = 12.sp,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 单行日期（活动周）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("27", "28", "29", "30", "31", "1", "2").forEachIndexed { index, day ->
                val isToday = day == "27"
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isToday) Color(0xFF4FC3F7) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        color = if (isToday) Color.Black else Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DragHandle(onDismiss: () -> Unit) {
    var accumulatedDrag by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onDismiss() } // 点击也能关闭
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { accumulatedDrag = 0f },
                    onDragCancel = { accumulatedDrag = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    accumulatedDrag += dragAmount
                    // 累计拖动超过 50px 则关闭
                    if (accumulatedDrag < -50) {
                        onDismiss()
                        accumulatedDrag = 0f // 重置防止多次触发
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(Color(0xFF666666), RoundedCornerShape(2.dp))
        )
    }
}
