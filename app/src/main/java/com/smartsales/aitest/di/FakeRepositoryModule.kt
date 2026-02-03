package com.smartsales.aitest.di

// 文件：app/src/main/java/com/smartsales/aitest/di/FakeRepositoryModule.kt
// 模块：:app
// 说明：Repository 的 Fake 实现模块 — 暂时禁用
//       问题: Prism interfaces 在 :app-prism (application module) 中，无法被 :app 依赖
// 作者：暂时禁用于 2026-01-30

// import com.smartsales.prism.domain.memory.*
// import com.smartsales.prism.data.fakes.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 的 Fake 实现模块 — 暂时禁用
 * 
 * 问题: RelevancyRepository, MemoryEntryRepository 等接口定义在 :app-prism,
 *       而 :app-prism 是 application module, 不能被 :app 依赖
 * 
 * TODO: 将 prism.domain.memory 包移至独立 library module
 */
@Module
@InstallIn(SingletonComponent::class)
object FakeRepositoryModule {
    
    // @Provides
    // @Singleton
    // fun provideRelevancyRepository(): RelevancyRepository = FakeRelevancyRepository()
    
    // @Provides
    // @Singleton
    // fun provideMemoryEntryRepository(): MemoryEntryRepository = FakeMemoryEntryRepository()
}
