// File: app-core/src/main/java/com/smartsales/prism/service/DownloadForegroundService.kt
// Module: :app-core
// Summary: 下载保活前台服务 — 后台下载期间持有前台服务 wakelock，防止进程被 OEM 系统杀死
// Author: created on 2026-04-16
package com.smartsales.prism.service

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.smartsales.prism.R
import com.smartsales.prism.data.audio.SimAudioRepositoryRuntime
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.notification.NotificationAction
import com.smartsales.prism.domain.notification.NotificationService
import com.smartsales.prism.domain.notification.NotificationPriority
import com.smartsales.prism.domain.notification.PrismNotificationChannel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 下载保活前台服务
 *
 * 当后台下载队列非空时启动此服务，在系统通知栏显示持续通知以保持进程生命周期。
 * 服务观察 audioFiles 流，当所有下载条目离开 DOWNLOADING/QUEUED 状态后自动停止。
 *
 * **OEM 兼容性**:
 * - Xiaomi MIUI / HyperOS: 若已豁免电池优化，前台服务将存活；否则可能被杀死
 * - OPPO ColorOS / Vivo OriginOS: 需将应用加入"受保护应用"或"后台活动"名单
 * - Huawei EMUI: 需开启"启动管理 > 允许后台活动"
 * - 现有 OemCompat + ReminderReliabilityAdvisor 基础设施已覆盖这些提示
 *
 * **架构**: 生命周期锚点模式，不改变下载协程执行上下文
 * - 下载逻辑仍在 SimAudioRepositorySyncSupport.repositoryScope 和 SimBadgeAudioAutoDownloader.scope 中运行
 * - 本服务仅观察 runtime.audioFiles，更新通知，当条件满足时自动停止
 *
 * **停止条件**: 当 runtime.audioFiles 中不存在 DOWNLOADING 或 QUEUED 状态的条目，
 * 且 runtime.queuedBadgeDownloads 为空时，等待 800 ms 去抖动后停止。
 * 800 ms 的窗口用于规避 QUEUED → DOWNLOADING 过程中 StateFlow 发出中间状态时被观察到的竞态。
 */
@AndroidEntryPoint
class DownloadForegroundService : LifecycleService() {

    @Inject
    lateinit var runtime: SimAudioRepositoryRuntime

    @Inject
    lateinit var notificationService: NotificationService

    companion object {
        private const val TAG = "DownloadService"
        // NOTIFICATION_ID 不能是 const，因为 hashCode() 不是编译时常量
        private val NOTIFICATION_ID = "download_foreground_service".hashCode()

        fun newIntent(context: Context) = Intent(context, DownloadForegroundService::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DownloadForegroundService created")

        // 启动前台服务 — 必须在 5 秒内调用（Android 12+ 要求）
        val notification = buildDownloadNotification(
            currentFile = null,
            completedCount = 0,
            totalCount = 0
        )

        // API 34+ 需要指定 SERVICE_TYPE_DATA_SYNC；低版本此参数被忽略
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }

        Log.d(TAG, "Foreground service started with notification ID: $NOTIFICATION_ID")

        // 观察 audioFiles 流，更新通知和停止条件
        lifecycleScope.launch {
            var lastStopCheckTime = 0L
            runtime.audioFiles.collect { audioFiles ->
                val downloadingCount = audioFiles.count { it.localAvailability == AudioLocalAvailability.DOWNLOADING }
                val queuedCount = audioFiles.count { it.localAvailability == AudioLocalAvailability.QUEUED }
                val completedCount = audioFiles.count { it.localAvailability == AudioLocalAvailability.READY }

                Log.d(TAG, "Audio files state: downloading=$downloadingCount, queued=$queuedCount, ready=$completedCount, " +
                    "queueSize=${runtime.queuedBadgeDownloads.size}")

                // 获取当前正在下载的文件名（用于通知显示）
                val currentFile = audioFiles.firstOrNull { it.localAvailability == AudioLocalAvailability.DOWNLOADING }

                // 更新通知
                val updatedNotification = buildDownloadNotification(
                    currentFile = currentFile?.filename,
                    completedCount = completedCount,
                    totalCount = downloadingCount + queuedCount + completedCount
                )
                updateForegroundNotification(updatedNotification)

                // 检查停止条件：没有正在下载或等待的条目，且队列为空
                val shouldStop = downloadingCount == 0 && queuedCount == 0 && runtime.queuedBadgeDownloads.isEmpty()

                if (shouldStop) {
                    val now = System.currentTimeMillis()
                    if (lastStopCheckTime == 0L) {
                        lastStopCheckTime = now
                        Log.d(TAG, "Stop condition met, scheduling 800ms debounce")
                    } else if (now - lastStopCheckTime >= 800L) {
                        Log.d(TAG, "Debounce expired, stopping foreground service")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        lastStopCheckTime = 0L
                    }
                } else {
                    lastStopCheckTime = 0L  // 重置去抖动计数器
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called (idempotent)")
        // 返回 START_STICKY 以便进程被杀后重启时恢复服务
        // 由于生命周期锚点模式，下载队列由 recovery 机制重建
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 构建下载进度通知
     *
     * **显示内容**:
     * - 标题: "正在后台同步录音"
     * - 内容: 当前文件名 + 完成进度（已完成 X / 共 Y 条）
     * - 进度条: 不可用则显示不确定进度
     * - 属性: ongoing=true（用户不可关闭）
     */
    private fun buildDownloadNotification(
        currentFile: String?,
        completedCount: Int,
        totalCount: Int
    ): android.app.Notification {
        val contentText = when {
            currentFile != null && totalCount > 0 -> {
                // 提取文件名，移除扩展名以保证简洁
                val displayName = currentFile.substringBeforeLast(".")
                "正在下载 $displayName（已完成 $completedCount / 共 $totalCount 条）"
            }
            totalCount > 0 -> "已完成 $completedCount / $totalCount 条"
            else -> "正在同步录音文件…"
        }

        val progressPercent = if (totalCount > 0) {
            (completedCount * 100) / totalCount
        } else {
            0
        }

        val notification = NotificationCompat.Builder(this, PrismNotificationChannel.BADGE_DOWNLOAD_PROGRESS.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("正在后台同步录音")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOngoing(true)  // 用户不可关闭
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setProgress(100, progressPercent, totalCount == 0)  // 进度条

        // 点击打开主界面
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        launchIntent?.let {
            notification.setContentIntent(
                android.app.PendingIntent.getActivity(
                    this,
                    NOTIFICATION_ID,
                    it,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        return notification.build()
    }

    private fun updateForegroundNotification(notification: android.app.Notification) {
        if (!notificationService.hasPermission()) {
            Log.w(TAG, "Notification permission missing, skip foreground notification update")
            return
        }
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Foreground notification update rejected: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DownloadForegroundService destroyed")
    }
}
