package com.smartsales.prism.di

import com.smartsales.prism.data.notification.RealNotificationService
import com.smartsales.prism.domain.notification.NotificationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 通知服务 DI 绑定
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationService(
        impl: RealNotificationService
    ): NotificationService
}
