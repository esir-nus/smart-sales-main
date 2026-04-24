package com.smartsales.prism.data.connectivity.legacy

/*
 * 相对路径: app-core/src/test/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerIngressSupportFilenameTest.kt
 * 模块: :app-core
 * 摘要: 验证徽章音频文件名归一化同时兼容带后缀和不带后缀的固件负载
 * Author: created on 2026-04-24
 */

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceConnectionManagerIngressSupportFilenameTest {

    @Test
    fun `toBadgeDownloadFilename returns empty for blank input`() {
        assertEquals("", "".toBadgeDownloadFilename())
        assertEquals("", "   ".toBadgeDownloadFilename())
    }

    @Test
    fun `toBadgeDownloadFilename normalizes bare and suffixed timestamp tokens`() {
        val timestamp = "20260423_111020"
        assertEquals("log_${timestamp}.wav", timestamp.toBadgeDownloadFilename())
        assertEquals("log_${timestamp}.wav", "${timestamp}.wav".toBadgeDownloadFilename())
        assertEquals("log_${timestamp}.wav", "${timestamp}.WAV".toBadgeDownloadFilename())
        assertEquals("log_${timestamp}.wav", "  ${timestamp}.wav  ".toBadgeDownloadFilename())
    }

    @Test
    fun `toBadgeDownloadFilename preserves already qualified log filenames`() {
        val timestamp = "20260423_111020"
        assertEquals("log_${timestamp}.wav", "log_${timestamp}.wav".toBadgeDownloadFilename())
        assertEquals("log_${timestamp}.wav", "log_${timestamp}".toBadgeDownloadFilename())
    }

    @Test
    fun `toBadgeDownloadFilename normalizes log hash payloads`() {
        val timestamp = "20260423_111020"
        assertEquals("log_${timestamp}.wav", "log#${timestamp}".toBadgeDownloadFilename())
        assertEquals("log_${timestamp}.wav", "log#${timestamp}.wav".toBadgeDownloadFilename())
    }

    @Test
    fun `toBadgeAudioFilename returns empty for blank input`() {
        assertEquals("", "".toBadgeAudioFilename())
        assertEquals("", "   ".toBadgeAudioFilename())
    }

    @Test
    fun `toBadgeAudioFilename normalizes bare and suffixed timestamp tokens`() {
        val timestamp = "20260423_111020"
        assertEquals("rec_${timestamp}.wav", timestamp.toBadgeAudioFilename())
        assertEquals("rec_${timestamp}.wav", "${timestamp}.wav".toBadgeAudioFilename())
        assertEquals("rec_${timestamp}.wav", "${timestamp}.WAV".toBadgeAudioFilename())
        assertEquals("rec_${timestamp}.wav", "  ${timestamp}.wav  ".toBadgeAudioFilename())
    }

    @Test
    fun `toBadgeAudioFilename preserves already qualified rec filenames`() {
        val timestamp = "20260423_111020"
        assertEquals("rec_${timestamp}.wav", "rec_${timestamp}.wav".toBadgeAudioFilename())
        assertEquals("rec_${timestamp}.wav", "rec_${timestamp}".toBadgeAudioFilename())
    }
}
