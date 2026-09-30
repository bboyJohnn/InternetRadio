package com.tohn95.internetradio.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Порт формул из DIV config.py (строки 144–185). Округление — как в Python
 *  (banker's rounding через Math.rint), куб — умножением, корень — cbrt. */
object Oklch {

    private fun cube(x: Double) = x * x * x

    private fun srgb8(c0: Double): Int {
        var c = c0.coerceIn(0.0, 1.0)
        c = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055
        return Math.rint(c.coerceIn(0.0, 1.0) * 255).toInt()
    }

    private fun toRgb(L: Double, C: Double, H: Double): IntArray {
        val h = Math.toRadians(H)
        val a = C * cos(h)
        val b = C * sin(h)
        val l_ = cube(L + 0.3963377774 * a + 0.2158037573 * b)
        val m_ = cube(L - 0.1055613458 * a - 0.0638541728 * b)
        val s_ = cube(L - 0.0894841775 * a - 1.2914855480 * b)
        val r = +4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_
        val g = -1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_
        val bl = -0.0041960863 * l_ - 0.7034186147 * m_ + 1.7076147010 * s_
        return intArrayOf(srgb8(r), srgb8(g), srgb8(bl))
    }

    fun toHex(L: Double, C: Double, H: Double): String {
        val rgb = toRgb(L, C, H)
        return "#%02x%02x%02x".format(rgb[0], rgb[1], rgb[2])
    }

    fun toColor(L: Double, C: Double, H: Double): Color {
        val rgb = toRgb(L, C, H)
        return Color(rgb[0], rgb[1], rgb[2])
    }

    fun fromSrgb(r8: Int, g8: Int, b8: Int): Triple<Double, Double, Double> {
        fun linear(c8: Int): Double {
            val c = c8 / 255.0
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = linear(r8); val g = linear(g8); val b = linear(b8)
        val l_ = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m_ = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s_ = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        val L = 0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_
        val a = 1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_
        val bb = 0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_
        val C = sqrt(a * a + bb * bb)
        val H = (Math.toDegrees(atan2(bb, a)) % 360 + 360) % 360
        return Triple(L, C, H)
    }
}
