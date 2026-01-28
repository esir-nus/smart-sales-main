package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Scheduler Calendar Component
 * @see prism-ui-ux-contract.md §1.3 "Month Carousel" & "Calendar Grid"
 * 
 * Implements:
 * 1. Expandable Grid (Week <-> Month) with Object Permanence
 * 2. Visual Style: Light Theme, Left Aligned Header
 * 3. Expansion Handle
 */

@Composable
fun SchedulerCalendar(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(bottom = 8.dp)
    ) {
        // 1. Header (Month Title + Pills)
        CalendarHeader()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 2. Expandable Grid
        ExpandableCalendarGrid(isExpanded = isExpanded)
        
        // 3. Expansion Handle
        ExpansionHandle(
            isExpanded = isExpanded,
            onExpandChange = onExpandChange
        )
    }
}

@Composable
private fun CalendarHeader() {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Year + Month Text
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "2026年",
                fontSize = 16.sp,
                color = Color(0xFF888888),
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "1月",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Month Pills (Visual Mock)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val months = listOf("1月", "2月", "3月", "4月", "5月")
            months.forEach { month ->
                val isSelected = month == "1月"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color.Black else Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = month,
                        fontSize = 14.sp,
                        color = if (isSelected) Color.White else Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableCalendarGrid(isExpanded: Boolean) {
    // Mock Data: 35 days (5 weeks)
    // Structure: 0-6 (Prev Month), 7-13, 14-20 (Active), 21-27, 28-34
    // Active Week is Index 14-20 (Row 3)
    val days = (1..35).toList()
    val activeDay = 28 // Mapping roughly to the visual position
    
    // Height Animation
    // Collapsed: ~56dp (1 Row)
    // Expanded: ~280dp (5 Rows) - height is adaptive
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {
        Column {
            // Week Headers
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { 
                    Text(
                        text = it, 
                        fontSize = 12.sp, 
                        color = Color(0xFF999999), 
                        modifier = Modifier.width(36.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            // Grid Rows
            // We implement "Visual Masking" logic
            // If Expanded: Show All Rows
            // If Collapsed: Show ONLY Row 3 (Active)
            
            if (isExpanded) {
                // Render ALL 5 Rows
                (0 until 5).forEach { rowIndex ->
                    CalendarRow(
                        days = days.subList(rowIndex * 7, (rowIndex + 1) * 7),
                        activeDay = activeDay
                    )
                }
            } else {
                // Render ONLY Active Row (Row 3 -> Indices 14-20)
                // In mock, let's say indices 14-20 correspond to days 15-21
                // Wait, active day is 28. Row 4? 
                // Let's assume Row 3 contains the "Active Date" for the mock visuals.
                // Row 0: 1-7
                // Row 1: 8-14
                // Row 2: 15-21
                // Row 3: 22-28 (Contains 28) -> This is the active row
                CalendarRow(
                    days = days.subList(21, 28), // 22..28
                    activeDay = activeDay
                )
            }
        }
    }
}

@Composable
private fun CalendarRow(days: List<Int>, activeDay: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp), // Fixed row height
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { dayVal ->
            // Remapping 1-35 to simplified visual days
            // Let's just render the number.
            // visualDay 28 matches activeDay 28
            
            val dayNum = if (dayVal > 31) dayVal - 31 else dayVal // Simplified wrap
            val isToday = dayVal == activeDay // Use raw list value for unique match
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isToday) Color(0xFF2962FF) else Color.Transparent, // Deep Blue
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayNum.toString(),
                        color = if (isToday) Color.White else Color(0xFF1A1A1A),
                        fontSize = 14.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    if (!isToday && dayNum == 30) { 
                        // Mock dot for other event
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color(0xFF4FC3F7), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpansionHandle(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    // Simple drag logic
    var dragOffset by remember { mutableStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragOffset += delta
                },
                onDragStopped = { 
                    if (isExpanded && dragOffset < -30) {
                        onExpandChange(false) // Collapse
                    } else if (!isExpanded && dragOffset > 30) {
                        onExpandChange(true) // Expand 
                    }
                    dragOffset = 0f
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
        )
    }
}
