package com.smartsales.prism.di

import com.smartsales.prism.data.audio.DeviceSpeechRecognizer
import com.smartsales.prism.data.audio.RealDeviceSpeechRecognizer
import com.smartsales.prism.data.onboarding.BaseRuntimeOnboardingHandoffGate
import com.smartsales.prism.data.onboarding.RuntimeOnboardingHandoffGate
import com.smartsales.prism.ui.onboarding.OnboardingInteractionService
import com.smartsales.prism.ui.onboarding.OnboardingQuickStartCalendarExporter
import com.smartsales.prism.ui.onboarding.OnboardingQuickStartReminderGuideCoordinator
import com.smartsales.prism.ui.onboarding.OnboardingQuickStartSandboxResolver
import com.smartsales.prism.ui.onboarding.OnboardingQuickStartService
import com.smartsales.prism.ui.onboarding.OnboardingSchedulerQuickStartCommitter
import com.smartsales.prism.ui.onboarding.RealOnboardingInteractionService
import com.smartsales.prism.ui.onboarding.RealOnboardingQuickStartCalendarExporter
import com.smartsales.prism.ui.onboarding.RealOnboardingQuickStartReminderGuideCoordinator
import com.smartsales.prism.ui.onboarding.RealOnboardingQuickStartSandboxResolver
import com.smartsales.prism.ui.onboarding.RealOnboardingQuickStartService
import com.smartsales.prism.ui.onboarding.RealOnboardingSchedulerQuickStartCommitter
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

    @Binds
    @Singleton
    abstract fun bindOnboardingQuickStartCommitter(
        impl: RealOnboardingSchedulerQuickStartCommitter
    ): OnboardingSchedulerQuickStartCommitter

    @Binds
    @Singleton
    abstract fun bindOnboardingQuickStartService(
        impl: RealOnboardingQuickStartService
    ): OnboardingQuickStartService

    @Binds
    @Singleton
    abstract fun bindOnboardingQuickStartSandboxResolver(
        impl: RealOnboardingQuickStartSandboxResolver
    ): OnboardingQuickStartSandboxResolver

    @Binds
    @Singleton
    abstract fun bindOnboardingQuickStartReminderGuideCoordinator(
        impl: RealOnboardingQuickStartReminderGuideCoordinator
    ): OnboardingQuickStartReminderGuideCoordinator

    @Binds
    @Singleton
    abstract fun bindOnboardingQuickStartCalendarExporter(
        impl: RealOnboardingQuickStartCalendarExporter
    ): OnboardingQuickStartCalendarExporter

    @Binds
    @Singleton
    abstract fun bindRuntimeOnboardingHandoffGate(
        impl: BaseRuntimeOnboardingHandoffGate
    ): RuntimeOnboardingHandoffGate
}
