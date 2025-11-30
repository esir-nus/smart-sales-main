package com.smartsales.aitest.devicemanager

// 文件：app/src/main/java/com/smartsales/aitest/devicemanager/DeviceMediaGatewayImpl.kt
// 模块：:app
// 说明：用 MediaServerClient 实现 DeviceMediaGateway
// 作者：创建于 2025-11-20

import com.smartsales.aitest.MediaServerClient
import com.smartsales.aitest.MediaServerFile
import com.smartsales.core.util.Result
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import com.smartsales.feature.media.devicemanager.DeviceMediaGateway
import com.smartsales.feature.media.devicemanager.DeviceUploadSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceMediaGatewayImpl @Inject constructor(
    private val mediaServerClient: MediaServerClient
) : DeviceMediaGateway {
    override suspend fun fetchFiles(baseUrl: String): Result<List<DeviceMediaFile>> {
        return when (val result = mediaServerClient.fetchFiles(baseUrl)) {
            is Result.Success -> Result.Success(result.data.map { it.toDeviceMediaFile() })
            is Result.Error -> Result.Error(result.throwable)
        }
    }

    override suspend fun uploadFile(baseUrl: String, source: DeviceUploadSource): Result<Unit> {
        return when (source) {
            is DeviceUploadSource.AndroidUri -> mediaServerClient.uploadFile(baseUrl, source.uri)
        }
    }

    override suspend fun applyFile(baseUrl: String, fileName: String): Result<Unit> =
        mediaServerClient.applyFile(baseUrl, fileName)

    override suspend fun deleteFile(baseUrl: String, fileName: String): Result<Unit> =
        mediaServerClient.deleteFile(baseUrl, fileName)

    override suspend fun downloadFile(
        baseUrl: String,
        file: DeviceMediaFile
    ): Result<java.io.File> = mediaServerClient.downloadFile(baseUrl, file.toMediaServerFile())
}

private fun MediaServerFile.toDeviceMediaFile(): DeviceMediaFile =
    DeviceMediaFile(
        name = name,
        sizeBytes = sizeBytes,
        mimeType = mimeType,
        modifiedAtMillis = modifiedAtMillis,
        mediaUrl = mediaUrl,
        downloadUrl = downloadUrl,
        location = location,
        source = source
    )

private fun DeviceMediaFile.toMediaServerFile(): MediaServerFile =
    MediaServerFile(
        name = name,
        sizeBytes = sizeBytes,
        mimeType = mimeType,
        modifiedAtMillis = modifiedAtMillis,
        mediaUrl = mediaUrl,
        downloadUrl = downloadUrl,
        location = location,
        source = source
    )

@Module
@InstallIn(SingletonComponent::class)
interface DeviceMediaGatewayModule {
    @Binds
    fun bindDeviceMediaGateway(impl: DeviceMediaGatewayImpl): DeviceMediaGateway
}
