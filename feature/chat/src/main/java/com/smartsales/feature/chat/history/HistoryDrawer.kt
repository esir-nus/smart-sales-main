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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.history.ChatHistoryTestTags
import com.smartsales.feature.chat.home.DeviceSnapshotUi
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.SessionListItemUi
import com.smartsales.feature.chat.home.theme.AppColors

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun HistoryDrawerContent(
    sessions: List<SessionListItemUi>,
    currentSessionId: String,
    deviceSnapshot: DeviceSnapshotUi?,
    formatSessionTime: (Long) -> String,
    historyDeviceStatus: @Composable (DeviceSnapshotUi?) -> Unit,
    onSessionSelected: (String) -> Unit,
    onSessionLongPress: (String) -> Unit,
    onUserCenterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ChatHistoryTestTags.PAGE),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        historyDeviceStatus(deviceSnapshot)
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
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
            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                items(items = sessions, key = { it.id }) { session ->
                    val isCurrent = session.id == currentSessionId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onSessionSelected(session.id) },
                                onLongClick = { onSessionLongPress(session.id) }
                            )
                            .testTag("${HomeScreenTestTags.HISTORY_ITEM_PREFIX}${session.id}"),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isCurrent) 4.dp else 1.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (session.lastMessagePreview.isNotBlank()) {
                                Text(
                                    text = session.lastMessagePreview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatSessionTime(session.updatedAtMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HistoryUserCenter(onClick = onUserCenterClick)
    }
}

@Composable
private fun HistoryUserCenter(onClick: () -> Unit) {
    // Profile Dock Container
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), // Frosted
        shadowElevation = 16.dp, // Upward shadow depth
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
         Box(
             modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
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
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = "F",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Frank",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
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
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color.Black.copy(alpha = 0.05f), CircleShape)
            .padding(10.dp), // Icon padding
        contentAlignment = Alignment.Center
    ) {
         Icon(
            imageVector = Icons.Filled.Settings, // Fixed: Correct usage of Icons.Filled.Settings
            contentDescription = "设置",
            tint = MaterialTheme.colorScheme.onSurface
         )
    }
}
