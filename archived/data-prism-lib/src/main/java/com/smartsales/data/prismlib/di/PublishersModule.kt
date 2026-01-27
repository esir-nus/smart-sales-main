package com.smartsales.data.prismlib.di

import com.smartsales.data.prismlib.publishers.AnalystPublisher
import com.smartsales.data.prismlib.publishers.ChatPublisher
import com.smartsales.data.prismlib.publishers.SchedulePublisher
import com.smartsales.domain.prism.core.ModePublisher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Mode Publishers 的 Hilt 绑定模块
 * 通过 @Named 限定符区分不同模式的发布器
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PublishersModule {

    @Binds
    @Singleton
    @Named("coach")
    abstract fun bindChatPublisher(impl: ChatPublisher): ModePublisher

    @Binds
    @Singleton
    @Named("analyst")
    abstract fun bindAnalystPublisher(impl: AnalystPublisher): ModePublisher

    @Binds
    @Singleton
    @Named("scheduler")
    abstract fun bindSchedulePublisher(impl: SchedulePublisher): ModePublisher
}
