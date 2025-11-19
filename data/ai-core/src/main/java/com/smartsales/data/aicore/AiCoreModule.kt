package com.smartsales.data.aicore

import com.smartsales.core.util.DefaultDispatcherProvider
import com.smartsales.core.util.DispatcherProvider
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Optional
import javax.inject.Singleton
import okhttp3.EventListener

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/AiCoreModule.kt
// 模块：:data:ai-core
// 说明：提供AI核心模块的Hilt依赖绑定和工厂
// 作者：创建于 2025-11-16
@Module
@InstallIn(SingletonComponent::class)
abstract class AiCoreModule {
    @Binds
    @Singleton
    abstract fun bindDashscopeClient(impl: DefaultDashscopeClient): DashscopeClient

    @Binds
    @Singleton
    abstract fun bindDashscopeCredentialsProvider(
        impl: BuildConfigDashscopeCredentialsProvider
    ): DashscopeCredentialsProvider

    @Binds
    @Singleton
    abstract fun bindTingwuCredentialsProvider(
        impl: BuildConfigTingwuCredentialsProvider
    ): TingwuCredentialsProvider

    @Binds
    @Singleton
    abstract fun bindExportFileStore(
        impl: AndroidExportFileStore
    ): ExportFileStore

    @Binds
    @Singleton
    abstract fun bindOssUploadClient(
        impl: RealOssUploadClient
    ): OssUploadClient

    @Binds
    @Singleton
    abstract fun bindOssCredentialsProvider(
        impl: BuildConfigOssCredentialsProvider
    ): OssCredentialsProvider

    @Binds
    @Singleton
    abstract fun bindOssSignedUrlProvider(
        impl: RealOssSignedUrlProvider
    ): OssSignedUrlProvider

    @BindsOptionalOf
    abstract fun optionalAiCoreConfig(): AiCoreConfig

    companion object {
        @Provides
        @Singleton
        fun provideAiChatService(
            optionalConfig: Optional<AiCoreConfig>,
            fake: FakeAiChatService,
            dashscope: DashscopeAiChatService
        ): AiChatService {
            val preferFake = optionalConfig.orElse(AiCoreConfig()).preferFakeAiChat
            return if (preferFake) fake else dashscope
        }

        @Provides
        @Singleton
        fun provideTingwuCoordinator(
            optionalConfig: Optional<AiCoreConfig>,
            fake: FakeTingwuCoordinator,
            real: RealTingwuCoordinator
        ): TingwuCoordinator {
            val preferFake = optionalConfig.orElse(AiCoreConfig()).preferFakeTingwu
            return if (preferFake) fake else real
        }

        @Provides
        @Singleton
        fun provideExportManager(
            optionalConfig: Optional<AiCoreConfig>,
            fake: FakeExportManager,
            real: RealExportManager
        ): ExportManager {
            val preferFake = optionalConfig.orElse(AiCoreConfig()).preferFakeExport
            return if (preferFake) fake else real
        }

        @Provides
        @Singleton
        fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider

        @Provides
        @Singleton
        fun provideTingwuDns(
            @javax.annotation.Nullable resolver: HttpDnsResolver?,
            optionalConfig: Optional<AiCoreConfig>
        ): okhttp3.Dns {
            val config = optionalConfig.orElse(AiCoreConfig())
            return if (config.enableTingwuHttpDns && resolver != null) {
                com.smartsales.data.aicore.tingwu.TingwuDns(resolver)
            } else {
                okhttp3.Dns.SYSTEM
            }
        }

        @Provides
        @Singleton
        fun provideOkHttpEventListenerFactory(): EventListener.Factory =
            com.smartsales.data.aicore.tingwu.TingwuOkHttpEventListener.Factory()

        @Provides
        @Singleton
        @javax.annotation.Nullable
        fun provideDefaultHttpDnsResolver(): HttpDnsResolver? = null

    }
}
