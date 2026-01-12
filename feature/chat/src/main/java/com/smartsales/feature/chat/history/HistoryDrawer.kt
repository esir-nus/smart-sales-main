// File: feature/chat/src/main/java/com/smartsales/feature/chat/history/HistoryDrawer.kt
// Module: :feature:chat
// Summary: History drawer components extracted from HomeScreen
// Author: created on 2026-01-06

package com.smartsales.feature.chat.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.history.ChatHistoryTestTags
import com.smartsales.feature.chat.home.DeviceSnapshotUi
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.SessionListItemUi
import com.smartsales.feature.chat.home.theme.AppColors
import com.smartsales.feature.chat.history.SessionGrouper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun HistoryDrawerContent(
    sessions: List<SessionListItemUi>,
    currentSessionId: String,
    deviceSnapshot: DeviceSnapshotUi?,
    // formatSessionTime removed as per V12 spec (Minimalist)
    historyDeviceStatus: @Composable (DeviceSnapshotUi?) -> Unit,
    onSessionSelected: (String) -> Unit,
    onSessionLongPress: (String) -> Unit,
    onUserCenterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ChatHistoryTestTags.PAGE)
    ) {
        // Scrollable Content Area (Weighted)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 8.dp), // Space before docked footer
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            historyDeviceStatus(deviceSnapshot)
            
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无历史会话，先开始一次对话吧。",
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .testTag(HomeScreenTestTags.HISTORY_EMPTY),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val groups = remember(sessions) { SessionGrouper.groupSessions(sessions) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp), // V8: Tight spacing
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp) // V8: More vertical padding
                ) {
                    groups.forEach { group ->
                        item(key = "header-${group.label}") {
                            Text(
                                text = group.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    letterSpacing = 0.05.em,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp) // Gap V13: Prototype spacing
                            )
                        }
                        
                        items(items = group.items, key = { it.id }) { session ->
                            val isCurrent = session.id == currentSessionId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onSessionSelected(session.id) },
                                        onLongClick = { onSessionLongPress(session.id) }
                                    )
                                    .testTag("${HomeScreenTestTags.HISTORY_ITEM_PREFIX}${session.id}")
                                    .then(
                                        if (isCurrent) Modifier.shadow(
                                            elevation = 6.dp, // Gap V13: Matches 12px blur
                                            shape = RoundedCornerShape(12.dp),
                                            spotColor = AppColors.LightAccentPrimary,
                                            ambientColor = AppColors.LightAccentPrimary
                                        ) else Modifier
                                    ),
                                color = if (isCurrent) AppColors.SessionRowActiveBg else Color.Transparent, // V8: Floating text
                                tonalElevation = 0.dp, // V8: Flat
                                border = null, // V8: No border
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp) // Fixed height for touch target
                                        .padding(horizontal = 16.dp), // Per prototype
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = session.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp // Gap V13: Exact match to prototype
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Pinned Profile Dock (No Spacer needed, straight to footer)
        HistoryUserCenter(onClick = onUserCenterClick)
    }
}

@Composable
private fun HistoryUserCenter(onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val footerBg = if (isDark) AppColors.DrawerFooterDark else AppColors.DrawerFooterLight

    // Profile Dock Container
    Surface(
        color = footerBg.copy(alpha = 0.6f), // Frosted Glass Tint
        shadowElevation = 16.dp, // Upward shadow depth
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
         Box(
             modifier = Modifier
                .fillMaxWidth()
                .background(footerBg.copy(alpha = 0.6f))
                .drawBehind {
                    // Top Border (1px)
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(24.dp) // Profile Dock Padding
         ) {
            Row(
                modifier = Modifier.fillMaxWidth().testTag(HomeScreenTestTags.HISTORY_USER_CENTER),
                horizontalArrangement = Arrangement.SpaceBetween, // Dock Layout
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                   horizontalArrangement = Arrangement.spacedBy(16.dp),
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with Gradient
                    Box(
                        modifier = Modifier
                            .size(48.dp) // Slightly larger touch target
                            .background(AppColors.AvatarBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "F",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White // Always white on gradient
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Frank",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "高级销售经理",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Settings Icon (Gear)
                SettingsGlyph()
            }
         }
    }
}

@Composable
private fun SettingsGlyph() {
    val isDark = isSystemInDarkTheme()
    val tint = if (isDark) AppColors.SettingsIconTintDark else AppColors.SettingsIconTintLight
    val bg = if (isDark) AppColors.SettingsIconBgDark else AppColors.SettingsIconBgLight

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(bg, CircleShape)
            .padding(10.dp), // Icon padding
        contentAlignment = Alignment.Center
    ) {
         Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "设置",
            tint = tint
         )
    }
}
