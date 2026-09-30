package com.tohn95.internetradio.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MetadataTrackerTest {
    @Test fun `detecting becomes notAvailable after timeout`() = runTest {
        val t = MetadataTracker(backgroundScope, timeoutMs = 5000)
        t.onStreamStarted(); runCurrent()
        assertEquals(MetadataState.Detecting, t.state.value)
        advanceTimeBy(5001); runCurrent()
        assertEquals(MetadataState.NotAvailable, t.state.value)
    }

    @Test fun `icy title arrives before timeout and survives it`() = runTest {
        val t = MetadataTracker(backgroundScope, timeoutMs = 5000)
        t.onStreamStarted(); runCurrent()
        advanceTimeBy(2000)
        t.onIcyTitle("Artist - Song"); runCurrent()
        assertEquals(MetadataState.Available("Artist - Song"), t.state.value)
        advanceTimeBy(10_000); runCurrent()
        assertEquals(MetadataState.Available("Artist - Song"), t.state.value)
    }

    @Test fun `buffering keeps known title`() = runTest {
        val t = MetadataTracker(backgroundScope, timeoutMs = 5000)
        t.onStreamStarted(); runCurrent()
        t.onIcyTitle("A - B"); runCurrent()
        t.onBuffering(); runCurrent()
        advanceTimeBy(10_000); runCurrent()
        assertEquals(MetadataState.Available("A - B"), t.state.value)
    }

    @Test fun `buffering before any title restarts detection`() = runTest {
        val t = MetadataTracker(backgroundScope, timeoutMs = 5000)
        t.onStreamStarted(); runCurrent()
        advanceTimeBy(4000)
        t.onBuffering(); runCurrent()
        advanceTimeBy(4000); runCurrent()
        assertEquals(MetadataState.Detecting, t.state.value)
    }

    @Test fun `new station resets title`() = runTest {
        val t = MetadataTracker(backgroundScope, timeoutMs = 5000)
        t.onStreamStarted(); runCurrent()
        t.onIcyTitle("A - B"); runCurrent()
        t.onStreamStarted(); runCurrent()
        assertEquals(MetadataState.Detecting, t.state.value)
    }

    @Test fun `blank title ignored`() = runTest {
        val t = MetadataTracker(backgroundScope, timeoutMs = 5000)
        t.onStreamStarted(); runCurrent()
        t.onIcyTitle("  "); runCurrent()
        assertEquals(MetadataState.Detecting, t.state.value)
    }
}
