package com.tohn95.internetradio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.ui.theme.LocalPalette
import kotlin.math.pow

/** Порт BannerWidget из DIV: 4 слоя «gentle wave» (кубические Безье),
 *  дрейф с периодами 7/10/13/20 c, boost-всплеск с затуханием 0.94/40мс. */
@Composable
fun WaveBanner(
    title: String,
    animated: Boolean,
    boostTrigger: Int,
    modifier: Modifier = Modifier,
    height: Dp = 84.dp,
    // <1 — волны полупрозрачные: на Главной сквозь них виден градиент, и скруглённый лист контента
    // поверх баннера читается (при 1 нижняя волна сливается с фоном страницы).
    waveAlpha: Float = 1f,
    // Подъём волн над нижним краем: на Главной низ баннера закрыт скруглённым листом.
    wavesLift: Dp = 0.dp,
) {
    val palette = LocalPalette.current
    var t by remember { mutableFloatStateOf(0f) }
    var boost by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(boostTrigger) { if (boostTrigger > 0 && animated) boost = 1f }
    LaunchedEffect(animated) {
        if (!animated) { boost = 0f; return@LaunchedEffect }
        var last = 0L
        while (true) withFrameNanos { now ->
            if (last != 0L) {
                val dt = (now - last) / 1e9f
                t += dt * (1f + 2f * boost)                       // widgets.py:317
                boost = if (boost > 0.01f) boost * 0.94f.pow(dt / 0.04f) else 0f
            }
            last = now
        }
    }

    // путь «gentle wave» — координаты widgets.py:324-338 дословно
    val wavePath = remember {
        Path().apply {
            moveTo(-160f, 44f)
            cubicTo(-130f, 44f, -102f, 26f, -72f, 26f)
            cubicTo(-42f, 26f, -14f, 44f, 16f, 44f)
            cubicTo(46f, 44f, 74f, 26f, 104f, 26f)
            cubicTo(134f, 26f, 162f, 44f, 192f, 44f)
            lineTo(192f, 92f)
            lineTo(-160f, 92f)
            close()
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier.fillMaxWidth()
            .height(height + statusBarPadding)
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
    ) {
        // Свой слой: волны перерисовываются каждый кадр — без него заново рисовался бы весь экран.
        Canvas(Modifier.fillMaxSize().graphicsLayer()) {
            val w = size.width
            val h = size.height
            // фон: диагональный градиент + два световых пятна (widgets.py:352-362)
            drawRect(
                Brush.linearGradient(
                    0.0f to palette.primaryActive, 0.55f to palette.primary, 1.0f to palette.btnBgActive,
                    start = Offset.Zero, end = Offset(w, h),
                )
            )
            val blob = Color.White.copy(alpha = 26f / 255f)
            drawOval(blob, topLeft = Offset(w * 0.62f, -h * 0.7f), size = Size(h * 1.7f, h * 1.7f))
            drawOval(blob, topLeft = Offset(w * 0.06f, h * 0.35f), size = Size(h * 1.1f, h * 1.1f))

            // волны (widgets.py:371-392)
            val waveH = 30.dp.toPx() * (1f + 0.22f * boost)
            val sx = w / 150f
            val sy = waveH / 32f
            val top = h - waveH - wavesLift.toPx()
            val layers = listOf(
                Triple(0f, 0.25f, 7f), Triple(3f, 0.50f, 10f),
                Triple(5f, 0.75f, 13f), Triple(7f, 1.00f, 20f),
            )
            for ((yOff, alpha, duration) in layers) {
                val phase = (t / duration) % 1f
                val xUnits = 48f - 90f + phase * 175f
                withTransform({
                    translate(0f, top - 20f * sy)
                    scale(sx, sy, pivot = Offset.Zero)
                    translate(xUnits, yOff)
                }) {
                    drawPath(wavePath, color = palette.pageBg.copy(alpha = alpha * waveAlpha))
                }
            }
        }
        // заголовок: центр зоны без нижних 26px — НАД волнами (widgets.py:369)
        Box(
            Modifier.fillMaxWidth().padding(top = statusBarPadding).height(height - 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
