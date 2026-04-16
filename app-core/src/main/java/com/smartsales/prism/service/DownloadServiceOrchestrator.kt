// File: app-core/src/main/java/com/smartsales/prism/service/DownloadServiceOrchestrator.kt
// Module: :app-core
// Summary: 下载服务编排器 — 统一管理前台服务的启动触发点
// Author: created on 2026-04-16
package com.smartsales.prism.service

import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 下载前台服务的编排器
 *
 * 提供统一的入口点来启动 DownloadForegroundService，解耦各下载源（BLE auto-download、manual sync）
 * 与 Service 具体实现的依赖。服务自身负责停止，不由编排器控制。
 */
@Singleton
class DownloadServiceOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 通知下载已启动 — 启动前台服务以保活进程
     *
     * 幂等操作：服务已运行时，再次调用仅触发 onStartCommand，不重复创建。
     */
    fun notifyDownloadStarting() {
        ContextCompat.startForegroundService(context, DownloadForegroundService.newIntent(context))
    }
}
