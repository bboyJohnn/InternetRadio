package com.tohn95.internetradio.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class PaletteTest {
    private fun hex(c: Color): String =
        "#%02x%02x%02x".format((c.red * 255 + 0.5f).toInt(), (c.green * 255 + 0.5f).toInt(), (c.blue * 255 + 0.5f).toInt())

    @Test fun `dark palette at hue 60 equals alammofm dark tokens`() {
        val p = buildPalette(hue = 60, saturationPercent = 100, dark = true)
        assertEquals("#ee9748", hex(p.primary))   // alammofm dark primary
        assertEquals("#120c07", hex(p.pageBg))    // alammofm dark page_bg
        assertEquals("#221b16", hex(p.cardBg))    // alammofm dark card_bg
        assertEquals(true, p.isDark)
    }

    @Test fun `light palette at hue 60 equals alammofm light tokens`() {
        val p = buildPalette(hue = 60, saturationPercent = 100, dark = false)
        assertEquals("#f4ede8", hex(p.pageBg))    // alammofm light page_bg
        assertEquals("#ffffff", hex(p.cardBg))    // фикс в светлой теме (config.py:248)
        assertEquals("#ffffff", hex(p.inputBg))   // фикс (config.py:250)
    }

    @Test fun `saturation scales chroma and clamps input`() {
        val vivid = buildPalette(60, 160, true)
        val mono = buildPalette(60, 50, true)
        // при меньшей насыщенности каналы primary сближаются (цвет серее)
        val vDelta = vivid.primary.red - vivid.primary.blue
        val mDelta = mono.primary.red - mono.primary.blue
        assert(vDelta > mDelta)
        // клампы не бросают исключений: sat 200->160, 10->50; hue -5->0, 400->360
        buildPalette(60, 200, true); buildPalette(60, 10, true)
        buildPalette(-5, 100, true); buildPalette(400, 100, true)
    }

    @Test fun `green and red ignore theme hue`() {
        val a = buildPalette(0, 100, true)
        val b = buildPalette(200, 100, true)
        assertEquals(hex(a.green), hex(b.green))
        assertEquals(hex(a.red), hex(b.red))
    }
}
