package com.smartsales.prism.ui.drawers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.core.SessionPreview
import com.smartsales.prism.ui.components.SessionItem

/**
 * 历史抽屉 — 左侧滑入
 * @see prism-ui-ux-contract.md §1.4
 */
@Composable
fun HistoryDrawer(
    groupedSessions: Map<String, List<SessionPreview>>,
    onSessionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(Color(0xFF0D0D1A))
    ) {
        // 设备状态头部 (2-Row)
        DeviceStateHeader()
        
        HorizontalDivider(color = Color(0xFF333333))
        
        // 会话列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            groupedSessions.forEach { (groupName, sessions) ->
                // 分组标题
                item(key = "header-$groupName") {
                    Text(
                        text = groupName,
                        color = Color(0xFF888888),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                
                // 会话项 (单行格式)
                items(
                    items = sessions,
                    key = { it.id }
                ) { session ->
                    SessionItem(
                        clientName = session.clientName,
                        summary = session.summary,
                        onClick = { onSessionClick(session.id) }
                    )
                }
            }
        }
        
        HorizontalDivider(color = Color(0xFF333333))
        
        // 用户底栏
        UserFooter(onSettingsClick = onSettingsClick)
    }
}

/**
 * 设备状态头部 — 2行布局
 * Row 1: Battery (left), SmartBadge (right)
 * Row 2: "已连接 • 正常" (centered, gray)
 */
@Composable
private fun DeviceStateHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Row 1: 设备信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔋 85%",
                color = Color(0xFF4CAF50),
                fontSize = 14.sp
            )
            Text(
                text = "📶 SmartBadge",
                color = Color.White,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Row 2: 连接状态
        Text(
            text = "已连接 • 正常",
            color = Color(0xFF888888),
            fontSize = 12.sp
        )
    }
}

/**
 * 用户底栏
 * Left: Avatar (40dp)
 * Middle: Stacked Name + PRO Badge (Teal)
 * Right: Settings
 */
@Composable
private fun UserFooter(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "User",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Frank Chen",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                // PRO Badge - Teal rounded rect
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF00BFA5)
                ) {
                    Text(
                        text = "PRO",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = Color(0xFF888888)
            )
        }
    }
}
