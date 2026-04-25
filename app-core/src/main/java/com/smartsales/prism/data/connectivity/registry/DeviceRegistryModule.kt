package com.smartsales.prism.data.connectivity.registry

import android.content.Context
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.prism.data.connectivity.legacy.ConnectivityScope
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.SessionStore
import com.smartsales.prism.data.connectivity.legacy.scan.BleScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DeviceRegistryModule {

    @Provides
    @Singleton
    fun provideDeviceRegistry(
        @ApplicationContext context: Context
    ): DeviceRegistry = SharedPrefsDeviceRegistry(context)

    @Provides
    @Singleton
    fun provideDeviceRegistryManager(
        registry: DeviceRegistry,
        sessionStore: SessionStore,
        deviceConnectionManager: DeviceConnectionManager,
        dispatchers: DispatcherProvider,
        bleScanner: BleScanner,
        @ConnectivityScope scope: CoroutineScope
    ): DeviceRegistryManager = RealDeviceRegistryManager(
        registry = registry,
        sessionStore = sessionStore,
        deviceConnectionManager = deviceConnectionManager,
        dispatchers = dispatchers,
        scope = scope,
        bleScanner = bleScanner
    )
}
