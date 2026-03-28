package com.smartsales.prism.ui.sim

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.ui.components.prismNavigationBarPadding
import com.smartsales.prism.ui.components.prismStatusBarTopSafeBandPadding
import com.smartsales.prism.ui.settings.EditProfileScreen
import com.smartsales.prism.ui.settings.ThemeModeDialog
import com.smartsales.prism.ui.settings.UserCenterViewModel
import com.smartsales.prism.ui.theme.PrismThemeDefaults
import com.smartsales.prism.ui.theme.toDisplayLabel

private val SimUserCenterDrawerShape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp)

private data class SimUserCenterPalette(
    val drawerSurface: Color,
    val cardSurface: Color,
    val border: Color,
    val divider: Color,
    val secondary: Color,
    val muted: Color,
    val accent: Color,
    val danger: Color,
    val logoutSurface: Color,
    val avatarSurface: Color,
    val avatarBorder: Color,
    val heroGlow: Color,
    val cardHighlight: Color,
    val textPrimary: Color,
    val textStrong: Color,
    val chevron: Color,
    val editButtonSurface: Color,
    val editButtonBorder: Color,
    val rowIconTint: Color,
    val switchUncheckedTrack: Color,
    val drawerShadow: Color
)

@Composable
private fun rememberSimUserCenterPalette(): SimUserCenterPalette {
    val prismColors = PrismThemeDefaults.colors
    return if (PrismThemeDefaults.isDarkTheme) {
        SimUserCenterPalette(
            drawerSurface = Color(0xED141416),
            cardSurface = Color(0xA61C1C1E),
            border = Color.White.copy(alpha = 0.08f),
            divider = Color.White.copy(alpha = 0.05f),
            secondary = Color(0xFF8D8D93),
            muted = Color(0xFFAEAEB2),
            accent = Color(0xFF0A84FF),
            danger = Color(0xFFFF453A),
            logoutSurface = Color(0x14FF453A),
            avatarSurface = Color.White.copy(alpha = 0.05f),
            avatarBorder = Color.White.copy(alpha = 0.14f),
            heroGlow = Color.White.copy(alpha = 0.04f),
            cardHighlight = Color.White.copy(alpha = 0.03f),
            textPrimary = Color.White,
            textStrong = Color.White,
            chevron = Color.White.copy(alpha = 0.18f),
            editButtonSurface = Color.White.copy(alpha = 0.06f),
            editButtonBorder = Color.White.copy(alpha = 0.10f),
            rowIconTint = Color(0xFF9A9AA2),
            switchUncheckedTrack = Color.White.copy(alpha = 0.12f),
            drawerShadow = Color.Black.copy(alpha = 0.32f)
        )
    } else {
        SimUserCenterPalette(
            drawerSurface = Color(0xFFF6F7FB).copy(alpha = 0.94f),
            cardSurface = Color.White.copy(alpha = 0.94f),
            border = Color.Black.copy(alpha = 0.06f),
            divider = Color.Black.copy(alpha = 0.045f),
            secondary = Color(0xFF8B909B),
            muted = Color(0xFFA9ADB7),
            accent = Color(0xFF007AFF),
            danger = Color(0xFFFF3B30),
            logoutSurface = Color(0x12FF3B30),
            avatarSurface = Color.White.copy(alpha = 0.86f),
            avatarBorder = Color.Black.copy(alpha = 0.06f),
            heroGlow = Color.White.copy(alpha = 0.52f),
            cardHighlight = Color(0xFFEFF4FB).copy(alpha = 0.56f),
            textPrimary = prismColors.textPrimary,
            textStrong = prismColors.textPrimary,
            chevron = Color.Black.copy(alpha = 0.16f),
            editButtonSurface = Color.White.copy(alpha = 0.68f),
            editButtonBorder = Color.Black.copy(alpha = 0.05f),
            rowIconTint = Color(0xFF8E8E93),
            switchUncheckedTrack = Color.Black.copy(alpha = 0.08f),
            drawerShadow = Color(0x2B1D2433)
        )
    }
}

@Composable
internal fun SimUserCenterDrawer(
    onClose: () -> Unit,
    viewModel: UserCenterViewModel = hiltViewModel()
) {
    val palette = rememberSimUserCenterPalette()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var isEditing by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (isEditing) {
            isEditing = false
        } else {
            onClose()
        }
    }

    if (isEditing && profile != null) {
        EditProfileScreen(
            profile = profile!!,
            onSave = { name, role, industry, exp, platform ->
                viewModel.updateProfile(name, role, industry, exp, platform)
                isEditing = false
            },
            onBack = { isEditing = false }
        )
        return
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(330.dp)
                .shadow(
                    elevation = if (PrismThemeDefaults.isDarkTheme) 18.dp else 28.dp,
                    shape = SimUserCenterDrawerShape,
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
            shape = SimUserCenterDrawerShape,
            color = palette.drawerSurface,
            contentColor = palette.textStrong
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(0.5.dp, palette.border, SimUserCenterDrawerShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.heroGlow,
                                Color.Transparent,
                                if (PrismThemeDefaults.isDarkTheme) {
                                    Color.Black.copy(alpha = 0.14f)
                                } else {
                                    Color(0xFFE9EEF7).copy(alpha = 0.82f)
                                }
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .prismNavigationBarPadding()
                        .prismStatusBarTopSafeBandPadding()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        item {
                            profile?.let { user ->
                                SimUserCenterProfileHero(
                                    profile = user,
                                    onEdit = { isEditing = true },
                                    palette = palette
                                )
                            }
                        }

                        item {
                            SimUserCenterSection(title = "偏好设置") {
                                SimUserCenterSelectRow(
                                    label = "主题外观",
                                    value = themeMode.toDisplayLabel(),
                                    onClick = { showThemeDialog = true },
                                    showDivider = true
                                )
                                SimUserCenterToggleRow(
                                    label = "AI 实验室",
                                    checked = true,
                                    showDivider = true
                                ) {}
                                SimNotificationSettingsRow(
                                    viewModel = viewModel,
                                    showDivider = false
                                )
                            }
                        }

                        item {
                            SimUserCenterSection(title = "空间管理") {
                                SimUserCenterInfoRow(
                                    label = "已用空间",
                                    value = "120 MB",
                                    leadingIcon = Icons.Default.Storage,
                                    showDivider = true
                                )
                                SimUserCenterActionRow(
                                    label = "清除缓存",
                                    leadingIcon = Icons.Default.CleaningServices,
                                    showDivider = false
                                ) {}
                            }
                        }

                        item {
                            SimUserCenterSection(title = "安全与隐私") {
                                SimUserCenterNavRow(
                                    label = "修改密码",
                                    showDivider = false
                                ) {}
                            }
                        }

                        item {
                            SimUserCenterSection(title = "关于") {
                                SimUserCenterInfoRow(
                                    label = "版本",
                                    value = "Prism v1.2 (Pro Max)",
                                    showDivider = true
                                )
                                SimUserCenterNavRow(
                                    label = "帮助中心",
                                    showDivider = false
                                ) {}
                            }
                        }

                        item {
                            SimUserCenterLogoutButton(onClick = onClose)
                        }
                    }
                }
            }
        }

        if (showThemeDialog) {
            ThemeModeDialog(
                currentMode = themeMode,
                onDismiss = { showThemeDialog = false },
                onSelectMode = viewModel::setThemeMode,
                supportingText = "当前设置会立即影响 SIM 启动主路径；调度、录音、连接等深层浮层仍在后续波次。"
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SimUserCenterProfileHero(
    profile: UserProfile,
    onEdit: () -> Unit,
    palette: SimUserCenterPalette
) {
    val chips = remember(profile) {
        buildList {
            profile.industry.trim().takeIf { it.isNotEmpty() }?.let(::add)
            profile.experienceYears.trim().takeIf { it.isNotEmpty() }?.let(::add)
            profile.communicationPlatform.trim().takeIf { it.isNotEmpty() }?.let(::add)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.heroGlow,
                            palette.avatarSurface
                        )
                    ),
                    shape = CircleShape
                )
                .border(1.dp, palette.avatarBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (profile.displayName.isNotBlank()) {
                Text(
                    text = profile.displayName.take(2).uppercase(),
                    color = palette.textStrong,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = palette.textStrong.copy(alpha = 0.8f),
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = profile.displayName,
                color = palette.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = profile.role,
                color = palette.secondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        if (chips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chips.forEach { chip ->
                    SimUserCenterMetadataChip(text = chip)
                }
                SimUserCenterMetadataChip(text = "PRO", accent = true)
            }
        } else {
            SimUserCenterMetadataChip(text = "PRO", accent = true)
        }

        Surface(
            modifier = Modifier.clickable(onClick = onEdit),
            color = palette.editButtonSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, palette.editButtonBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = palette.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "编辑资料",
                    color = palette.secondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SimUserCenterMetadataChip(
    text: String,
    accent: Boolean = false
) {
    val palette = rememberSimUserCenterPalette()
    val background = if (accent) {
        palette.accent.copy(alpha = 0.15f)
    } else {
        palette.textStrong.copy(alpha = if (PrismThemeDefaults.isDarkTheme) 0.06f else 0.04f)
    }
    val border = if (accent) {
        palette.accent.copy(alpha = 0.35f)
    } else {
        palette.border
    }
    val textColor = if (accent) palette.accent else palette.muted

    Surface(
        color = background,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, border)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SimUserCenterSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = rememberSimUserCenterPalette()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = palette.secondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 10.dp)
        )
        Surface(
            color = palette.cardSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, palette.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.cardHighlight,
                                Color.Transparent,
                                palette.cardHighlight.copy(alpha = 0.15f)
                            )
                        )
                    )
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SimUserCenterSelectRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    SimUserCenterRow(
        label = label,
        leadingIcon = Icons.Default.Public,
        value = value,
        showChevron = true,
        onClick = onClick,
        showDivider = showDivider
    )
}

@Composable
private fun SimUserCenterNavRow(
    label: String,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    SimUserCenterRow(
        label = label,
        leadingIcon = when (label) {
            "修改密码" -> Icons.Default.Lock
            "帮助中心" -> Icons.Default.HelpOutline
            else -> Icons.Default.ChevronRight
        },
        showChevron = true,
        onClick = onClick,
        showDivider = showDivider
    )
}

@Composable
private fun SimUserCenterActionRow(
    label: String,
    leadingIcon: ImageVector,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    SimUserCenterRow(
        label = label,
        leadingIcon = leadingIcon,
        showDivider = showDivider,
        onClick = onClick
    )
}

@Composable
private fun SimUserCenterInfoRow(
    label: String,
    value: String,
    leadingIcon: ImageVector = Icons.Default.Info,
    showDivider: Boolean
) {
    SimUserCenterRow(
        label = label,
        leadingIcon = leadingIcon,
        value = value,
        onClick = null,
        showDivider = showDivider
    )
}

@Composable
private fun SimUserCenterToggleRow(
    label: String,
    checked: Boolean,
    showDivider: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SimUserCenterRow(
        label = label,
        leadingIcon = when (label) {
            "AI 实验室" -> Icons.Default.Psychology
            "消息通知" -> Icons.Default.NotificationsNone
            else -> Icons.Default.Person
        },
        onClick = null,
        showDivider = showDivider,
        rightContent = {
            val palette = rememberSimUserCenterPalette()
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = palette.accent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = palette.switchUncheckedTrack,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    )
}

@Composable
private fun SimNotificationSettingsRow(
    viewModel: UserCenterViewModel,
    showDivider: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsEnabled by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = viewModel.hasNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        notificationsEnabled = viewModel.hasNotificationPermission()
    }

    SimUserCenterToggleRow(
        label = "消息通知",
        checked = notificationsEnabled,
        showDivider = showDivider,
        onCheckedChange = {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }
    )
}

@Composable
private fun SimUserCenterRow(
    label: String,
    leadingIcon: ImageVector,
    value: String? = null,
    valueColor: Color? = null,
    valueFontWeight: FontWeight = FontWeight.Normal,
    showChevron: Boolean = false,
    rightContent: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)?,
    showDivider: Boolean
) {
    val palette = rememberSimUserCenterPalette()
    val rowModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 50.dp)
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )
        .padding(horizontal = 15.dp, vertical = 12.dp)

    Column {
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = palette.rowIconTint,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = label,
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (value != null) {
                    Text(
                        text = value,
                        color = valueColor ?: palette.muted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = valueFontWeight
                    )
                }
                rightContent?.invoke(this)
                if (showChevron) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = palette.chevron,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = palette.divider
            )
        }
    }
}

@Composable
private fun SimUserCenterLogoutButton(onClick: () -> Unit) {
    val palette = rememberSimUserCenterPalette()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.logoutSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, palette.danger.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = palette.danger,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "退出登录",
                color = palette.danger,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
