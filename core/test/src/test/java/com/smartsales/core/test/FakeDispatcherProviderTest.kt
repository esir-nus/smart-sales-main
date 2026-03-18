package com.smartsales.core.test

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeDispatcherProviderTest {

    @Test
    fun `uses one test dispatcher across lanes and stays scheduler controlled`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val provider = FakeDispatcherProvider(dispatcher)
        var ran = false

        launch(provider.io) {
            ran = true
        }

        assertSame(dispatcher, provider.io)
        assertSame(dispatcher, provider.main)
        assertSame(dispatcher, provider.default)
        assertFalse(ran)

        testScheduler.runCurrent()

        assertTrue(ran)
    }
}
