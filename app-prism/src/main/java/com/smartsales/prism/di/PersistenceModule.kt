package com.smartsales.prism.di

import android.content.Context
import androidx.room.Room
import com.smartsales.prism.data.persistence.PrismDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room 持久化 DI 模块
 * 
 * 仅提供 PrismDatabase 单例
 * DAOs 由各 Repository 内部通过 db.xxxDao() 获取
 */
@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {
    
    @Provides
    @Singleton
    fun providePrismDatabase(@ApplicationContext context: Context): PrismDatabase {
        return Room.databaseBuilder(
            context,
            PrismDatabase::class.java,
            "prism_database"
        )
            .fallbackToDestructiveMigrationFrom(1)  // v1→v2: CalendarProvider → Room
            .build()
    }
    
    @Provides
    @Singleton
    fun provideScheduledTaskDao(database: PrismDatabase) = database.scheduledTaskDao()
}
