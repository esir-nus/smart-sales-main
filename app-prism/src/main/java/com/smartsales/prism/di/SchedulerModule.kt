package com.smartsales.prism.di

import com.smartsales.prism.data.memory.RealScheduleBoard
import com.smartsales.prism.data.scheduler.RealAlarmScheduler
import com.smartsales.prism.data.scheduler.RealInspirationRepository
import com.smartsales.prism.data.scheduler.RoomScheduledTaskRepository
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Scheduler DI Module
 * 
 * SchedulerLinter is @Inject @Singleton, so Hilt provides it automatically.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {

    @Binds
    @Singleton
    abstract fun bindScheduledTaskRepository(
        impl: RoomScheduledTaskRepository
    ): ScheduledTaskRepository

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(
        impl: RealAlarmScheduler
    ): AlarmScheduler

    @Binds
    @Singleton
    abstract fun bindScheduleBoard(
        impl: RealScheduleBoard
    ): ScheduleBoard

    @Binds
    @Singleton
    abstract fun bindInspirationRepository(
        impl: RealInspirationRepository
    ): InspirationRepository
}
