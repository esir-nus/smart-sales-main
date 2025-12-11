package com.smartsales.feature.usercenter.data

// 文件：feature/usercenter/src/test/java/com/smartsales/feature/usercenter/data/PersistentUserProfileRepositoryTest.kt
// 模块：:feature:usercenter
// 说明：验证用户画像字段在仓库中的读写回路
// 作者：创建于 2025-12-10

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
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
        context = FakeContext()
        context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
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

private class FakeContext : ContextWrapper(null) {
    private val prefs = mutableMapOf<String, SharedPreferences>()

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        val key = name ?: "default"
        return prefs.getOrPut(key) { InMemorySharedPreferences() }
    }
}

private class InMemorySharedPreferences : SharedPreferences {

    private val data = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()

    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(
        key: String?,
        defValues: MutableSet<String>?
    ): MutableSet<String>? =
        data[key] as? MutableSet<String> ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        data[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        data[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        data[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean =
        data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        // not needed for this test
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) {
        // not needed for this test
    }

    private inner class EditorImpl : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            pending[key!!] = value
            return this
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?
        ): SharedPreferences.Editor {
            pending[key!!] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            pending[key!!] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            pending[key!!] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            pending[key!!] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            pending[key!!] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            pending[key!!] = null
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) {
                data.clear()
            }
            for ((k, v) in pending) {
                if (v == null) data.remove(k) else data[k] = v
            }
        }
    }
}
