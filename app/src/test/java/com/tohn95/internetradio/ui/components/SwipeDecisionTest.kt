package com.tohn95.internetradio.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeDecisionTest {
    @Test
    fun small_drag_does_nothing() {
        assertEquals(0, swipeDecision(-60f, 0f, 1000f, rtl = false))
    }

    @Test
    fun third_left_is_next() {
        assertEquals(+1, swipeDecision(-340f, 0f, 1000f, rtl = false))
    }

    @Test
    fun third_right_is_prev() {
        assertEquals(-1, swipeDecision(340f, 0f, 1000f, rtl = false))
    }

    @Test
    fun fast_flick_counts() {
        assertEquals(+1, swipeDecision(-80f, -2000f, 1000f, rtl = false))
    }

    @Test
    fun tiny_fast_touch_ignored() {
        assertEquals(0, swipeDecision(-10f, -3000f, 1000f, rtl = false))
    }

    @Test
    fun flick_back_cancels() {
        assertEquals(0, swipeDecision(-80f, 2000f, 1000f, rtl = false))
    }

    @Test
    fun rtl_mirrors() {
        assertEquals(-1, swipeDecision(-340f, 0f, 1000f, rtl = true))
    }
}
