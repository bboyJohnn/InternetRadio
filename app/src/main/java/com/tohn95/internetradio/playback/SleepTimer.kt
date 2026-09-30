package com.tohn95.internetradio.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Таймер сна: обратный отсчёт, линейный fade громкости в последние fadeMs,
 *  по истечении — pause (через onExpired) и восстановление громкости.
 *  Main-confined, по образцу ReconnectScheduler. */
class SleepTimer(
    private val scope: CoroutineScope,
    private val setVolume: (Float) -> Unit,
    private val onExpired: () -> Unit,
    private val fadeMs: Long = 30_000,
    private val tickMs: Long = 1_000,
) {
    private val _remainingMs = MutableStateFlow<Long?>(null)
    val remainingMs: StateFlow<Long?> = _remainingMs
    private var job: Job? = null

    fun start(durationMs: Long) {
        job?.cancel()
        setVolume(1f)
        job = scope.launch {
            var left = durationMs
            _remainingMs.value = left
            while (left > 0) {
                delay(tickMs)
                left -= tickMs
                _remainingMs.value = left
                if (left in 1 until fadeMs) setVolume(left.toFloat() / fadeMs)
            }
            setVolume(1f)
            onExpired()
            _remainingMs.value = null
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = null
        setVolume(1f)
    }
}
