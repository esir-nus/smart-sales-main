package com.smartsales.data.habit

import com.smartsales.prism.domain.habit.UserHabitRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class HabitDataModule {
    @Binds
    abstract fun bindUserHabitRepository(impl: RoomUserHabitRepository): UserHabitRepository
}
