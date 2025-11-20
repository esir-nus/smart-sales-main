package com.smartsales.aitest

import android.content.Context
import com.smartsales.core.util.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus

// 文件：aiFeatureTestApp/src/main/java/com/smartsales/aitest/FeatureTestAppModule.kt
// 模块：:aiFeatureTestApp
// 说明：为特性测试应用提供全局协程作用域
// 作者：创建于 2025-11-15
@Module
@InstallIn(SingletonComponent::class)
object FeatureTestAppModule {
    @Provides
    @Singleton
    fun provideAppScope(dispatcherProvider: DispatcherProvider): CoroutineScope {
        return CoroutineScope(SupervisorJob()) + dispatcherProvider.default
    }

    @Provides
    @Singleton
    fun provideMediaServerClient(
        @ApplicationContext context: Context
    ): MediaServerClient = MediaServerClient(context)
}
