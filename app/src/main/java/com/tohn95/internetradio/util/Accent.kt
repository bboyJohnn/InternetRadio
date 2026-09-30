package com.tohn95.internetradio.util

import android.graphics.Bitmap
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

/**
 * Акцентный цвет (ARGB) из картинки — для плеера и обложки уведомления.
 * null — картинка почти бесцветная (белый фон, серый текст): такой серый акцент не берём.
 * Тяжёлая операция — вызывать не на главном потоке.
 */
fun accentFrom(bitmap: Bitmap): Int? {
    val p = Palette.from(bitmap).maximumColorCount(16).generate()
    val swatch = p.vibrantSwatch ?: p.darkVibrantSwatch ?: p.lightVibrantSwatch ?: p.dominantSwatch
    return swatch?.takeIf { it.hsl[1] >= 0.25f }?.let { normalizeAccent(it.rgb) }
}

/** Яркость акцента — в рабочий диапазон: слишком тёмный не виден на тёмном фоне, светлый выцветает. */
private fun normalizeAccent(rgb: Int): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(rgb, hsl)
    hsl[1] = hsl[1].coerceIn(0.45f, 0.9f)
    hsl[2] = hsl[2].coerceIn(0.45f, 0.62f)
    return ColorUtils.HSLToColor(hsl)
}
