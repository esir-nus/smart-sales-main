package com.smartsales.prism.di

import com.smartsales.prism.data.scheduler.RealConflictResolver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt Entry Point for Composables
 * 
 * Allows ConflictCard to access RealConflictResolver without ViewModelScope
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HiltComponentProvider {
    fun conflictResolver(): RealConflictResolver
}
