package com.smartsales.prism.ui.sim

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.config.SubscriptionTier
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.SessionKind
import com.smartsales.prism.ui.components.PrismStatusBarTopSafeArea
import com.smartsales.prism.ui.components.prismNavigationBarPadding
import com.smartsales.prism.ui.theme.PrismTheme
import com.smartsales.prism.ui.theme.PrismThemeDefaults

private val SimHistoryDrawerShape = RoundedCornerShape(topEnd = 36.dp, bottomEnd = 36.dp)
private val SimHistoryRowShape = RoundedCornerShape(12.dp)

private data class SimHistoryPalette(
    val drawerSurface: Color,
    val drawerBorder: Color,
    val drawerShadow: Color,
    val titleColor: Color,
    val groupLabel: Color,
    val rowTitle: Color,
    val summary: Color,
    val selectedFill: Color,
    val selectedBorder: Color,
    val rowHover: Color,
    val dockSurface: Color,
    val dockBorder: Color,
    val dockDivider: Color,
    val dockSecondary: Color,
    val avatarStart: Color,
    val avatarEnd: Color,
    val iconTint: Color,
    val menuSurface: Color,
    val overlayTop: Color,
    val overlayBottom: Color,
    val danger: Color
)

@Composable
private fun rememberSimHistoryPalette(): SimHistoryPalette {
    val colors = PrismThemeDefaults.colors
    return if (PrismThemeDefaults.isDarkTheme) {
        SimHistoryPalette(
            drawerSurface = Color(0xF0121216),
            drawerBorder = Color.White.copy(alpha = 0.08f),
            drawerShadow = Color.Black.copy(alpha = 0.68f),
            titleColor = Color.White.copy(alpha = 0.96f),
            groupLabel = Color(0xFF9D9DA4),
            rowTitle = Color.White.copy(alpha = 0.92f),
            summary = Color(0xFFAEAEB2),
            selectedFill = Color.White.copy(alpha = 0.08f),
            selectedBorder = Color.White.copy(alpha = 0.10f),
            rowHover = Color.White.copy(alpha = 0.03f),
            dockSurface = Color.White.copy(alpha = 0.03f),
            dockBorder = Color.White.copy(alpha = 0.05f),
            dockDivider = Color.White.copy(alpha = 0.05f),
            dockSecondary = Color(0xFF78A9FF),
            avatarStart = Color(0xFF2A2C31),
            avatarEnd = Color(0xFF1B1C20),
            iconTint = Color.White.copy(alpha = 0.84f),
            menuSurface = Color(0xFF1B1B1F),
            overlayTop = Color.White.copy(alpha = 0.05f),
            overlayBottom = Color.Black.copy(alpha = 0.16f),
            danger = Color(0xFFFF6B60)
        )
    } else {
        SimHistoryPalette(
            drawerSurface = Color(0xFFF4F6FB).copy(alpha = 0.94f),
            drawerBorder = Color.Black.copy(alpha = 0.055f),
            drawerShadow = Color.Black.copy(alpha = 0.08f),
            titleColor = colors.textPrimary,
            groupLabel = Color(0xFF8F949D),
            rowTitle = Color(0xFF2B2F36),
            summary = Color(0xFF767982),
            selectedFill = Color.White.copy(alpha = 0.56f),
            selectedBorder = Color.Black.copy(alpha = 0.055f),
            rowHover = Color.Black.copy(alpha = 0.018f),
            dockSurface = Color.White.copy(alpha = 0.68f),
            dockBorder = Color.Black.copy(alpha = 0.035f),
            dockDivider = Color.Black.copy(alpha = 0.04f),
            dockSecondary = Color(0xFF007AFF),
            avatarStart = Color.White,
            avatarEnd = Color(0xFFE5E5EA),
            iconTint = Color(0xFF51545B),
            menuSurface = Color.White.copy(alpha = 0.97f),
            overlayTop = Color.White.copy(alpha = 0.05f),
            overlayBottom = Color.Black.copy(alpha = 0.018f),
            danger = Color(0xFFFF3B30)
        )
    }
}

@Composable
internal fun SimHistoryDrawer(
    groupedSessions: Map<String, List<SessionPreview>>,
    currentSessionId: String?,
    displayName: String,
    subscriptionTier: SubscriptionTier,
    onSessionClick: (String) -> Unit,
    onPinSession: (String) -> Unit,
    onRenameSession: (String, String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = rememberSimHistoryPalette()

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(320.dp)
            .shadow(
                elevation = 28.dp,
                shape = SimHistoryDrawerShape,
                ambientColor = palette.drawerShadow,
                spotColor = palette.drawerShadow
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            },
        color = palette.drawerSurface,
        contentColor = palette.rowTitle,
        shape = SimHistoryDrawerShape
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 0.5.dp, color = palette.drawerBorder, shape = SimHistoryDrawerShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.overlayTop,
                                Color.Transparent,
                                palette.overlayBottom
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .prismNavigationBarPadding()
            ) {
                PrismStatusBarTopSafeArea()

                Text(
                    text = "历史记录",
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = palette.titleColor,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (groupedSessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        SimHistoryEmptyState()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        groupedSessions.toList().forEach { (groupLabel, sessions) ->
                            item(key = groupLabel) {
                                SimHistoryGroupSection(
                                    label = groupLabel,
                                    sessions = sessions,
                                    currentSessionId = currentSessionId,
                                    onSessionClick = onSessionClick,
                                    onPinSession = onPinSession,
                                    onRenameSession = onRenameSession,
                                    onDeleteSession = onDeleteSession
                                )
                            }
                        }
                    }
                }

                SimHistoryUserDock(
                    displayName = displayName,
                    subscriptionTier = subscriptionTier,
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun SimHistoryGroupSection(
    label: String,
    sessions: List<SessionPreview>,
    currentSessionId: String?,
    onSessionClick: (String) -> Unit,
    onPinSession: (String) -> Unit,
    onRenameSession: (String, String, String) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    val palette = rememberSimHistoryPalette()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp),
            color = palette.groupLabel,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sessions.forEach { session ->
                key(session.id) {
                    SimHistoryRow(
                        session = session,
                        isCurrent = session.id == currentSessionId,
                        onClick = { onSessionClick(session.id) },
                        onPinToggle = { onPinSession(session.id) },
                        onRename = { title ->
                            onRenameSession(session.id, title, session.summary)
                        },
                        onDelete = { onDeleteSession(session.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SimHistoryRow(
    session: SessionPreview,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    val palette = rememberSimHistoryPalette()
    var showMenu by remember(session.id) { mutableStateOf(false) }
    var showRenameDialog by remember(session.id) { mutableStateOf(false) }
    var renameTitle by remember(session.id) { mutableStateOf(session.clientName) }

    val haptic = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(SimHistoryRowShape)
                .background(if (isCurrent) palette.selectedFill else Color.Transparent)
                .border(
                    width = if (isCurrent) 0.5.dp else 0.dp,
                    color = if (isCurrent) palette.selectedBorder else Color.Transparent,
                    shape = SimHistoryRowShape
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            SimSessionTitleLabel(
                title = session.clientName,
                hasAudioContextHistory = session.hasAudioContextHistory,
                color = palette.rowTitle,
                style = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 18.sp
                ),
                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            containerColor = palette.menuSurface
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (session.isPinned) "取消置顶" else "置顶",
                        color = palette.rowTitle
                    )
                },
                onClick = {
                    showMenu = false
                    onPinToggle()
                }
            )
            DropdownMenuItem(
                text = { Text("重命名", color = palette.rowTitle) },
                onClick = {
                    showMenu = false
                    showRenameDialog = true
                }
            )
            DropdownMenuItem(
                text = { Text("删除", color = palette.danger) },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名会话") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = renameTitle,
                        onValueChange = { renameTitle = it },
                        label = { Text("标题") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(renameTitle)
                        showRenameDialog = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SimHistoryEmptyState() {
    val palette = rememberSimHistoryPalette()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (PrismThemeDefaults.isDarkTheme) 0.04f else 0.22f))
                .border(0.5.dp, palette.drawerBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = palette.groupLabel,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "暂无会话",
            color = palette.titleColor,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "您的历史沟通记录会在这里按时间归档，方便稍后继续跟进。",
            color = palette.summary,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SimHistoryUserDock(
    displayName: String,
    subscriptionTier: SubscriptionTier,
    onOpenSettings: () -> Unit
) {
    val palette = rememberSimHistoryPalette()
    val resolvedDisplayName = displayName.ifBlank { "用户" }
    val planLabel = when (subscriptionTier) {
        SubscriptionTier.FREE -> "Free Plan"
        SubscriptionTier.PRO -> "Pro Plan"
        SubscriptionTier.ENTERPRISE -> "Enterprise"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(palette.dockDivider)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.dockSurface)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenSettings),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(palette.avatarStart, palette.avatarEnd)
                            )
                        )
                        .border(0.5.dp, palette.dockBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = palette.iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = resolvedDisplayName,
                        color = palette.rowTitle,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = planLabel,
                        color = palette.dockSecondary,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "打开设置",
                    tint = palette.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewSimHistoryDrawerHost(
    groupedSessions: Map<String, List<SessionPreview>>,
    currentSessionId: String? = null,
    displayName: String = "Frank",
    subscriptionTier: SubscriptionTier = SubscriptionTier.PRO
) {
    PrismTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .size(width = 390.dp, height = 844.dp)
                .background(Color(0xFFF5F5F7))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x33007AFF),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(330f, 620f),
                            radius = 280f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.10f))
            )
            SimHistoryDrawer(
                groupedSessions = groupedSessions,
                currentSessionId = currentSessionId,
                displayName = displayName,
                subscriptionTier = subscriptionTier,
                onSessionClick = {},
                onPinSession = {},
                onRenameSession = { _, _, _ -> },
                onDeleteSession = {},
                onOpenSettings = {},
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

private fun previewSession(
    id: String,
    title: String,
    summary: String = "",
    pinned: Boolean = false
): SessionPreview = SessionPreview(
    id = id,
    clientName = title,
    summary = summary,
    timestamp = 0L,
    isPinned = pinned,
    sessionKind = SessionKind.GENERAL
)

private fun previewPopulatedGroups(): Map<String, List<SessionPreview>> = linkedMapOf(
    "置顶" to listOf(
        previewSession("pinned-1", "华东大区 Q1 业务复盘", pinned = true)
    ),
    "今天" to listOf(
        previewSession("today-1", "产品需求评审会 V2.4"),
        previewSession("today-2", "拜访客户 A 录音分析")
    ),
    "最近30天" to listOf(
        previewSession("recent-1", "技术团队 OKR 讨论"),
        previewSession("recent-2", "活动策划预演")
    ),
    "2026-02" to listOf(
        previewSession("month-1", "年度全员大会筹备")
    )
)

private fun previewDenseGroups(): Map<String, List<SessionPreview>> = linkedMapOf(
    "置顶" to listOf(
        previewSession("dense-p1", "华东大区 Q1 业务复盘", pinned = true),
        previewSession("dense-p2", "华南渠道周报同步", pinned = true)
    ),
    "今天" to listOf(
        previewSession("dense-t1", "产品需求评审会 V2.4"),
        previewSession("dense-t2", "拜访客户 A 录音分析"),
        previewSession("dense-t3", "KA 客户复盘纪要"),
        previewSession("dense-t4", "财务预算讨论")
    ),
    "最近30天" to listOf(
        previewSession("dense-r1", "技术团队 OKR 讨论"),
        previewSession("dense-r2", "活动策划预演"),
        previewSession("dense-r3", "渠道商季度对账"),
        previewSession("dense-r4", "新品上市发布会")
    )
)

private fun previewLongTitleGroups(): Map<String, List<SessionPreview>> = linkedMapOf(
    "今天" to listOf(
        previewSession(
            id = "long-1",
            title = "这是一个需要在历史抽屉中做单行省略处理的超长客户沟通标题示例"
        ),
        previewSession(
            id = "long-2",
            title = "另一个非常长的录音分析标题用于验证卡片在窄抽屉中的文本截断表现"
        )
    ),
    "最近30天" to listOf(
        previewSession(
            id = "long-3",
            title = "年度全域营销项目协同会议纪要与后续商机追踪"
        )
    )
)

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F7, name = "SIM History 1. Open / Populated")
@Composable
private fun PreviewSimHistoryDrawerPopulated() {
    PreviewSimHistoryDrawerHost(
        groupedSessions = previewPopulatedGroups(),
        currentSessionId = "today-1"
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F7, name = "SIM History 2. Open / Empty")
@Composable
private fun PreviewSimHistoryDrawerEmpty() {
    PreviewSimHistoryDrawerHost(
        groupedSessions = emptyMap()
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F7, name = "SIM History 3. Open / Truncation Stress")
@Composable
private fun PreviewSimHistoryDrawerLongTitles() {
    PreviewSimHistoryDrawerHost(
        groupedSessions = previewLongTitleGroups(),
        currentSessionId = "long-1"
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F7, name = "SIM History 4. Open / Dense Groups")
@Composable
private fun PreviewSimHistoryDrawerDense() {
    PreviewSimHistoryDrawerHost(
        groupedSessions = previewDenseGroups(),
        currentSessionId = "dense-t2"
    )
}
