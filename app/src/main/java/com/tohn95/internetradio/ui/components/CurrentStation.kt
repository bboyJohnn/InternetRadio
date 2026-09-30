package com.tohn95.internetradio.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.playback.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** uuid играющей станции — чтобы подсветить её строку в списках (null в превью/без плеера). */
@Composable
fun rememberCurrentUuid(playerController: PlayerController?): String? {
    val flow: StateFlow<Station?> = remember(playerController) {
        playerController?.currentStation ?: MutableStateFlow(null)
    }
    val current by flow.collectAsStateWithLifecycle()
    return current?.uuid
}
