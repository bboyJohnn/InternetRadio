package com.tohn95.internetradio.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface MetadataState {
    data object Detecting : MetadataState
    data class Available(val text: String) : MetadataState
    data object NotAvailable : MetadataState
}

/** «Что сейчас играет» по идее RadioWave: Detecting -> (таймаут) NotAvailable | Available. */
class MetadataTracker(
    private val scope: CoroutineScope,
    private val timeoutMs: Long = 5000,
) {
    private val _state = MutableStateFlow<MetadataState>(MetadataState.Detecting)
    val state: StateFlow<MetadataState> = _state
    private var timeout: Job? = null

    fun onStreamStarted() = restartDetection()

    /**
     * Подгрузка буфера той же станции: известный трек не сбрасываем. Плеер шлёт заголовок только
     * при его смене, поэтому после сброса тот же трек заново не пришёл бы — и до следующей песни
     * висело бы «станция не передаёт название».
     */
    fun onBuffering() {
        if (_state.value !is MetadataState.Available) restartDetection()
    }

    fun onIcyTitle(title: String?) {
        val t = title?.trim().orEmpty()
        if (t.isEmpty()) return
        timeout?.cancel()
        _state.value = MetadataState.Available(t)
    }

    private fun restartDetection() {
        timeout?.cancel()
        _state.value = MetadataState.Detecting
        timeout = scope.launch {
            delay(timeoutMs)
            if (_state.value == MetadataState.Detecting) _state.value = MetadataState.NotAvailable
        }
    }
}
