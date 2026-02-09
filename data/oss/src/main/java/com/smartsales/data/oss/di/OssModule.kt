package com.smartsales.data.oss.di

import android.content.Context
import com.alibaba.sdk.android.oss.OSS
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider
import com.smartsales.data.oss.BuildConfig
import com.smartsales.data.oss.OssUploader
import com.smartsales.data.oss.RealOssUploader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * OSS Hilt 依赖注入模块。
 *
 * 提供 OSSClient 单例和 OssUploader 绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
object OssModule {

    @Provides
    @Named("ossBucketName")
    fun provideBucketName(): String = BuildConfig.OSS_BUCKET_NAME

    @Provides
    @Named("ossEndpoint")
    fun provideEndpoint(): String = BuildConfig.OSS_ENDPOINT

    @Provides
    @Singleton
    fun provideOssClient(
        @ApplicationContext context: Context
    ): OSS {
        val credentialProvider = OSSPlainTextAKSKCredentialProvider(
            BuildConfig.OSS_ACCESS_KEY_ID,
            BuildConfig.OSS_ACCESS_KEY_SECRET
        )
        return OSSClient(context, BuildConfig.OSS_ENDPOINT, credentialProvider)
    }

    @Provides
    @Singleton
    fun provideOssUploader(
        ossClient: OSS,
        @Named("ossBucketName") bucketName: String,
        @Named("ossEndpoint") endpoint: String
    ): OssUploader = RealOssUploader(ossClient, bucketName, endpoint)
}
