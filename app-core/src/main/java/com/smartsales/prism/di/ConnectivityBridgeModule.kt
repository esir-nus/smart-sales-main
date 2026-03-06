package com.smartsales.prism.di

import com.smartsales.prism.data.connectivity.RealConnectivityBridge
import com.smartsales.prism.data.connectivity.RealConnectivityService
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.ConnectivityService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Connectivity Bridge 模块 — 提供 Badge 连接服务
 * 
 * Wave 1: Fake (testing)
 * Wave 2: Real Bridge (wrapping legacy DeviceConnectionManager)
 * Wave 2.5: Real Service (UI wiring)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityBridgeModule {
    
    @Binds
    @Singleton
    abstract fun bindConnectivityBridge(impl: RealConnectivityBridge): ConnectivityBridge
    
    @Binds
    @Singleton
    abstract fun bindConnectivityService(impl: RealConnectivityService): ConnectivityService
}

