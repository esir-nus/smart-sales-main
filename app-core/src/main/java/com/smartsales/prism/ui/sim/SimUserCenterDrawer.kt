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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.ui.components.PrismStatusBarTopSafeArea
import com.smartsales.prism.ui.components.prismNavigationBarPadding
import com.smartsales.prism.ui.settings.EditProfileScreen
import com.smartsales.prism.ui.settings.ThemeModeDialog
import com.smartsales.prism.ui.settings.UserCenterViewModel
import com.smartsales.prism.ui.theme.PrismThemeDefaults
import com.smartsales.prism.ui.theme.toDisplayLabel

private val SimUserCenterDrawerShape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp)

private const val SimDeferredSettingValue = "后续开放"

private enum class SimUserCenterRowPresentation {
    Interactive,
    Informational,
    DeferredDisabled
}

private data class SimUserCenterPalette(
    val drawerSurface: Color,
    val cardSurface: Color,
    val border: Color,
    val divider: Color,
    val secondary: Color,
    val muted: Color,
    val accent: Color,
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
    val drawerShadow: Color,
    val deferredText: Color,
    val deferredButtonSurface: Color,
    val deferredButtonBorder: Color,
    val destructiveMuted: Color
)

@Composable
private fun rememberSimUserCenterPalette(): SimUserCenterPalette {
    val prismColors = PrismThemeDefaults.colors
    return if (PrismThemeDefaults.isDarkTheme) {
        SimUserCenterPalette(
            drawerSurface = Color(0xF2141416),
            cardSurface = Color(0xD91B1B1D),
            border = Color.White.copy(alpha = 0.10f),
            divider = Color.White.copy(alpha = 0.06f),
            secondary = Color(0xFF8B8B91),
            muted = Color(0xFFAEAEB2),
            accent = Color(0xFF0A84FF),
            avatarSurface = Color.White.copy(alpha = 0.045f),
            avatarBorder = Color.White.copy(alpha = 0.16f),
            heroGlow = Color(0x330A84FF),
            cardHighlight = Color.White.copy(alpha = 0.022f),
            textPrimary = Color.White,
            textStrong = Color.White,
            chevron = Color.White.copy(alpha = 0.22f),
            editButtonSurface = Color.White.copy(alpha = 0.055f),
            editButtonBorder = Color.White.copy(alpha = 0.12f),
            rowIconTint = Color(0xFF9999A1),
            switchUncheckedTrack = Color.White.copy(alpha = 0.13f),
            drawerShadow = Color.Black.copy(alpha = 0.38f),
            deferredText = Color(0xFF72727A),
            deferredButtonSurface = Color(0x1CFF453A),
            deferredButtonBorder = Color(0x33FF453A),
            destructiveMuted = Color(0x66FF453A)
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
            drawerShadow = Color(0x2B1D2433),
            deferredText = Color(0xFFB0B4BE),
            deferredButtonSurface = Color(0x10D94A38),
            deferredButtonBorder = Color(0x19D94A38),
            destructiveMuted = Color(0x66D94A38)
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
    val versionLabel = remember {
        BuildConfig.VERSION_NAME
            .takeIf { it.isNotBlank() }
            ?.let { "v$it" }
    }

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
                    elevation = if (PrismThemeDefaults.isDarkTheme) 24.dp else 28.dp,
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
                                palette.heroGlow.copy(alpha = 0.44f),
                                Color.Transparent,
                                if (PrismThemeDefaults.isDarkTheme) {
                                    Color.Black.copy(alpha = 0.22f)
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
                ) {
                    PrismStatusBarTopSafeArea()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                SimUserCenterDeferredRow(
                                    label = "AI 实验室",
                                    leadingIcon = Icons.Default.Psychology,
                                    showDivider = true
                                )
                                SimNotificationSettingsRow(
                                    viewModel = viewModel,
                                    showDivider = false
                                )
                            }
                        }

                        item {
                            SimUserCenterSection(title = "设备控制") {
                                val voiceVolume by viewModel.voiceVolume.collectAsStateWithLifecycle()
                                SimUserCenterVolumeRow(
                                    label = "徽章语音音量",
                                    value = voiceVolume,
                                    onValueChange = viewModel::onVoiceVolumeDrag,
                                    onValueChangeFinished = viewModel::onVoiceVolumeCommitted,
                                    palette = palette,
                                    showDivider = false
                                )
                            }
                        }

                        item {
                            SimUserCenterSection(title = "空间管理") {
                                SimUserCenterDeferredRow(
                                    label = "已用空间",
                                    leadingIcon = Icons.Default.Storage,
                                    showDivider = true
                                )
                                SimUserCenterDeferredRow(
                                    label = "清除缓存",
                                    leadingIcon = Icons.Default.Delete,
                                    showDivider = false
                                )
                            }
                        }

                        item {
                            SimUserCenterSection(title = "安全与隐私") {
                                SimUserCenterDeferredRow(
                                    label = "修改密码",
                                    leadingIcon = Icons.Default.Lock,
                                    showDivider = false
                                )
                            }
                        }

                        item {
                            SimUserCenterSection(title = "关于") {
                                SimUserCenterDeferredRow(
                                    label = "帮助中心",
                                    leadingIcon = Icons.AutoMirrored.Filled.HelpOutline,
                                    showDivider = versionLabel != null
                                )
                                if (versionLabel != null) {
                                    SimUserCenterInfoRow(
                                        label = "版本",
                                        value = versionLabel,
                                        showDivider = false
                                    )
                                }
                            }
                        }

                        item {
                            SimUserCenterDeferredLogoutButton()
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
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
            verticalArrangement = Arrangement.spacedBy(2.dp)
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
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                chips.forEach { chip ->
                    SimUserCenterMetadataChip(text = chip)
                }
                SimUserCenterMetadataChip(text = "PRO", accent = true)
            }
        } else {
            SimUserCenterMetadataChip(text = "PRO", accent = true)
        }

        val haptic = LocalHapticFeedback.current
        Surface(
            modifier = Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEdit()
            },
            color = palette.editButtonSurface,
            shape = RoundedCornerShape(15.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, palette.editButtonBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = palette.secondary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "编辑资料",
                    color = palette.secondary,
                    style = MaterialTheme.typography.labelSmall,
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
        shape = RoundedCornerShape(9.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, border)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = palette.secondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 8.dp)
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
                                palette.cardHighlight.copy(alpha = 0.08f)
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
        presentation = SimUserCenterRowPresentation.Interactive,
        label = label,
        leadingIcon = Icons.Default.Public,
        value = value,
        showChevron = true,
        onClick = onClick,
        showDivider = showDivider
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
        presentation = SimUserCenterRowPresentation.Informational,
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
        presentation = SimUserCenterRowPresentation.Interactive,
        label = label,
        leadingIcon = if (label == "消息通知") Icons.Default.NotificationsNone else Icons.Default.Person,
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
private fun SimUserCenterVolumeRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    palette: SimUserCenterPalette,
    showDivider: Boolean
) {
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = palette.rowIconTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = label,
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = value.toString(),
                    color = palette.muted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                onValueChangeFinished = onValueChangeFinished,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = palette.accent,
                    activeTrackColor = palette.accent,
                    inactiveTrackColor = palette.divider
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = palette.divider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

@Composable
private fun SimUserCenterDeferredRow(
    label: String,
    leadingIcon: ImageVector,
    showDivider: Boolean
) {
    SimUserCenterRow(
        presentation = SimUserCenterRowPresentation.DeferredDisabled,
        label = label,
        leadingIcon = leadingIcon,
        value = SimDeferredSettingValue,
        onClick = null,
        showDivider = showDivider
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
    presentation: SimUserCenterRowPresentation,
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
    val isInteractive = presentation == SimUserCenterRowPresentation.Interactive && onClick != null
    val labelColor = if (presentation == SimUserCenterRowPresentation.DeferredDisabled) {
        palette.secondary
    } else {
        palette.textPrimary
    }
    val iconTint = if (presentation == SimUserCenterRowPresentation.DeferredDisabled) {
        palette.secondary.copy(alpha = 0.88f)
    } else {
        palette.rowIconTint
    }
    val resolvedValueColor = valueColor ?: if (presentation == SimUserCenterRowPresentation.DeferredDisabled) {
        palette.deferredText
    } else {
        palette.muted
    }
    val rowModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .then(
            if (isInteractive) {
                val haptic = LocalHapticFeedback.current
                Modifier.clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick!!()
                }
            } else {
                Modifier
            }
        )
        .padding(horizontal = 14.dp, vertical = 11.dp)

    Column {
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    color = labelColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 15.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (value != null) {
                    Text(
                        text = value,
                        color = resolvedValueColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = valueFontWeight,
                        fontSize = 15.sp
                    )
                }
                rightContent?.invoke(this)
                if (showChevron && presentation == SimUserCenterRowPresentation.Interactive) {
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
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 0.5.dp,
                color = palette.divider
            )
        }
    }
}

@Composable
private fun SimUserCenterDeferredLogoutButton() {
    val palette = rememberSimUserCenterPalette()

    Surface(
        color = palette.deferredButtonSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, palette.deferredButtonBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = palette.destructiveMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "退出登录",
                modifier = Modifier.padding(start = 8.dp),
                color = palette.destructiveMuted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
