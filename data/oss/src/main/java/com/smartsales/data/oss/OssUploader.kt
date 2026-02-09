package com.smartsales.data.oss

import java.io.File

/**
 * OSS 文件上传接口。
 *
 * 消费者只需调用 upload()，不需要了解 OSSClient 内部实现。
 */
interface OssUploader {

    /**
     * 上传本地文件到 OSS，返回公开可访问的 URL。
     *
     * @param file 本地文件
     * @param objectKey OSS 对象路径 (e.g., "smartsales/audio/2026-02-09/recording.wav")
     * @return 上传结果，包含公开 URL 或错误信息
     */
    suspend fun upload(file: File, objectKey: String): OssUploadResult
}
