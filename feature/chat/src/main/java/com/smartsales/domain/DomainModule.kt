// File: feature/chat/src/main/java/com/smartsales/domain/DomainModule.kt
// Module: :feature:chat
// Summary: Hilt bindings for domain layer interfaces
// Author: created on 2026-01-07

package com.smartsales.domain

import com.smartsales.domain.debug.DebugCoordinator
import com.smartsales.domain.debug.DebugCoordinatorImpl
import com.smartsales.domain.export.ExportCoordinator
import com.smartsales.domain.export.ExportCoordinatorImpl
import com.smartsales.domain.sessions.SessionsManager
import com.smartsales.domain.sessions.SessionsManagerImpl
import com.smartsales.domain.transcription.Disector
import com.smartsales.domain.transcription.DisectorImpl
import com.smartsales.domain.transcription.Sanitizer
import com.smartsales.domain.transcription.SanitizerImpl
import com.smartsales.domain.transcription.TranscriptionCoordinator
import com.smartsales.domain.transcription.TranscriptionCoordinatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DomainModule: binds domain layer interfaces to implementations.
 * 
 * Purpose: Decouple javax.inject from domain layer interfaces.
 * Pattern: Interface in domain/, Impl with @Inject/@Singleton in impl/
 */
@Module
@InstallIn(SingletonComponent::class)
interface DomainModule {

    @Binds
    @Singleton
    fun bindDisector(impl: DisectorImpl): Disector

    @Binds
    @Singleton
    fun bindSanitizer(impl: SanitizerImpl): Sanitizer

    @Binds
    @Singleton
    fun bindExportCoordinator(impl: ExportCoordinatorImpl): ExportCoordinator

    @Binds
    @Singleton
    fun bindDebugCoordinator(impl: DebugCoordinatorImpl): DebugCoordinator

    @Binds
    @Singleton
    fun bindTranscriptionCoordinator(impl: TranscriptionCoordinatorImpl): TranscriptionCoordinator

    @Binds
    @Singleton
    fun bindSessionsManager(impl: SessionsManagerImpl): SessionsManager

    @Binds
    @Singleton
    fun bindChatCoordinator(impl: com.smartsales.domain.chat.ChatCoordinatorImpl): com.smartsales.domain.chat.ChatCoordinator
}

