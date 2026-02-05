package com.smartsales.prism.di

import com.smartsales.prism.data.connectivity.RealConnectivityBridge
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Connectivity Bridge 模块 — 提供 Badge 连接服务
 * 
 * Wave 1: Fake (testing)
 * Wave 2: Real (wrapping legacy DeviceConnectionManager)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityBridgeModule {
    
    @Binds
    @Singleton
    abstract fun bindConnectivityBridge(impl: RealConnectivityBridge): ConnectivityBridge
}
