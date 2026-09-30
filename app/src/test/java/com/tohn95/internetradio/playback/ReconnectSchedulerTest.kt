package com.tohn95.internetradio.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReconnectSchedulerTest {
    @Test fun `exponential backoff 1-2-4 seconds`() = runTest {
        var retries = 0
        val s = ReconnectScheduler(backgroundScope) { retries++ }
        s.onError(); runCurrent()
        advanceTimeBy(999); runCurrent(); assertEquals(0, retries)
        advanceTimeBy(2); runCurrent(); assertEquals(1, retries)              // ~1 c
        s.onError(); advanceTimeBy(2001); runCurrent(); assertEquals(2, retries)  // ~2 c
        s.onError(); advanceTimeBy(4001); runCurrent(); assertEquals(3, retries)  // ~4 c
    }

    @Test fun `stops after 10 attempts`() = runTest {
        var retries = 0
        val s = ReconnectScheduler(backgroundScope) { retries++ }
        repeat(12) { s.onError(); advanceTimeBy(31_000); runCurrent() }
        assertEquals(10, retries)
    }

    @Test fun `network available triggers immediate retry and resets counter`() = runTest {
        var retries = 0
        val s = ReconnectScheduler(backgroundScope) { retries++ }
        repeat(5) { s.onError(); advanceTimeBy(31_000); runCurrent() }
        assertEquals(5, retries)
        s.onNetworkAvailable(); runCurrent()
        assertEquals(6, retries)
        assertEquals(0, s.attempts.value)
    }

    @Test fun `reset cancels pending retry`() = runTest {
        var retries = 0
        val s = ReconnectScheduler(backgroundScope) { retries++ }
        s.onError(); s.reset()
        advanceTimeBy(60_000); runCurrent()
        assertEquals(0, retries)
    }
}
