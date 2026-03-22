package com.smartsales.prism.ui.onboarding

import com.smartsales.prism.data.fakes.FakeUserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeUserProfileRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeUserProfileRepository()
        viewModel = OnboardingViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveProfile updates profile fields`() = runTest {
        viewModel.saveProfile(displayName = "Alice", role = "seller")
        advanceUntilIdle()

        val profile = repository.getProfile()
        assertEquals("Alice", profile.displayName)
        assertEquals("seller", profile.role)
    }
}
