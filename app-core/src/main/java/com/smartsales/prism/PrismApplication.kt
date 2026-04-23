package com.smartsales.prism

import android.app.Application
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

        // 预热 SIM 音频仓库，确保抽屉命名空间在应用期内可接收自动管道写入。
        entryPoint.simAudioRepository()

        // 启动 rec# 自动下载器，监听 BLE 音频通知并后台同步到抽屉。
        entryPoint.simBadgeAudioAutoDownloader()

        // Android 主线保持单一 debug 包，调度链路默认启用。
        if (AppFlavor.schedulerEnabled) {
            entryPoint.badgeAudioPipeline()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface PrismApplicationEntryPoint {
    fun badgeAudioPipeline(): BadgeAudioPipeline
    fun simAudioRepository(): SimAudioRepository
    fun simBadgeAudioAutoDownloader(): SimBadgeAudioAutoDownloader
}
