package com.smartsales.prism.di

import com.smartsales.prism.data.audio.DeviceSpeechRecognizer
import com.smartsales.prism.data.audio.RealDeviceSpeechRecognizer
import com.smartsales.prism.ui.onboarding.OnboardingInteractionService
import com.smartsales.prism.ui.onboarding.RealOnboardingInteractionService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingInteractionModule {

    @Binds
    @Singleton
    abstract fun bindDeviceSpeechRecognizer(
        impl: RealDeviceSpeechRecognizer
    ): DeviceSpeechRecognizer

    @Binds
    @Singleton
    abstract fun bindOnboardingInteractionService(
        impl: RealOnboardingInteractionService
    ): OnboardingInteractionService
}
