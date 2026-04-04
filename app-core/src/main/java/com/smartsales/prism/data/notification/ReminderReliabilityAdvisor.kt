package com.smartsales.prism.data.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat

/**
 * 提醒可靠性引导器。
 *
 * 统一收敛：
 * - 精确闹钟权限
 * - 电池优化豁免
 * - 中国 OEM 的自启动/锁屏/悬浮/后台通知加固建议
 *
 * 说明：
 * - 这是“是否要提示”与“提示什么内容”的唯一来源
 * - UI 侧只消费 [ReminderReliabilityGuide]，不再硬编码 OEM 文案
 */
object ReminderReliabilityAdvisor {

    enum class Action {
        EXACT_ALARM,
        FULL_SCREEN_INTENT,
        BATTERY_OPTIMIZATION,
        OEM_PERMISSION_EDITOR,
        AUTO_START,
        APP_NOTIFICATION_SETTINGS
    }

    data class Snapshot(
        val manufacturerLabel: String,
        val isChineseOem: Boolean,
        val isXiaomi: Boolean,
        val isHuawei: Boolean,
        val isOppo: Boolean,
        val isVivo: Boolean,
        val notificationsEnabled: Boolean,
        val needsExactAlarmPermission: Boolean,
        val canUseFullScreenIntent: Boolean,
        val ignoresBatteryOptimizations: Boolean,
        val canShowOnLockScreen: Boolean,
        val canShowFloatingNotification: Boolean,
        val canSendBackgroundNotification: Boolean
    )

    data class ReminderReliabilityGuide(
        val title: String,
        val message: String,
        val checklist: List<String>,
        val primaryAction: Action,
        val primaryLabel: String,
        val secondaryAction: Action? = null,
        val secondaryLabel: String? = null
    )

    fun fromContext(context: Context): ReminderReliabilityGuide? {
        return fromSnapshot(collectSnapshot(context))
    }

    fun collectSnapshot(context: Context): Snapshot {
        return Snapshot(
            manufacturerLabel = android.os.Build.MANUFACTURER.ifBlank { "Android" },
            isChineseOem = OemCompat.isChineseOem,
            isXiaomi = OemCompat.isXiaomi,
            isHuawei = OemCompat.isHuawei,
            isOppo = OemCompat.isOppo,
            isVivo = OemCompat.isVivo,
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            needsExactAlarmPermission = OemCompat.needsExactAlarmPermission(context),
            canUseFullScreenIntent = OemCompat.canUseFullScreenIntent(context),
            ignoresBatteryOptimizations = OemCompat.isIgnoringBatteryOptimizations(context),
            canShowOnLockScreen = OemCompat.canShowOnLockScreen(context),
            canShowFloatingNotification = OemCompat.canShowFloatingNotification(context),
            canSendBackgroundNotification = OemCompat.canSendBackgroundNotification(context)
        )
    }

    fun fromSnapshot(snapshot: Snapshot): ReminderReliabilityGuide? {
        val checklist = buildChecklist(snapshot)
        if (checklist.isEmpty()) return null

        val primaryAction = when {
            !snapshot.notificationsEnabled -> Action.APP_NOTIFICATION_SETTINGS
            snapshot.needsExactAlarmPermission -> Action.EXACT_ALARM
            !snapshot.canUseFullScreenIntent -> Action.FULL_SCREEN_INTENT
            snapshot.isHuawei -> Action.AUTO_START
            !snapshot.ignoresBatteryOptimizations -> Action.BATTERY_OPTIMIZATION
            snapshot.isXiaomi && hasXiaomiVisibilityRisk(snapshot) -> Action.OEM_PERMISSION_EDITOR
            snapshot.isChineseOem -> Action.AUTO_START
            else -> Action.APP_NOTIFICATION_SETTINGS
        }

        val secondaryAction = when {
            snapshot.isHuawei && !snapshot.notificationsEnabled ->
                Action.AUTO_START
            snapshot.isHuawei && !snapshot.canUseFullScreenIntent ->
                Action.AUTO_START
            snapshot.isHuawei && primaryAction != Action.AUTO_START ->
                Action.AUTO_START
            snapshot.isHuawei &&
                !snapshot.ignoresBatteryOptimizations &&
                primaryAction != Action.BATTERY_OPTIMIZATION ->
                Action.BATTERY_OPTIMIZATION
            snapshot.isHuawei -> null
            snapshot.isXiaomi &&
                !snapshot.canUseFullScreenIntent &&
                hasXiaomiVisibilityRisk(snapshot) ->
                Action.OEM_PERMISSION_EDITOR
            snapshot.isXiaomi && primaryAction != Action.OEM_PERMISSION_EDITOR && hasXiaomiVisibilityRisk(snapshot) ->
                Action.OEM_PERMISSION_EDITOR
            snapshot.isChineseOem && primaryAction != Action.AUTO_START ->
                Action.AUTO_START
            primaryAction != Action.APP_NOTIFICATION_SETTINGS ->
                Action.APP_NOTIFICATION_SETTINGS
            else -> null
        }

        return ReminderReliabilityGuide(
            title = buildTitle(snapshot),
            message = buildMessage(snapshot),
            checklist = checklist,
            primaryAction = primaryAction,
            primaryLabel = actionLabel(primaryAction),
            secondaryAction = secondaryAction,
            secondaryLabel = secondaryAction?.let(::actionLabel)
        )
    }

    fun openAction(context: Context, action: Action): Boolean {
        return when (action) {
            Action.EXACT_ALARM -> OemCompat.openExactAlarmSettings(context)
            Action.FULL_SCREEN_INTENT -> OemCompat.openFullScreenIntentSettings(context)
            Action.BATTERY_OPTIMIZATION -> OemCompat.openBatteryOptimizationSettings(context)
            Action.OEM_PERMISSION_EDITOR -> {
                if (OemCompat.isXiaomi) {
                    OemCompat.openLockScreenPermission(context)
                } else {
                    OemCompat.openNotificationSettings(context)
                }
            }
            Action.AUTO_START -> OemCompat.openAutoStartSettings(context)
            Action.APP_NOTIFICATION_SETTINGS -> {
                if (OemCompat.isHuawei) {
                    OemCompat.openHuaweiNotificationSettings(context)
                } else {
                    OemCompat.openNotificationSettings(context)
                }
            }
        }
    }

    private fun buildChecklist(snapshot: Snapshot): List<String> {
        val items = mutableListOf<String>()

        if (!snapshot.notificationsEnabled) {
            items += "系统已关闭本应用通知，请先在应用通知设置中重新开启，否则提醒会被系统直接拦截。"
        }
        if (snapshot.needsExactAlarmPermission) {
            items += "开启“闹钟和提醒 / 精确闹钟”权限，避免提醒被延迟。"
        }
        if (!snapshot.canUseFullScreenIntent) {
            items += "开启“全屏闹钟 / 全屏通知”权限，避免熄屏时只能看到普通通知而无法直接拉起提醒界面。"
        }
        if (!snapshot.ignoresBatteryOptimizations) {
            items += "关闭电池优化，改为“无限制 / 不受限制”。"
        }

        when {
            snapshot.isXiaomi -> {
                if (!snapshot.canShowOnLockScreen) {
                    items += "开启锁屏显示 / 锁屏通知，否则到点可能已触发但看不到。"
                }
                if (!snapshot.canShowFloatingNotification) {
                    items += "开启悬浮通知，否则横幅提醒可能被系统隐藏。"
                }
                if (!snapshot.canSendBackgroundNotification) {
                    items += "允许后台通知，否则后台到点提醒可能不弹出。"
                }
                if (items.isNotEmpty()) {
                    items += "确认“后台弹出页面”已允许，否则 HyperOS 仍可能拦截熄屏时的全屏提醒界面。"
                    items += "确认已开启自启动，避免重启后或被杀后台后丢提醒。"
                }
            }
            snapshot.isHuawei -> {
                items += "在应用启动管理里改为手动允许：自启动、关联启动、后台活动。"
                items += "确认锁屏通知已开启（设置 → 通知和状态栏 → 锁屏通知）。"
                items += "确认通知横幅/悬浮显示已开启。"
            }
            snapshot.isOppo -> {
                if (items.isNotEmpty()) {
                    items += "在自启动管理与电池管理中允许后台启动并关闭省电限制。"
                }
            }
            snapshot.isVivo -> {
                if (items.isNotEmpty()) {
                    items += "在后台高耗电 / 自启动管理中允许后台运行。"
                }
            }
        }

        return items
    }

    private fun buildTitle(snapshot: Snapshot): String {
        return when {
            !snapshot.notificationsEnabled -> "应用通知被系统关闭"
            snapshot.needsExactAlarmPermission -> "精确闹钟权限"
            !snapshot.canUseFullScreenIntent -> "全屏闹钟权限"
            snapshot.isXiaomi -> "小米 / HyperOS 提醒加固"
            snapshot.isHuawei -> "华为 / Honor 提醒加固"
            snapshot.isOppo -> "OPPO / Realme 提醒加固"
            snapshot.isVivo -> "vivo / iQOO 提醒加固"
            !snapshot.ignoresBatteryOptimizations -> "提醒可靠性设置"
            else -> "${snapshot.manufacturerLabel} 提醒设置"
        }
    }

    private fun buildMessage(snapshot: Snapshot): String {
        return when {
            !snapshot.notificationsEnabled ->
                "系统当前已禁用本应用通知。即使闹钟已触发、Receiver 已运行，系统仍会直接拦截展示。"
            snapshot.needsExactAlarmPermission ->
                "未授予精确闹钟权限时，提醒可能延迟最多 1 小时。"
            !snapshot.canUseFullScreenIntent ->
                "系统当前未授予全屏闹钟权限。屏幕熄灭时，提醒可能只显示普通通知，无法直接拉起提醒界面。"
            snapshot.isXiaomi && hasXiaomiVisibilityRisk(snapshot) ->
                "当前设备可能已经成功触发提醒，但 MIUI / HyperOS 仍可能拦截锁屏、悬浮、后台通知或后台弹出页面。"
            snapshot.isHuawei ->
                "华为 / Honor 的 HarmonyOS / EMUI 常见问题是应用启动管理和后台活动限制，会导致提醒只在前台可靠。"
            snapshot.isChineseOem ->
                "当前设备的系统省电 / 自启动策略可能影响提醒稳定性，建议先完成以下加固。"
            else ->
                "请先检查以下提醒可靠性设置。"
        }
    }

    private fun hasXiaomiVisibilityRisk(snapshot: Snapshot): Boolean {
        return !snapshot.canShowOnLockScreen ||
            !snapshot.canShowFloatingNotification ||
            !snapshot.canSendBackgroundNotification
    }

    private fun actionLabel(action: Action): String {
        return when (action) {
            Action.EXACT_ALARM -> "闹钟权限"
            Action.FULL_SCREEN_INTENT -> "全屏闹钟"
            Action.BATTERY_OPTIMIZATION -> "电池设置"
            Action.OEM_PERMISSION_EDITOR -> "OEM 权限"
            Action.AUTO_START -> "自启动"
            Action.APP_NOTIFICATION_SETTINGS -> "通知设置"
        }
    }
}
