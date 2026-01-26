package com.smartsales.data.prismlib.di

import android.content.Context
import androidx.room.Room
import com.smartsales.data.prismlib.db.PrismDatabase
import com.smartsales.data.prismlib.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrismLibModule {

    @Provides
    @Singleton
    fun providePrismDatabase(
        @ApplicationContext context: Context
    ): PrismDatabase {
        return Room.databaseBuilder(
            context,
            PrismDatabase::class.java,
            "prism_db"
        )
        .fallbackToDestructiveMigration() // For Phase 2 dev velocity
        .build()
    }

    @Provides
    fun provideMemoryEntryDao(db: PrismDatabase): MemoryEntryDao = db.memoryEntryDao()

    @Provides
    fun provideRelevancyDao(db: PrismDatabase): RelevancyDao = db.relevancyDao()

    @Provides
    fun provideSessionsDao(db: PrismDatabase): SessionsDao = db.sessionsDao()

    @Provides
    fun provideScheduledTaskDao(db: PrismDatabase): ScheduledTaskDao = db.scheduledTaskDao()

    @Provides
    fun provideUserProfileDao(db: PrismDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideUserHabitDao(db: PrismDatabase): UserHabitDao = db.userHabitDao()

    @Provides
    fun provideInspirationDao(db: PrismDatabase): InspirationDao = db.inspirationDao()
}


