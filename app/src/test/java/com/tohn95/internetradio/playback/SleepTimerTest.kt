package com.tohn95.internetradio.playback

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {
    @Test fun `counts down and expires with volume restore`() = runTest {
        val volumes = mutableListOf<Float>()
        var expired = 0
        val t = SleepTimer(backgroundScope, setVolume = { volumes.add(it) }, onExpired = { expired++ })
        t.start(60_000)
        runCurrent()
        assertEquals(60_000L, t.remainingMs.value)
        advanceTimeBy(30_001); runCurrent()
        assertEquals(30_000L, t.remainingMs.value)   // ~половина
        advanceTimeBy(30_000); runCurrent()
        assertEquals(1, expired)
        assertNull(t.remainingMs.value)
        assertEquals(1f, volumes.last())              // громкость восстановлена
    }

    @Test fun `fades linearly during last 30 seconds`() = runTest {
        val volumes = mutableListOf<Float>()
        val t = SleepTimer(backgroundScope, setVolume = { volumes.add(it) }, onExpired = {})
        t.start(40_000)
        runCurrent()
        advanceTimeBy(10_000); runCurrent()          // осталось 30с — fade ещё не начался
        assertEquals(listOf(1f), volumes)            // только сброс из start() (A1), fade ещё не начался
        advanceTimeBy(15_000); runCurrent()          // осталось 15с — середина fade
        assertEquals(0.5f, volumes.last(), 0.02f)
        advanceTimeBy(14_000); runCurrent()          // осталось 1с
        assertEquals(1f / 30f, volumes.last(), 0.02f)
    }

    @Test fun `cancel restores volume and clears remaining`() = runTest {
        val volumes = mutableListOf<Float>()
        var expired = 0
        val t = SleepTimer(backgroundScope, setVolume = { volumes.add(it) }, onExpired = { expired++ })
        t.start(40_000)
        advanceTimeBy(25_000); runCurrent()          // внутри fade-зоны
        t.cancel(); runCurrent()
        assertNull(t.remainingMs.value)
        assertEquals(1f, volumes.last())
        advanceTimeBy(60_000); runCurrent()
        assertEquals(0, expired)                     // отменённый не срабатывает
    }

    @Test fun `restart overrides previous timer`() = runTest {
        var expired = 0
        val t = SleepTimer(backgroundScope, setVolume = {}, onExpired = { expired++ })
        t.start(60_000)
        advanceTimeBy(10_000); runCurrent()
        t.start(120_000); runCurrent()               // перезапуск
        assertEquals(120_000L, t.remainingMs.value)
        advanceTimeBy(60_000); runCurrent()
        assertEquals(0, expired)                     // старый уже не жив
        advanceTimeBy(60_000); runCurrent()
        assertEquals(1, expired)
    }

    @Test fun `restart restores volume when previous timer was fading`() = runTest {
        val volumes = mutableListOf<Float>()
        val t = SleepTimer(backgroundScope, setVolume = { volumes.add(it) }, onExpired = {})
        t.start(40_000)
        advanceTimeBy(25_000); runCurrent()   // inside fade window, volume ~0.5
        t.start(60_000); runCurrent()
        assertEquals(1f, volumes.last())      // volume restored on restart
    }
}
