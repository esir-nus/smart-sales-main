package com.smartsales.feature.usercenter.data

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/data/UserProfileRepository.kt
// 模块：:feature:usercenter
// 说明：用户信息的存储仓库，当前使用内存 stub
// 作者：创建于 2025-11-30

import com.smartsales.feature.usercenter.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface UserProfileRepository {
    suspend fun load(): UserProfile
    suspend fun save(profile: UserProfile)
    suspend fun clear()
    val profileFlow: Flow<UserProfile>
}

@Singleton
class PersistentUserProfileRepository @Inject constructor(
    @ApplicationContext context: Context
) : UserProfileRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val state = MutableStateFlow(readProfile())

    override suspend fun load(): UserProfile = mutex.withLock {
        val profile = readProfile()
        state.value = profile
        profile
    }

    override suspend fun save(profile: UserProfile) = mutex.withLock {
        val sanitized = profile.sanitized()
        writeProfile(sanitized)
        state.value = sanitized
    }

    override suspend fun clear() = mutex.withLock {
        // 访客状态：置空用户名与邮箱，并标记 isGuest。
        val guest = guestProfile()
        writeProfile(guest)
        state.value = guest
    }

    override val profileFlow: Flow<UserProfile> = state

    private fun readProfile(): UserProfile {
        val isGuest = prefs.getBoolean(KEY_GUEST, false)
        val displayName = prefs.getString(KEY_NAME, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val role = prefs.getString(KEY_ROLE, null)
        val industry = prefs.getString(KEY_INDUSTRY, null)
        val organization = prefs.getString(KEY_ORGANIZATION, null)
        val phone = prefs.getString(KEY_PHONE, null)
        val base = UserProfile(
            displayName = displayName,
            email = email,
            isGuest = isGuest,
            organization = organization,
            role = role,
            industry = industry,
            phone = phone
        )
        return if (isGuest) guestProfile() else base.sanitized()
    }

    private fun writeProfile(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_NAME, profile.displayName)
            .putString(KEY_EMAIL, profile.email)
            .putBoolean(KEY_GUEST, profile.isGuest)
            .putString(KEY_ORGANIZATION, profile.organization)
            .putString(KEY_ROLE, profile.role)
            .putString(KEY_INDUSTRY, profile.industry)
            .putString(KEY_PHONE, profile.phone)
            .apply()
    }

    private fun UserProfile.sanitized(): UserProfile {
        val trimmedName = displayName.trim()
        val trimmedRole = role?.trim().orEmpty().ifBlank { null }?.let { it }
        val trimmedIndustry = industry?.trim().orEmpty().ifBlank { null }?.let { it }
        val trimmedOrg = organization?.trim().orEmpty().ifBlank { null }?.let { it }
        val trimmedPhone = phone?.trim().orEmpty().ifBlank { null }?.let { it }
        val trimmedEmail = email.trim()
        val resolvedName = if (trimmedName.isNotEmpty()) trimmedName else DEFAULT_NAME
        return copy(
            displayName = resolvedName,
            email = trimmedEmail,
            isGuest = false,
            organization = trimmedOrg,
            role = trimmedRole,
            industry = trimmedIndustry,
            phone = trimmedPhone
        )
    }

    private fun guestProfile(): UserProfile = UserProfile(
        displayName = "",
        email = "",
        isGuest = true,
        organization = null,
        role = null,
        industry = null,
        phone = null
    )

    companion object {
        private const val PREFS_NAME = "user_profile_prefs"
        private const val KEY_NAME = "display_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_GUEST = "guest"
        private const val KEY_ORGANIZATION = "organization"
        private const val KEY_ROLE = "role"
        private const val KEY_INDUSTRY = "industry"
        private const val KEY_PHONE = "phone"
        private const val DEFAULT_NAME = "SmartSales 用户"
    }
}
