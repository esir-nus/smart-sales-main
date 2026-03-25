// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/AiCoreModule.kt
// 模块：:data:ai-core
// 说明：提供AI核心模块的Hilt依赖绑定和工厂
// 作者：创建于 2025-12-11
package com.smartsales.data.aicore

import com.google.gson.Gson
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.util.DefaultDispatcherProvider
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.data.aicore.debug.DashscopeDebugClient
import com.smartsales.data.aicore.debug.DashscopeDebugClientImpl
import com.smartsales.data.aicore.debug.DebugOrchestrator
import com.smartsales.data.aicore.debug.RealDebugOrchestrator
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.AiParaSettingsRepository
import com.smartsales.data.aicore.params.DefaultAiParaSettingsProvider
import com.smartsales.data.aicore.params.InMemoryAiParaSettingsRepository
import com.smartsales.data.aicore.posttingwu.PostTingwuTranscriptEnhancer
import com.smartsales.data.aicore.posttingwu.RealPostTingwuTranscriptEnhancer
import com.smartsales.data.aicore.tingwu.artifact.DefaultTingwuRawDumpDirectoryProvider
import com.smartsales.data.aicore.tingwu.artifact.TingwuRawDumpDirectoryProvider
import com.smartsales.data.aicore.tingwu.store.FileBasedTingwuJobStore
import com.smartsales.data.aicore.tingwu.store.TingwuJobStore
import javax.inject.Named

import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Optional
import javax.inject.Singleton
import okhttp3.EventListener

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
    abstract fun bindTingwuArtifactFetcher(
        impl: com.smartsales.data.aicore.tingwu.artifact.RealTingwuArtifactFetcher
    ): com.smartsales.data.aicore.tingwu.artifact.TingwuArtifactFetcher

    @Binds
    @Singleton
    abstract fun bindExportFileStore(
        impl: AndroidExportFileStore
    ): ExportFileStore

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

    @Binds
    @Singleton
    abstract fun bindDashscopeDebugClient(
        impl: DashscopeDebugClientImpl
    ): DashscopeDebugClient

    @Binds
    @Singleton
    abstract fun bindDebugOrchestrator(
        impl: RealDebugOrchestrator
    ): DebugOrchestrator

    @Binds
    @Singleton
    abstract fun bindPostTingwuTranscriptEnhancer(
        impl: RealPostTingwuTranscriptEnhancer
    ): PostTingwuTranscriptEnhancer

    @Binds
    @Singleton
    abstract fun bindPipelineTracer(
        impl: com.smartsales.data.aicore.debug.RealPipelineTracer
    ): com.smartsales.data.aicore.debug.PipelineTracer

    @Binds
    @Singleton
    abstract fun bindTingwuSubmissionService(
        impl: com.smartsales.data.aicore.tingwu.submission.RealTingwuSubmissionService
    ): com.smartsales.data.aicore.tingwu.submission.TingwuSubmissionService

    @Binds
    @Singleton
    abstract fun bindPollingLoop(
        impl: com.smartsales.data.aicore.tingwu.polling.TingwuPollingLoop
    ): com.smartsales.data.aicore.tingwu.polling.PollingLoop

    @Binds
    @Singleton
    abstract fun bindTingwuApiRepository(
        impl: com.smartsales.data.aicore.tingwu.polling.RealTingwuApiRepository
    ): com.smartsales.data.aicore.tingwu.polling.TingwuApiRepository


    @Binds
    @Singleton
    abstract fun bindTranscriptProcessor(
        impl: com.smartsales.data.aicore.tingwu.processor.TingwuTranscriptProcessor
    ): com.smartsales.data.aicore.tingwu.processor.TranscriptProcessor

    @Binds
    @Singleton
    abstract fun bindPublisher(
        impl: com.smartsales.data.aicore.tingwu.publisher.TranscriptPublisher
    ): com.smartsales.data.aicore.tingwu.publisher.Publisher

    @Binds
    @Singleton
    abstract fun bindAiParaSettingsProvider(
        impl: DefaultAiParaSettingsProvider
    ): AiParaSettingsProvider

    @Binds
    @Singleton
    abstract fun bindTingwuIdentityHintResolver(
        impl: com.smartsales.data.aicore.tingwu.identity.RealTingwuIdentityHintResolver
    ): com.smartsales.data.aicore.tingwu.identity.TingwuIdentityHintResolver

    @Binds
    @Singleton
    abstract fun bindAiParaSettingsRepository(
        impl: InMemoryAiParaSettingsRepository,
    ): AiParaSettingsRepository



    @Binds
    @Singleton
    abstract fun bindTingwuRawDumpDirectoryProvider(
        impl: DefaultTingwuRawDumpDirectoryProvider,
    ): TingwuRawDumpDirectoryProvider
    // Tingwu 调试痕迹存储
    // 仅调试 HUD 使用，生产逻辑不依赖

    @Binds
    @Singleton
    abstract fun bindNetworkChecker(
        impl: AndroidNetworkChecker
    ): NetworkChecker

    @Binds
    @Singleton
    abstract fun bindTingwuJobStore(
        impl: FileBasedTingwuJobStore
    ): TingwuJobStore

    @Binds
    @Singleton
    abstract fun bindMetaHubWriter(
        impl: com.smartsales.data.aicore.tingwu.metadata.RealMetaHubWriter
    ): com.smartsales.data.aicore.tingwu.metadata.MetaHubWriter

    @Binds
    @Singleton
    abstract fun bindResultProcessor(
        impl: com.smartsales.data.aicore.tingwu.result.RealResultProcessor
    ): com.smartsales.data.aicore.tingwu.result.ResultProcessor

    @Binds
    @Singleton
    abstract fun bindEnhancerIntegration(
        impl: com.smartsales.data.aicore.tingwu.enhancer.RealEnhancerIntegration
    ): com.smartsales.data.aicore.tingwu.enhancer.EnhancerIntegration


    @BindsOptionalOf
    abstract fun optionalAiCoreConfig(): AiCoreConfig

    companion object {
        // 提供统一 Gson 实例，供转写解析与增强使用
        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()

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
            real: TingwuRunner
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
        fun provideExportOrchestrator(
            metaHub: MetaHub,
            exportManager: ExportManager,
            exportFileStore: ExportFileStore,
            dispatchers: DispatcherProvider
        ): ExportOrchestrator = RealExportOrchestrator(
            metaHub = metaHub,
            exportManager = exportManager,
            exportFileStore = exportFileStore,
            dispatchers = dispatchers
        )

        @Provides
        @Singleton
        fun provideTranscriptOrchestrator(
            metaHub: MetaHub,
            dispatchers: DispatcherProvider,
            aiChatService: AiChatService
        ): TranscriptOrchestrator = RealTranscriptOrchestrator(
            metaHub = metaHub,
            dispatchers = dispatchers,
            aiChatService = aiChatService
        )

        @Provides
        @Singleton
        fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider

        @Provides
        @Singleton
        fun provideTingwuDns(
            resolver: HttpDnsResolver?,
            optionalConfig: Optional<AiCoreConfig>
        ): okhttp3.Dns {
            val config = optionalConfig.orElse(AiCoreConfig())
            return if (config.enableTingwuHttpDns && resolver != null) {
                com.smartsales.data.aicore.tingwu.api.TingwuDns(resolver)
            } else {
                okhttp3.Dns.SYSTEM
            }
        }

        @Provides
        @Singleton
        fun provideOkHttpEventListenerFactory(): EventListener.Factory =
            com.smartsales.data.aicore.tingwu.api.TingwuOkHttpEventListener.Factory()

        @Provides
        @Singleton
        fun provideDefaultHttpDnsResolver(): HttpDnsResolver? = null

        @Provides
        @Singleton
        @Named("TingwuStoreDir")
        fun provideTingwuStoreDir(@dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context): java.io.File {
            // Permanent storage for job manifest and batch artifacts
            val dir = java.io.File(context.filesDir, "tingwu_jobs")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    }
}
