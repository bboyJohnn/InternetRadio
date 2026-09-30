package com.tohn95.internetradio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import android.os.Build
import kotlin.math.roundToInt

val LocalPalette = compositionLocalOf { buildPalette(60, 100, dark = false) }

@Composable
fun InternetRadioTheme(settings: ThemeSettings, content: @Composable () -> Unit) {
    val dark = when (settings.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // «Цвета из обоев» (Android 12+): оттенок и насыщенность берём из акцента системы (Material You),
    // а сама палитра остаётся нашей OKLCH — меняется только цвет.
    val (hue, saturation) = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        wallpaperHueSat(colorResource(android.R.color.system_accent1_500).toArgb())
    } else settings.hue to settings.saturation
    val palette = remember(hue, saturation, dark, settings.trueBlack) {
        buildPalette(hue, saturation, dark, settings.trueBlack)
    }
    val scheme = if (dark) darkColorScheme(
        primary = palette.primary, onPrimary = palette.pageBg,
        background = palette.pageBg, onBackground = palette.text,
        surface = palette.cardBg, onSurface = palette.text,
        surfaceVariant = palette.itemBg, onSurfaceVariant = palette.textMuted,
        secondaryContainer = palette.btnBg, onSecondaryContainer = palette.btnText,
        outline = palette.cardBorder, error = palette.red,
    ) else lightColorScheme(
        primary = palette.primary, onPrimary = palette.cardBg,
        background = palette.pageBg, onBackground = palette.text,
        surface = palette.cardBg, onSurface = palette.text,
        surfaceVariant = palette.itemBg, onSurfaceVariant = palette.textMuted,
        secondaryContainer = palette.btnBg, onSecondaryContainer = palette.btnText,
        outline = palette.cardBorder, error = palette.red,
    )
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Акцент обоев (ARGB) → наш оттенок OKLCH и насыщенность в % (как у пипетки готовых цветов). */
private fun wallpaperHueSat(argb: Int): Pair<Int, Int> {
    val (_, c, h) = Oklch.fromSrgb((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
    return h.roundToInt().coerceIn(0, 360) to ((c / 0.14) * 100).roundToInt().coerceIn(50, 160)
}
