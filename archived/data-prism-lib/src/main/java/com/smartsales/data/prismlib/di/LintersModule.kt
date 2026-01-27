package com.smartsales.data.prismlib.di

import com.smartsales.data.prismlib.linters.LinterRegistry
import com.smartsales.data.prismlib.linters.RealEntityLinter
import com.smartsales.data.prismlib.linters.RealPlanLinter
import com.smartsales.data.prismlib.linters.RealRelevancyLinter
import com.smartsales.data.prismlib.linters.RealSchedulerLinter
import com.smartsales.data.prismlib.notifications.RealMemoryCenterNotifier
import com.smartsales.domain.prism.core.MemoryCenterNotifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Linters 和 MemoryCenterNotifier 的 Hilt 绑定模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LintersModule {

    @Binds
    @Singleton
    abstract fun bindMemoryCenterNotifier(impl: RealMemoryCenterNotifier): MemoryCenterNotifier
}

/**
 * LinterRegistry 提供模块
 */
@Module
@InstallIn(SingletonComponent::class)
object LintersProviderModule {

    @Provides
    @Singleton
    fun provideLinterRegistry(
        entityLinter: RealEntityLinter,
        planLinter: RealPlanLinter,
        schedulerLinter: RealSchedulerLinter,
        relevancyLinter: RealRelevancyLinter
    ): LinterRegistry {
        return LinterRegistry(
            entityLinter = entityLinter,
            planLinter = planLinter,
            schedulerLinter = schedulerLinter,
            relevancyLinter = relevancyLinter
        )
    }
}
