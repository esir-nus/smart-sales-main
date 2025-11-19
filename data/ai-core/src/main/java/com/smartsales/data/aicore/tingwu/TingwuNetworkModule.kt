package com.smartsales.data.aicore.tingwu

import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.data.aicore.TingwuCredentialsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Optional
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.EventListener
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuNetworkModule.kt
// 模块：:data:ai-core
// 说明：构建 Tingwu Retrofit/OkHttp 客户端
// 作者：创建于 2025-11-16
@Module
@InstallIn(SingletonComponent::class)
object TingwuNetworkModule {
    @Provides
    @Singleton
    fun provideTingwuOkHttpClient(
        authInterceptor: TingwuAuthInterceptor,
        optionalConfig: Optional<AiCoreConfig>,
        dns: Dns,
        eventListenerFactory: EventListener.Factory
    ): OkHttpClient {
        val config = optionalConfig.orElse(AiCoreConfig())
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.tingwuConnectTimeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(config.tingwuReadTimeoutMillis, TimeUnit.MILLISECONDS)
            .writeTimeout(config.tingwuWriteTimeoutMillis, TimeUnit.MILLISECONDS)
            .dns(dns)
            .retryOnConnectionFailure(true)
            .connectionPool(
                ConnectionPool(
                    config.tingwuConnectionPoolMaxIdle,
                    config.tingwuConnectionPoolKeepAliveMinutes,
                    TimeUnit.MINUTES
                )
            )
            .addInterceptor(authInterceptor)
        if (config.enableTingwuNetworkEventLog) {
            builder.eventListenerFactory(eventListenerFactory)
        }
        if (config.enableTingwuHttpLogging) {
            builder.addNetworkInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideTingwuRetrofit(
        okHttpClient: OkHttpClient,
        credentialsProvider: TingwuCredentialsProvider,
        optionalConfig: Optional<AiCoreConfig>
    ): Retrofit {
        val config = optionalConfig.orElse(AiCoreConfig())
        val baseUrl = config.tingwuBaseUrlOverride?.takeIf { it.isNotBlank() }
            ?: credentialsProvider.obtain().baseUrl
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTingwuApi(retrofit: Retrofit): TingwuApi =
        retrofit.create(TingwuApi::class.java)
}
