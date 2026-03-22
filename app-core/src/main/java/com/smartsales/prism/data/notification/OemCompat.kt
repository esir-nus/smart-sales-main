package com.smartsales.prism.data.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * 中国 OEM 兼容工具
 *
 * 国产 ROM (MIUI, EMUI, ColorOS, OriginOS) 会主动杀死后台应用、
 * 取消 AlarmManager 闹钟、拦截 BOOT_COMPLETED。
 * 此工具提供运行时检测和设置引导。
 *
 * 架构: 单 APK + Build.MANUFACTURER 运行时检测
 */
object OemCompat {

    private const val TAG = "OemCompat"

    private val manufacturer: String get() = Build.MANUFACTURER.lowercase()

    val isXiaomi get() = manufacturer in listOf("xiaomi", "redmi", "poco")
    val isHuawei get() = manufacturer in listOf("huawei", "honor")
    val isOppo get() = manufacturer in listOf("oppo", "realme", "oneplus")
    val isVivo get() = manufacturer in listOf("vivo", "iqoo")
    val isChineseOem get() = isXiaomi || isHuawei || isOppo || isVivo

    // ---- Defense Layer 1: 电池优化豁免 ----

    /**
     * 检查是否已豁免电池优化
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * 创建请求电池优化豁免的 Intent
     * 调用方负责 startActivity
     */
    fun createBatteryOptimizationIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * 打开电池优化豁免设置
     */
    fun openBatteryOptimizationSettings(context: Context): Boolean {
        return try {
            val intent = createBatteryOptimizationIntent(context).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "已打开电池优化豁免设置")
            true
        } catch (e: Exception) {
            Log.w(TAG, "无法打开电池优化设置: ${e.message}")
            openAppInfo(context)
        }
    }

    // ---- Defense Layer 3: 精确闹钟权限 ----

    /**
     * Android 12+ 需要 SCHEDULE_EXACT_ALARM 权限
     * 未授予时 AlarmManager 降级为 setAndAllowWhileIdle，最多延迟 1 小时
     */
    fun needsExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        return !am.canScheduleExactAlarms()
    }

    /**
     * 打开精确闹钟权限设置页
     * Android 12+: ACTION_REQUEST_SCHEDULE_EXACT_ALARM
     */
    fun openExactAlarmSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return try {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "已打开精确闹钟权限设置")
            true
        } catch (e: Exception) {
            Log.w(TAG, "无法打开闹钟权限设置: ${e.message}")
            openAppInfo(context)
        }
    }

    // ---- Defense Layer 4: MIUI 锁屏显示权限 ----

    /**
     * MIUI 有独立的"锁屏显示"权限 — 无法通过 API 检测状态
     * 只能引导用户到 MIUI 权限编辑页手动开启
     *
     * 路径: 设置 > 应用管理 > [App] > 其他权限 > 锁屏显示
     *
     * @return true 如果成功打开了设置页
     */
    fun openLockScreenPermission(context: Context): Boolean {
        if (!isXiaomi) return false
        return try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "已打开 MIUI 锁屏权限设置")
            true
        } catch (e: Exception) {
            Log.w(TAG, "MIUI 权限编辑页不可用: ${e.message}")
            openAppInfo(context)
        }
    }

    /**
     * 打开应用通知设置页
     */
    fun openNotificationSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra("app_package", context.packageName)
                putExtra("app_uid", context.applicationInfo.uid)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "已打开应用通知设置")
            true
        } catch (e: Exception) {
            Log.w(TAG, "无法打开应用通知设置: ${e.message}")
            openAppInfo(context)
        }
    }

    /**
     * 检测 MIUI 锁屏显示权限 (op 10020)
     *
     * MIUI 把锁屏显示藏在 AppOps 里，标准 API 读不到。
     * 反射 checkOpNoThrow(10020, uid, pkg)。
     * 非小米设备直接返回 true（无此限制）。
     * 反射失败时降级检查 NotificationChannel.lockscreenVisibility。
     */
    fun canShowOnLockScreen(context: Context): Boolean {
        if (!isXiaomi) return true
        return checkMiuiOp(context, MIUI_OP_SHOW_ON_LOCK_SCREEN)
            ?: fallbackCheckLockscreenVisibility(context)
    }

    /**
     * 检测 MIUI 悬浮通知权限 (op 10016)
     *
     * 即使设置了 fullScreenIntent，用户关闭"悬浮通知"后横幅不弹。
     * 反射 checkOpNoThrow(10016, uid, pkg)。
     */
    fun canShowFloatingNotification(context: Context): Boolean {
        if (!isXiaomi) return true
        return checkMiuiOp(context, MIUI_OP_FLOATING_NOTIFICATION) ?: true
    }

    /**
     * 检测 HyperOS 后台发送本地通知权限 (op 10021)
     *
     * HyperOS 默认拒绝后台发送本地通知。
     * 反射 checkOpNoThrow(10021, uid, pkg)。
     * 非小米设备直接返回 true。
     */
    fun canSendBackgroundNotification(context: Context): Boolean {
        if (!isXiaomi) return true
        return checkMiuiOp(context, MIUI_OP_BACKGROUND_NOTIFICATION) ?: true
    }

    // MIUI/HyperOS AppOps 操作码
    private const val MIUI_OP_FLOATING_NOTIFICATION = 10016
    private const val MIUI_OP_SHOW_ON_LOCK_SCREEN = 10020
    private const val MIUI_OP_BACKGROUND_NOTIFICATION = 10021

    /**
     * 通用 MIUI AppOps 反射检测
     * @return true=已授权, false=未授权, null=反射失败
     */
    private fun checkMiuiOp(context: Context, opCode: Int): Boolean? {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE)
                as android.app.AppOpsManager
            val method = android.app.AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.java, Int::class.java, String::class.java
            )
            val uid = android.os.Process.myUid()
            val result = method.invoke(appOps, opCode, uid, context.packageName) as Int
            (result == android.app.AppOpsManager.MODE_ALLOWED).also {
                Log.d(TAG, "MIUI op $opCode 检测: ${if (it) "已授权" else "未授权"}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "MIUI op $opCode 反射失败: ${e.message}")
            null
        }
    }

    /**
     * 反射降级: 通过 NotificationChannel.lockscreenVisibility 判断
     * 不完全准确但可用
     */
    private fun fallbackCheckLockscreenVisibility(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
            // 检查任务提醒渠道
            val channel = nm.getNotificationChannel(
                com.smartsales.prism.domain.notification.PrismNotificationChannel.TASK_REMINDER_DEADLINE.channelId
            )
            channel?.lockscreenVisibility != android.app.Notification.VISIBILITY_SECRET
        } catch (e: Exception) {
            true // 无法判断时假设可用
        }
    }

    // ---- Defense Layer 2: 自启动引导 ----

    /**
     * OEM 自启动管理 Activity 映射
     * 全部 try/catch 降级到系统应用信息页
     */
    private data class AutoStartTarget(
        val packageName: String,
        val className: String
    )

    private val autoStartTargets: Map<String, List<AutoStartTarget>> = mapOf(
        "xiaomi" to listOf(
            AutoStartTarget("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        ),
        "redmi" to listOf(
            AutoStartTarget("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        ),
        "poco" to listOf(
            AutoStartTarget("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        ),
        "huawei" to listOf(
            AutoStartTarget("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            AutoStartTarget("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
        ),
        "honor" to listOf(
            AutoStartTarget("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
        ),
        "oppo" to listOf(
            AutoStartTarget("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            AutoStartTarget("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
        ),
        "realme" to listOf(
            AutoStartTarget("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
        ),
        "oneplus" to listOf(
            AutoStartTarget("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
        ),
        "vivo" to listOf(
            AutoStartTarget("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            AutoStartTarget("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
        ),
        "iqoo" to listOf(
            AutoStartTarget("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
        )
    )

    /**
     * 尝试打开 OEM 自启动设置
     * 如果 OEM 页面不可用，降级到系统应用信息页
     * @return true 如果成功打开了某个页面
     */
    fun openAutoStartSettings(context: Context): Boolean {
        // 优先尝试厂商特定的 Intent
        val targets = autoStartTargets[manufacturer] ?: autoStartTargets.values.flatten()
        
        for (target in targets) {
            try {
                val intent = Intent().apply {
                    component = ComponentName(target.packageName, target.className)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.d(TAG, "已打开自启动设置: ${target.packageName}")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "自启动页面不可用: ${target.className}, 尝试下一个")
            }
        }

        // 所有 OEM 页面都失败，降级到系统应用信息
        return openAppInfo(context)
    }

    /**
     * 降级: 打开系统应用信息页
     */
    private fun openAppInfo(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.d(TAG, "降级到系统应用信息页")
            true
        } catch (e: Exception) {
            Log.e(TAG, "无法打开任何设置页面: ${e.message}")
            false
        }
    }
}
