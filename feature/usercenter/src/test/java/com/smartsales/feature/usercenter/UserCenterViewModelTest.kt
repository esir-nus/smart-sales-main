package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/test/java/com/smartsales/feature/usercenter/UserCenterViewModelTest.kt
// 模块：:feature:usercenter
// 说明：验证用户中心 ViewModel 的基本状态更新与事件
// 作者：创建于 2025-11-21

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.usercenter.data.UserProfileRepository
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
        assertEquals("Tester", state.userName)
        assertEquals("tester@example.com", state.email)
        assertEquals(10, state.tokensRemaining)
        assertEquals(true, state.featureFlags["开关A"])
    }

    @Test
    fun `editing fields updates ui state`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.onUserNameChanged("NewName")
        viewModel.onEmailChanged("a@b.com")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("NewName", state.userName)
        assertEquals("a@b.com", state.email)
    }

    @Test
    fun `toggle feature flag flips value`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.onToggleFeatureFlag("开关A")
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.featureFlags["开关A"])
    }

    @Test
    fun `save shows saving then clears`() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.onSaveClicked()
        advanceTimeBy(100)
        assertTrue(viewModel.uiState.value.isSaving)
        advanceTimeBy(600)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `logout emits event`() = runTest(dispatcher) {
        advanceUntilIdle()
        val event = backgroundScope.async { viewModel.events.first() }
        viewModel.onLogoutClicked()
        advanceUntilIdle()

        assertTrue(event.await() is UserCenterEvent.Logout)
    }

    private class FakeUserProfileRepository : UserProfileRepository {
        private val store = AtomicReference(
            UserProfile(
                userName = "Tester",
                email = "tester@example.com",
                tokensRemaining = 10,
                featureFlags = linkedMapOf("开关A" to true, "开关B" to false)
            )
        )

        override suspend fun load(): Result<UserProfile> = Result.Success(store.get())

        override suspend fun save(profile: UserProfile): Result<Unit> {
            store.set(profile)
            return Result.Success(Unit)
        }
    }
}
