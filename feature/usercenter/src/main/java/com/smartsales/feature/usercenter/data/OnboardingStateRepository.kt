package com.smartsales.feature.usercenter.data

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/data/OnboardingStateRepository.kt
// 模块：:feature:usercenter
// 说明：首次启动引导状态的持久化仓库
// 作者：创建于 2025-12-10

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface OnboardingStateRepository {
    val completedFlow: Flow<Boolean>
    suspend fun markCompleted(completed: Boolean)
}

@Singleton
class PersistentOnboardingStateRepository @Inject constructor(
    @ApplicationContext context: Context
) : OnboardingStateRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val state = MutableStateFlow(readFlag())

    override val completedFlow: Flow<Boolean> = state.asStateFlow()

    override suspend fun markCompleted(completed: Boolean) {
        mutex.withLock {
            writeFlag(completed)
            state.value = completed
        }
    }

    private fun readFlag(): Boolean {
        testOverrideCompleted?.let { return it }
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    private fun writeFlag(value: Boolean) {
        prefs.edit().putBoolean(KEY_COMPLETED, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "onboarding_state_prefs"
        private const val KEY_COMPLETED = "completed"
        /**
         * 测试覆盖：用于自动化测试跳过或强制引导。
         * 生产环境保持 null。
         */
        @JvmStatic
        var testOverrideCompleted: Boolean? = null
    }
}
