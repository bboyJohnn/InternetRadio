package com.tohn95.internetradio.ui.theme

import androidx.compose.ui.graphics.Color

data class Palette(
    val primary: Color, val primaryHover: Color, val primaryActive: Color,
    val pageBg: Color, val pageBgDeep: Color, val cardBg: Color, val cardBorder: Color,
    val inputBg: Color, val btnBg: Color, val btnBgHover: Color, val btnBgActive: Color,
    val btnText: Color, val text: Color, val textMuted: Color, val itemBg: Color,
    val track: Color, val thumbBg: Color, val green: Color, val red: Color,
    val isDark: Boolean,
)

/** [trueBlack] — чисто чёрный фон в тёмной теме (OLED); карточки чуть темнее обычных, чтобы не терять контраст. */
fun buildPalette(hue: Int, saturationPercent: Int, dark: Boolean, trueBlack: Boolean = false): Palette {
    val h = hue.coerceIn(0, 360).toDouble()
    val s = saturationPercent.coerceIn(50, 160) / 100.0
    fun c(base: Double) = base * s
    fun o(L: Double, C: Double) = Oklch.toColor(L, C, h)

    val base = if (dark) Palette(
        primary = o(.75, c(.14)), primaryHover = o(.70, c(.14)), primaryActive = o(.65, c(.13)),
        pageBg = o(.16, c(.014)), pageBgDeep = o(.10, c(.014)),
        cardBg = o(.23, c(.015)), cardBorder = o(.33, c(.02)), inputBg = o(.19, c(.012)),
        btnBg = o(.33, c(.035)), btnBgHover = o(.38, c(.04)), btnBgActive = o(.43, c(.045)),
        btnText = o(.80, c(.10)), text = o(.93, c(.01)), textMuted = o(.68, c(.02)),
        itemBg = o(.26, c(.015)), track = o(.34, c(.02)), thumbBg = o(.30, c(.015)),
        green = Oklch.toColor(.75, .13, 150.0), red = Oklch.toColor(.72, .16, 25.0),
        isDark = true,
    ) else Palette(
        primary = o(.70, c(.14)), primaryHover = o(.63, c(.13)), primaryActive = o(.58, c(.12)),
        pageBg = o(.95, c(.01)), pageBgDeep = o(.86, c(.03)),
        cardBg = Color.White, cardBorder = o(.90, c(.012)), inputBg = Color.White,
        btnBg = o(.95, c(.025)), btnBgHover = o(.90, c(.05)), btnBgActive = o(.85, c(.08)),
        btnText = o(.55, c(.12)), text = o(.25, c(.02)), textMuted = o(.55, c(.02)),
        itemBg = o(.975, c(.008)), track = o(.92, c(.02)), thumbBg = o(.93, c(.015)),
        green = Oklch.toColor(.62, .14, 150.0), red = Oklch.toColor(.55, .19, 25.0),
        isDark = false,
    )
    return if (dark && trueBlack) base.copy(
        pageBg = Color.Black, pageBgDeep = Color.Black,
        cardBg = o(.17, c(.012)), inputBg = o(.13, c(.01)), itemBg = o(.19, c(.012)),
        thumbBg = o(.22, c(.012)), track = o(.28, c(.015)),
    ) else base
}
