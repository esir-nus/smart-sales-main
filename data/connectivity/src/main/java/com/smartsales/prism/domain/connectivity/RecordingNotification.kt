package com.smartsales.prism.domain.connectivity

/**
 * 录音通知 — Badge 发送的事件
 * 
 * @see connectivity-bridge/spec.md
 */
sealed class RecordingNotification {
    /**
     * Badge 完成录音，文件可下载
     *
     * 触发条件：Badge 通过 BLE 发送 `log#YYYYMMDD_HHMMSS` 命令
     */
    data class RecordingReady(
        /** 文件名格式: log_YYYYMMDD_HHMMSS.wav */
        val filename: String
    ) : RecordingNotification()

    /**
     * Badge 音频文件就绪，仅下载不转写
     *
     * 触发条件：Badge 通过 BLE 发送 `rec#YYYYMMDD_HHMMSS` 命令
     */
    data class AudioRecordingReady(
        /** 文件名格式: rec_YYYYMMDD_HHMMSS.wav */
        val filename: String
    ) : RecordingNotification()
}
