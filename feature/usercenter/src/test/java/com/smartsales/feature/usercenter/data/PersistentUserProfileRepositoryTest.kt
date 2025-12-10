package com.smartsales.feature.usercenter.data

// 文件：feature/usercenter/src/test/java/com/smartsales/feature/usercenter/data/PersistentUserProfileRepositoryTest.kt
// 模块：:feature:usercenter
// 说明：验证用户画像字段在仓库中的读写回路
// 作者：创建于 2025-12-10

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.smartsales.feature.usercenter.SalesPersona
import com.smartsales.feature.usercenter.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersistentUserProfileRepositoryTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun `persona fields round trip through repository`() = runTest {
        val repo = PersistentUserProfileRepository(context)
        val persona = SalesPersona(
            role = "客户经理",
            industry = "汽车",
            mainChannel = "微信+电话",
            experienceLevel = "1-5 年",
            stylePreference = "偏口语"
        )
        val profile = UserProfile(
            displayName = "张三",
            email = "test@example.com",
            isGuest = false,
            role = persona.role,
            industry = persona.industry,
            salesPersona = persona
        )

        repo.save(profile)
        val loaded = repo.load()

        val loadedPersona = loaded.salesPersona!!
        assertEquals("客户经理", loadedPersona.role)
        assertEquals("汽车", loadedPersona.industry)
        assertEquals("微信+电话", loadedPersona.mainChannel)
        assertEquals("1-5 年", loadedPersona.experienceLevel)
        assertEquals("偏口语", loadedPersona.stylePreference)
    }
}
