package com.smartsales.data.memory

import com.smartsales.prism.domain.memory.MemoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MemoryDataModule {
    @Binds
    abstract fun bindMemoryRepository(impl: RoomMemoryRepository): MemoryRepository
}
