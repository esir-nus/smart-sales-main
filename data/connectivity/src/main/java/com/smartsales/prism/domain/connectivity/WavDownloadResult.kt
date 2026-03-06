package com.smartsales.prism.domain.connectivity

import java.io.File

/**
 * WAV 下载结果
 * 
 * @see connectivity-bridge/spec.md
 */
sealed class WavDownloadResult {
    /**
     * 下载成功
     */
    data class Success(
        /** 本地文件路径 */
        val localFile: File,
        
        /** Badge 上的原始文件名 */
        val originalFilename: String,
        
        /** 文件大小（字节） */
        val sizeBytes: Long
    ) : WavDownloadResult()
    
    /**
     * 下载失败
     */
    data class Error(
        val code: ErrorCode,
        val message: String
    ) : WavDownloadResult()
    
    enum class ErrorCode {
        /** Badge 未连接 */
        NOT_CONNECTED,
        
        /** Badge 离线（无 WiFi） */
        BADGE_OFFLINE,
        
        /** 文件不存在 */
        FILE_NOT_FOUND,
        
        /** 下载失败 */
        DOWNLOAD_FAILED,
        
        /** 磁盘空间不足 */
        DISK_FULL
    }
}
