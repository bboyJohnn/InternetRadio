package com.tohn95.internetradio.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.pow

/** Переподключение при обрыве: backoff 1,2,4,...,30 c, максимум 10 попыток;
 *  возврат сети -> немедленный retry и сброс счётчика. Своя разработка. */
class ReconnectScheduler(
    private val scope: CoroutineScope,
    private val retry: suspend () -> Unit,
) {
    private val _attempts = MutableStateFlow(0)
    val attempts: StateFlow<Int> = _attempts

    /** Сдались: попытки кончились или ошибка безнадёжная (формат не поддерживается, 404). */
    private val _gaveUp = MutableStateFlow(false)
    val gaveUp: StateFlow<Boolean> = _gaveUp
    private var pending: Job? = null

    fun onError() {
        if (_attempts.value >= MAX_ATTEMPTS) { _gaveUp.value = true; return }
        pending?.cancel()
        val n = _attempts.value
        val delaySec = min(30.0, 2.0.pow(n)).toLong()
        pending = scope.launch {
            delay(delaySec * 1000)
            _attempts.value = n + 1
            retry()
        }
    }

    fun onNetworkAvailable() {
        if (_attempts.value == 0 && pending == null) return
        pending?.cancel(); pending = null
        _attempts.value = 0
        _gaveUp.value = false
        scope.launch { retry() }
    }

    fun reset() {
        pending?.cancel(); pending = null
        _attempts.value = 0
        _gaveUp.value = false
    }

    /** Безнадёжная ошибка — не мучаем сеть повторами. */
    fun giveUp() {
        pending?.cancel(); pending = null
        _gaveUp.value = true
    }

    companion object { const val MAX_ATTEMPTS = 10 }
}
