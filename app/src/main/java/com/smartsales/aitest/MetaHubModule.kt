package com.smartsales.aitest

// 文件：app/src/main/java/com/smartsales/aitest/MetaHubModule.kt
// 模块：:app
// 说明：提供元数据中心的Hilt绑定，默认启用文件持久化
// 作者：创建于 2025-12-04

import com.smartsales.core.metahub.MetaHub
import com.smartsales.data.aicore.metahub.FileBackedMetaHub
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.google.gson.Gson
import android.content.Context
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MetaHubModule {
    @Provides
    @Singleton
    fun provideMetaHub(
        @ApplicationContext context: Context,
        gson: Gson
    ): MetaHub = FileBackedMetaHub(
        rootDir = File(context.filesDir, "metahub"),
        gson = gson
    )
}
