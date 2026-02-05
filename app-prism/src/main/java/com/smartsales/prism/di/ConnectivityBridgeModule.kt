package com.smartsales.prism.di

import com.smartsales.prism.data.fakes.FakeConnectivityBridge
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Connectivity Bridge 模块 — 提供 Badge 连接服务
 * 
 * Wave 1: 绑定 FakeConnectivityBridge
 * Wave 2: 切换到 RealConnectivityBridge
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityBridgeModule {
    
    @Binds
    @Singleton
    abstract fun bindConnectivityBridge(fake: FakeConnectivityBridge): ConnectivityBridge
}
