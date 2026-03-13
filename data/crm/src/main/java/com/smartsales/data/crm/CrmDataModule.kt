package com.smartsales.data.crm

import com.smartsales.prism.domain.memory.EntityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CrmDataModule {
    @Binds
    abstract fun bindEntityRepository(impl: RoomEntityRepository): EntityRepository

    @Binds
    abstract fun bindAliasCache(impl: RealAliasCache): com.smartsales.prism.domain.memory.AliasCache
}
