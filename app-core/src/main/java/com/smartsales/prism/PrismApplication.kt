package com.smartsales.prism

import android.app.Application
import com.smartsales.prism.data.connectivity.registry.DeviceRegistryManager
import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.data.audio.SimAudioRepository
import com.smartsales.prism.data.audio.SimBadgeAudioAutoDownloader
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Prism Clean Room Application
 * 
 * This is a standalone application for testing the Prism architecture
 * in complete isolation from legacy code.
 */
@HiltAndroidApp
class PrismApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val entryPoint = EntryPointAccessors.fromApplication(
            this,
            PrismApplicationEntryPoint::class.java
        )

        // 加载设备注册表并初始化自动重连策略（包括手动断开标记）。
        // 必须在 ViewModel 首次触发 scheduleAutoReconnect 之前完成，
        // 否则已手动断开的设备在重启后会被意外自动重连。
        entryPoint.deviceRegistryManager().initializeOnLaunch()

        // 预热 SIM 音频仓库，确保抽屉命名空间在应用期内可接收自动管道写入。
        entryPoint.simAudioRepository()

        // 启动 rec# 自动下载器，监听 BLE 音频通知并后台同步到抽屉。
        entryPoint.simBadgeAudioAutoDownloader()

        // Android 主线固定启用 scheduler 音频管道。
        entryPoint.badgeAudioPipeline()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PrismApplicationEntryPoint {
    fun deviceRegistryManager(): DeviceRegistryManager
    fun badgeAudioPipeline(): BadgeAudioPipeline
    fun simAudioRepository(): SimAudioRepository
    fun simBadgeAudioAutoDownloader(): SimBadgeAudioAutoDownloader
}
