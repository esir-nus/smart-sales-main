package com.smartsales.data.prismlib.di

import com.smartsales.data.prismlib.tools.DashscopeTingwuRunner
import com.smartsales.data.prismlib.tools.OkHttpUrlFetcher
import com.smartsales.data.prismlib.tools.QwenVLAnalyzer
import com.smartsales.domain.prism.core.tools.TingwuRunner
import com.smartsales.domain.prism.core.tools.UrlFetcher
import com.smartsales.domain.prism.core.tools.VisionAnalyzer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Input Tools 的 Hilt 绑定模块
 * 提供 UrlFetcher、VisionAnalyzer、TingwuRunner 的实现绑定
 * 
 * TODO(Phase 3): 取消注释 @InstallIn 以启用真实实现
 * 当前被禁用以避免与 FakeCoreModule 冲突
 */
@Module
// @InstallIn(SingletonComponent::class) // Disabled for Skeleton phase
abstract class InputToolsModule {

    @Binds
    @Singleton
    abstract fun bindUrlFetcher(impl: OkHttpUrlFetcher): UrlFetcher

    @Binds
    @Singleton
    abstract fun bindVisionAnalyzer(impl: QwenVLAnalyzer): VisionAnalyzer

    @Binds
    @Singleton
    abstract fun bindTingwuRunner(impl: DashscopeTingwuRunner): TingwuRunner
}

/**
 * 提供 OkHttpClient 实例
 * 
 * TODO(Phase 3): 取消注释 @InstallIn 或考虑使用 @Named 区分不同用途的 OkHttpClient
 * 当前被禁用以避免与 TingwuNetworkModule 的 OkHttpClient 冲突
 */
@Module
// @InstallIn(SingletonComponent::class) // Disabled for Skeleton phase
object InputToolsProviderModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
