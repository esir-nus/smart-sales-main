package com.smartsales.prism.ui.settings

import android.content.SharedPreferences
import com.smartsales.prism.data.connectivity.legacy.FakeDeviceConnectionManager
import com.smartsales.prism.domain.config.SubscriptionTier
import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.domain.notification.NotificationAction
import com.smartsales.prism.domain.notification.NotificationPriority
import com.smartsales.prism.domain.notification.NotificationService
import com.smartsales.prism.domain.notification.PrismNotificationChannel
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.ui.theme.PrismThemeMode
import com.smartsales.prism.ui.theme.ThemePreferenceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserCenterViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes current theme mode from preference store`() {
        val repository = FakeUserProfileRepository()
        val notificationService = FakeNotificationService()
        val themeStore = ThemePreferenceStore(InMemorySharedPreferences().apply {
            edit().putString("theme_mode", PrismThemeMode.DARK.name).apply()
        })
        val voiceVolumeStore = VoiceVolumePreferenceStore(InMemorySharedPreferences())
        val connectionManager = FakeDeviceConnectionManager()

        val viewModel = UserCenterViewModel(
            repository,
            notificationService,
            themeStore,
            voiceVolumeStore,
            connectionManager
        )

        assertEquals(PrismThemeMode.DARK, viewModel.themeMode.value)
    }

    @Test
    fun `setThemeMode updates preference flow`() {
        val repository = FakeUserProfileRepository()
        val notificationService = FakeNotificationService()
        val themeStore = ThemePreferenceStore(InMemorySharedPreferences())
        val voiceVolumeStore = VoiceVolumePreferenceStore(InMemorySharedPreferences())
        val connectionManager = FakeDeviceConnectionManager()
        val viewModel = UserCenterViewModel(
            repository,
            notificationService,
            themeStore,
            voiceVolumeStore,
            connectionManager
        )

        viewModel.setThemeMode(PrismThemeMode.LIGHT)

        assertEquals(PrismThemeMode.LIGHT, viewModel.themeMode.value)
    }

    @Test
    fun `updateProfile persists edited profile`() = runTest {
        val repository = FakeUserProfileRepository()
        val notificationService = FakeNotificationService()
        val themeStore = ThemePreferenceStore(InMemorySharedPreferences())
        val voiceVolumeStore = VoiceVolumePreferenceStore(InMemorySharedPreferences())
        val connectionManager = FakeDeviceConnectionManager()
        val viewModel = UserCenterViewModel(
            repository,
            notificationService,
            themeStore,
            voiceVolumeStore,
            connectionManager
        )
        val collectionJob = backgroundScope.launch { viewModel.profile.collect { } }
        advanceUntilIdle()

        viewModel.updateProfile(
            displayName = "Frank",
            role = "manager",
            industry = "Technology",
            experienceYears = "10 years",
            communicationPlatform = "WeChat"
        )
        advanceUntilIdle()

        val profile = repository.profile.value
        assertEquals("Frank", profile.displayName)
        assertEquals("manager", profile.role)
        assertEquals("Technology", profile.industry)
        assertEquals("10 years", profile.experienceYears)
        assertEquals("WeChat", profile.communicationPlatform)
        assertTrue(profile.updatedAt > 0L)

        collectionJob.cancel()
    }

    @Test
    fun `voice volume commit persists desired volume and marks applied on successful send`() = runTest {
        val repository = FakeUserProfileRepository()
        val notificationService = FakeNotificationService()
        val themeStore = ThemePreferenceStore(InMemorySharedPreferences())
        val prefs = InMemorySharedPreferences()
        val voiceVolumeStore = VoiceVolumePreferenceStore(prefs)
        val connectionManager = FakeDeviceConnectionManager()
        val viewModel = UserCenterViewModel(
            repository,
            notificationService,
            themeStore,
            voiceVolumeStore,
            connectionManager
        )

        viewModel.onVoiceVolumeDrag(72)
        viewModel.onVoiceVolumeCommitted()
        advanceUntilIdle()

        assertEquals(72, voiceVolumeStore.desiredVolume.value)
        assertEquals(72, voiceVolumeStore.lastAppliedVolume.value)
        assertEquals(listOf(72), connectionManager.voiceVolumeCalls)
    }

    @Test
    fun `voice volume commit retries same value after disconnected no-op send`() = runTest {
        val repository = FakeUserProfileRepository()
        val notificationService = FakeNotificationService()
        val themeStore = ThemePreferenceStore(InMemorySharedPreferences())
        val prefs = InMemorySharedPreferences()
        val voiceVolumeStore = VoiceVolumePreferenceStore(prefs)
        val connectionManager = FakeDeviceConnectionManager().apply {
            setVoiceVolumeShouldSucceed = false
        }
        val viewModel = UserCenterViewModel(
            repository,
            notificationService,
            themeStore,
            voiceVolumeStore,
            connectionManager
        )

        viewModel.onVoiceVolumeDrag(61)
        viewModel.onVoiceVolumeCommitted()
        advanceUntilIdle()

        assertEquals(61, voiceVolumeStore.desiredVolume.value)
        assertEquals(null, voiceVolumeStore.lastAppliedVolume.value)
        assertEquals(listOf(61), connectionManager.voiceVolumeCalls)

        connectionManager.setVoiceVolumeShouldSucceed = true
        viewModel.onVoiceVolumeCommitted()
        advanceUntilIdle()

        assertEquals(61, voiceVolumeStore.lastAppliedVolume.value)
        assertEquals(listOf(61, 61), connectionManager.voiceVolumeCalls)
    }

    private class FakeUserProfileRepository : UserProfileRepository {
        private val state = MutableStateFlow(
            UserProfile(
                id = 1,
                displayName = "Initial",
                role = "sales",
                industry = "Retail",
                experienceLevel = "expert",
                updatedAt = 1L,
                subscriptionTier = SubscriptionTier.PRO
            )
        )

        override val profile: StateFlow<UserProfile> = state

        override suspend fun getProfile(): UserProfile = state.value

        override suspend fun updateProfile(profile: UserProfile) {
            state.value = profile
        }
    }

    private class FakeNotificationService : NotificationService {
        override fun show(
            id: String,
            title: String,
            body: String,
            channel: PrismNotificationChannel,
            priority: NotificationPriority,
            action: NotificationAction
        ) = Unit

        override fun cancel(id: String) = Unit

        override fun hasPermission(): Boolean = true

        override fun stopVibration() = Unit

        override fun startPersistentVibration() = Unit
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? {
            return values[key] as? String ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return (values[key] as? Set<String>)?.toMutableSet() ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = Editor(values)

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        private class Editor(
            private val values: MutableMap<String, Any?>
        ) : SharedPreferences.Editor {
            private val pending = linkedMapOf<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = value
            }

            override fun putStringSet(
                key: String?,
                values: MutableSet<String>?
            ): SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = values?.toSet()
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = value
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = value
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = value
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = value
            }

            override fun remove(key: String?): SharedPreferences.Editor = apply {
                pending[key.orEmpty()] = null
            }

            override fun clear(): SharedPreferences.Editor = apply {
                clearRequested = true
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearRequested) {
                    values.clear()
                }
                pending.forEach { (key, value) ->
                    if (value == null) {
                        values.remove(key)
                    } else {
                        values[key] = value
                    }
                }
                pending.clear()
                clearRequested = false
            }
        }
    }
}
