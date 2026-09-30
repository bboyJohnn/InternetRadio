package com.tohn95.internetradio.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class OklchTest {
    @Test fun `toHex matches python goldens`() {
        assertEquals("#ee9748", Oklch.toHex(0.75, 0.14, 60.0))
        assertEquals("#120c07", Oklch.toHex(0.16, 0.014, 60.0))
        assertEquals("#f4ede8", Oklch.toHex(0.95, 0.01, 60.0))
        assertEquals("#221b16", Oklch.toHex(0.23, 0.015, 60.0))
        assertEquals("#659ff4", Oklch.toHex(0.70, 0.14, 258.0))
        assertEquals("#6cc581", Oklch.toHex(0.75, 0.13, 150.0))
        assertEquals("#f97770", Oklch.toHex(0.72, 0.16, 25.0))
        assertEquals("#f3a3bb", Oklch.toHex(0.8, 0.1, 0.0))
        assertEquals("#f3a3bb", Oklch.toHex(0.8, 0.1, 360.0))   // hue цикличен
        assertEquals("#eeff61", Oklch.toHex(1.0, 0.2, 120.0))   // кламп сверху
        assertEquals("#000001", Oklch.toHex(0.0, 0.1, 200.0))   // кламп снизу
        assertEquals("#636363", Oklch.toHex(0.5, 0.0, 0.0))     // серый при C=0
    }

    @Test fun `fromSrgb matches python goldens`() {
        val (l, c, h) = Oklch.fromSrgb(0xdd, 0x87, 0x36)   // #dd8736 (alammofm primary)
        assertEquals(0.700658, l, 1e-5)
        assertEquals(0.140301, c, 1e-5)
        assertEquals(59.925359, h, 1e-3)
        val (l2, c2, h2) = Oklch.fromSrgb(0x3b, 0x82, 0xf6) // #3b82f6
        assertEquals(0.623083, l2, 1e-5)
        assertEquals(0.188015, c2, 1e-5)
        assertEquals(259.814527, h2, 1e-3)
        val black = Oklch.fromSrgb(0, 0, 0)
        assertEquals(0.0, black.first, 1e-9)
        assertEquals(0.0, black.second, 1e-9)
    }
}
