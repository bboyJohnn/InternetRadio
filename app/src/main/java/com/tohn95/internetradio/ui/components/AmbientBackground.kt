package com.tohn95.internetradio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.tohn95.internetradio.ui.theme.LocalPalette
import com.tohn95.internetradio.ui.theme.Oklch
import com.tohn95.internetradio.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Двигать ли фоновые переливы (тот же переключатель, что и волны баннера). */
val LocalAmbientAnimated = compositionLocalOf { true }

/** Где «живут» пятна: у верха экрана (обычные экраны) или ниже середины (Главная — там сверху баннер). */
enum class AmbientAnchor { TOP, LOWER }

/** Цвет пятна по оттенку OKLCH — под тему: в тёмной насыщеннее, в светлой пастельнее. */
fun Palette.ambient(hue: Double): Color =
    if (isDark) Oklch.toColor(0.58, 0.15, hue) else Oklch.toColor(0.80, 0.12, hue)

/**
 * Мягкая «подложка-перелив» как у плеера: 2–3 размытых цветных пятна, медленно дрейфующих
 * (период 20–35 с) и тающих к низу. Намеренно бледная — фон, а не украшение.
 */
@Composable
fun AmbientBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    anchor: AmbientAnchor = AmbientAnchor.TOP,
    strength: Float = 1f,
) {
    val palette = LocalPalette.current
    val animated = LocalAmbientAnimated.current
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animated) {
        if (!animated) return@LaunchedEffect
        // ~15 кадров/с хватает для очень медленного дрейфа. Шаг ждёт кадр экрана: когда приложение
        // свёрнуто, кадров нет — цикл засыпает, а не будит телефон 15 раз в секунду под музыку.
        var last = withFrameMillis { it }
        while (true) {
            delay(66)
            val now = withFrameMillis { it }
            t += ((now - last) / 1000f).coerceAtMost(0.2f)   // после сворачивания пятна не прыгают
            last = now
        }
    }
    val alpha = (if (palette.isDark) 0.24f else 0.34f) * strength
    // (x, y, радиус) в долях ширины экрана; y для TOP — от верха, для LOWER — ниже середины.
    val spots = when (anchor) {
        AmbientAnchor.TOP -> listOf(Triple(0.10f, 0.02f, 0.95f), Triple(0.95f, 0.22f, 0.80f), Triple(0.50f, 0.55f, 0.65f))
        AmbientAnchor.LOWER -> listOf(Triple(0.05f, 1.10f, 0.85f), Triple(0.95f, 1.45f, 0.80f), Triple(0.55f, 1.85f, 0.60f))
    }
    // Свой слой: пятна перерисовываются сами, не заставляя перерисовываться весь экран поверх них.
    Canvas(modifier.fillMaxSize().graphicsLayer()) {
        val w = size.width
        colors.take(3).forEachIndexed { i, c ->
            val (fx, fy, fr) = spots[i]
            val phase = (t / (22f + i * 6.5f)) * 2f * PI.toFloat()
            val center = Offset(
                w * fx + w * 0.07f * sin(phase),
                w * fy + w * 0.05f * cos(phase * 0.8f),
            )
            val r = w * fr
            drawCircle(
                Brush.radialGradient(
                    listOf(c.copy(alpha = alpha), c.copy(alpha = alpha * 0.45f), Color.Transparent),
                    center = center, radius = r,
                ),
                radius = r, center = center,
            )
        }
    }
}

/** Оттенок (OKLCH hue) основного цвета темы — от него считаются цвета переливов экранов. */
fun Palette.baseHue(): Double = Oklch.fromSrgb(
    (primary.red * 255).roundToInt(), (primary.green * 255).roundToInt(), (primary.blue * 255).roundToInt(),
).third
