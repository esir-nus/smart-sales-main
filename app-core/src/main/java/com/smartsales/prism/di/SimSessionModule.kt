package com.smartsales.prism.di

import android.content.Context
import com.smartsales.prism.data.session.SimSessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SimSessionModule {

    @Provides
    @Singleton
    fun provideSimSessionRepository(
        @ApplicationContext context: Context
    ): SimSessionRepository {
        return SimSessionRepository(context.filesDir)
    }
}
