package com.tohn95.internetradio.playback

import com.tohn95.internetradio.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayQueueTest {
    private fun st(id: String) = Station(id, id, "http://$id", null, emptyList(), "", "", 0, "", 0, 0, false)
    private val a = st("a")
    private val b = st("b")
    private val c = st("c")
    private val d = st("d")

    @Test
    fun next_and_prev_wrap_around() {
        val list = listOf(a, b, c)
        assertEquals(a, neighbor(list, "c", 2, +1))
        assertEquals(c, neighbor(list, "a", 0, -1))
    }

    @Test
    fun current_missing_uses_anchor() {
        val list = listOf(a, b, d)            // c стояла на месте 2 и её убрали
        assertEquals(d, neighbor(list, "c", 2, +1))
        assertEquals(b, neighbor(list, "c", 2, -1))
    }

    @Test
    fun anchor_beyond_end_wraps() {
        val list = listOf(a, b)               // убрали последнюю (стояла на месте 2)
        assertEquals(a, neighbor(list, "c", 2, +1))
        assertEquals(b, neighbor(list, "c", 2, -1))
    }

    @Test
    fun not_in_list_without_anchor() {
        val list = listOf(a, b, c)
        assertEquals(a, neighbor(list, "x", -1, +1))
        assertEquals(c, neighbor(list, "x", -1, -1))
    }

    @Test
    fun empty_list_gives_null() {
        assertNull(neighbor(emptyList(), "a", 0, 1))
    }

    @Test
    fun has_neighbors_rules() {
        assertFalse(hasNeighbors(emptyList(), "a"))
        assertFalse(hasNeighbors(listOf(a), "a"))
        assertTrue(hasNeighbors(listOf(a), "x"))
        assertTrue(hasNeighbors(listOf(a, b), "a"))
    }

    @Test
    fun reconcile_keeps_shuffle_order() {
        assertEquals(listOf(a, b, d), reconcile(oldOrder = listOf(c, a, b), fresh = listOf(a, b, d)))
        assertEquals(listOf(b, a, c), reconcile(oldOrder = listOf(b, a, c), fresh = listOf(a, b, c)))
    }
}
