package com.tohn95.internetradio.ui.screens.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Громкость дугой вокруг обложки (по мотивам референса): нижняя полуокружность от «без звука»
 * слева (9 часов) через низ до «громко» справа (3 часа). Тянуть пальцем или ткнуть в точку дуги.
 * Жест перехватываем только рядом с дугой — в остальном месте экран спокойно листается вниз.
 */
@Composable
fun ArcVolumeRing(
    // Лямбда: громкость читается только при рисовании дуги — перетаскивание не пересобирает весь плеер.
    volume: () -> Float,
    onVolumeChange: (Float) -> Unit,
    accent: Color,
    track: Color,
    iconTint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val onChange by rememberUpdatedState(onVolumeChange)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        content()
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                val ringPad = 18.dp.toPx()
                val grab = 34.dp.toPx()
                fun radius() = this.size.width / 2f - ringPad
                fun valueAt(p: Offset): Float {
                    val dx = p.x - this.size.width / 2f
                    val dy = p.y - this.size.height / 2f
                    if (dy < 0f) return if (dx < 0f) 0f else 1f          // выше центра — прижимаем к концам дуги
                    val deg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()   // 0 справа … 180 слева
                    return (1f - deg / 180f).coerceIn(0f, 1f)
                }
                fun nearRing(p: Offset): Boolean {
                    val dx = p.x - this.size.width / 2f
                    val dy = p.y - this.size.height / 2f
                    val d = hypot(dx, dy)
                    return dy > -grab && d > radius() - grab && d < radius() + grab
                }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!nearRing(down.position)) return@awaitEachGesture
                    down.consume()
                    onChange(valueAt(down.position))
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        onChange(valueAt(change.position))
                    }
                }
            },
        ) {
            val volume = volume()
            val stroke = 6.dp.toPx()
            val r = this.size.width / 2f - 18.dp.toPx()
            val c = center
            val topLeft = Offset(c.x - r, c.y - r)
            val arcSize = Size(r * 2, r * 2)
            // Риски снаружи дуги, как шкала.
            for (i in 0..36) {
                val a = Math.toRadians(180.0 - i * 5.0)
                val r1 = r + 10.dp.toPx()
                val r2 = r + (if (i % 6 == 0) 17.dp else 14.dp).toPx()
                drawLine(
                    track, Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                    Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat()),
                    strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round,
                )
            }
            // 180° → против часовой через низ (sweep отрицательный) до 0°.
            drawArc(track, 180f, -180f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(accent, 180f, -180f * volume, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            val a = Math.toRadians(180.0 - 180.0 * volume)
            val knob = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
            drawCircle(accent, radius = 11.dp.toPx(), center = knob)
            drawCircle(Color.White, radius = 4.dp.toPx(), center = knob)
        }
        // Значки на концах дуги.
        Icon(
            Icons.AutoMirrored.Filled.VolumeOff, null, tint = iconTint,
            modifier = Modifier.align(Alignment.CenterStart).offset(y = (-22).dp).size(20.dp),
        )
        Icon(
            Icons.AutoMirrored.Filled.VolumeUp, null, tint = iconTint,
            modifier = Modifier.align(Alignment.CenterEnd).offset(y = (-22).dp).size(20.dp),
        )
    }
}
