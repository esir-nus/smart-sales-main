package com.smartsales.prism

import android.app.Application
import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.data.audio.SimAudioRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Prism Clean Room Application
 * 
 * This is a standalone application for testing the Prism architecture
 * in complete isolation from legacy code.
 */
@HiltAndroidApp
class PrismApplication : Application() {
    
    // 强制 Hilt 在 Application 创建时实例化 BadgeAudioPipeline
    // 使其 init{} 块启动录音通知监听（BLE log# → 下载 → 转写 → 调度）
    @Inject lateinit var badgeAudioPipeline: BadgeAudioPipeline

    // 预热 SIM 音频仓库，确保抽屉命名空间在应用期内可接收自动管道写入。
    @Inject lateinit var simAudioRepository: SimAudioRepository
}
