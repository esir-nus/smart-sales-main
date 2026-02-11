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
        val targets = autoStartTargets[manufacturer] ?: return openAppInfo(context)

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
