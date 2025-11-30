package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/test/java/com/smartsales/feature/usercenter/UserCenterViewModelTest.kt
// 模块：:feature:usercenter
// 说明：验证用户中心 ViewModel 的基本状态更新与事件
// 作者：创建于 2025-11-30

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.feature.usercenter.data.UserProfileRepository
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserCenterViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeUserProfileRepository
    private lateinit var viewModel: UserCenterViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeUserProfileRepository()
        viewModel = UserCenterViewModel(
            repository = repository,
            dispatchers = FakeDispatcherProvider(dispatcher)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadUser populates initial state`() = runTest(dispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Tester", state.displayName)
        assertEquals("tester@example.com", state.email)
        assertFalse(state.isGuest)
        assertTrue(state.canLogout)
    }

    @Test
    fun `logout emits event`() = runTest(dispatcher) {
        advanceUntilIdle()
        val event = backgroundScope.async { viewModel.events.first() }
        viewModel.onLogoutClick()
        advanceUntilIdle()

        assertTrue(event.await() is UserCenterEvent.Logout)
        assertFalse(viewModel.uiState.value.canLogout)
        assertTrue(viewModel.uiState.value.isGuest)
    }

    private class FakeUserProfileRepository : UserProfileRepository {
        private val store = AtomicReference(
            UserProfile(
                displayName = "Tester",
                email = "tester@example.com",
                isGuest = false
            )
        )

        override suspend fun load(): UserProfile = store.get()

        override suspend fun save(profile: UserProfile) {
            store.set(profile)
        }

        override suspend fun clear() {
            store.set(UserProfile(displayName = "", email = "", isGuest = true))
        }
    }
}
