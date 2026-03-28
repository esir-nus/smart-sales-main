package com.smartsales.prism.di

import com.smartsales.prism.ui.onboarding.OnboardingAudioCapture
import com.smartsales.prism.ui.onboarding.OnboardingInteractionService
import com.smartsales.prism.ui.onboarding.RealOnboardingAudioCapture
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
    abstract fun bindOnboardingAudioCapture(
        impl: RealOnboardingAudioCapture
    ): OnboardingAudioCapture

    @Binds
    @Singleton
    abstract fun bindOnboardingInteractionService(
        impl: RealOnboardingInteractionService
    ): OnboardingInteractionService
}
