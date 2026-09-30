package com.tohn95.internetradio.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * «Волна эфира» вместо полосы перемотки (у радио её нет): декоративные бары, которые двигаются,
 * пока станция играет, и затихают на паузе. Слева от центральной черты — акцентом, справа — приглушённо.
 */
@Composable
fun LiveWaveform(playing: Boolean, accent: Color, muted: Color, modifier: Modifier = Modifier) {
    var t by remember { mutableFloatStateOf(0f) }
    val level by animateFloatAsState(if (playing) 1f else 0.15f, label = "waveLevel")
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        var last = 0L
        while (true) withFrameNanos { now ->
            if (last != 0L) t += (now - last) / 1e9f
            last = now
        }
    }
    // Свой слой: бары меняются каждый кадр — остальной плеер не перерисовываем.
    Canvas(modifier.fillMaxWidth().height(40.dp).graphicsLayer()) {
        val bars = 46
        val gap = size.width / bars
        val barW = gap * 0.42f
        val mid = size.height / 2f
        val center = bars / 2
        for (i in 0 until bars) {
            val shape = abs(sin(i * 0.55f + t * 2.2f) * cos(i * 0.23f - t * 1.3f))
            // К краям бары ниже — волна «собирается» к центру, как на референсе.
            val envelope = 1f - abs(i - center) / center.toFloat() * 0.55f
            val h = (0.12f + 0.88f * shape * level) * envelope * size.height
            val x = gap * i + gap / 2f
            val color = when {
                i == center -> Color.White
                i < center -> accent
                else -> muted
            }
            val w = if (i == center) barW * 1.3f else barW
            val hh = if (i == center) size.height else h
            drawLine(color, Offset(x, mid - hh / 2f), Offset(x, mid + hh / 2f), strokeWidth = w, cap = StrokeCap.Round)
        }
    }
}
