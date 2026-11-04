package com.tohn95.internetradio.playback

import com.tohn95.internetradio.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QueueStateTest {
    private fun st(id: String) = Station(id, id, "http://$id", null, emptyList(), "", "", 0, "", 0, 0, false)
    private val a = st("a")
    private val b = st("b")
    private val c = st("c")
    private val d = st("d")
    private val e = st("e")
    private val folder = PlaySource.Folder(1, "Rock")

    @Test
    fun start_sets_source_and_next_follows_queue() {
        val q = QueueState()
        q.start(PlayQueue(folder, listOf(a, b, c)), b)
        assertEquals(folder, q.source)
        assertEquals(c, q.next("b", +1, fallback = emptyList()))
        assertEquals(a, q.next("b", -1, fallback = emptyList()))
    }

    @Test
    fun no_queue_uses_fallback_favorites() {
        val q = QueueState()
        q.start(null, e)
        assertEquals(PlaySource.None, q.source)
        assertEquals(a, q.next("e", +1, fallback = listOf(a, b)))
        assertTrue(q.hasNeighbors("e", fallback = listOf(a)))
        assertFalse(q.hasNeighbors("e", fallback = emptyList()))
    }

    @Test
    fun removed_current_keeps_its_place() {
        val q = QueueState()
        q.start(PlayQueue(PlaySource.Favorites, listOf(a, b, c, d)), c)
        q.update(PlaySource.Favorites, listOf(a, b, d), currentUuid = "c")     // ♡ сняли с играющей
        assertEquals(d, q.next("c", +1, fallback = emptyList()))
        assertEquals(b, q.next("c", -1, fallback = emptyList()))
    }

    @Test
    fun update_for_other_source_is_ignored() {
        val q = QueueState()
        q.start(PlayQueue(folder, listOf(a, b)), a)
        q.update(PlaySource.Favorites, listOf(c, d), currentUuid = "a")
        assertEquals(listOf(a, b), q.stations)
    }

    @Test
    fun reorder_moves_anchor_with_current() {
        val q = QueueState()
        q.start(PlayQueue(folder, listOf(a, b, c)), a)
        q.update(folder, listOf(b, c, a), currentUuid = "a")                     // перетащили a в конец
        assertEquals(b, q.next("a", +1, fallback = emptyList()))
        assertEquals(c, q.next("a", -1, fallback = emptyList()))
    }

    @Test
    fun shuffle_keeps_current_first_and_off_restores_order() {
        val q = QueueState(Random(7))
        q.start(PlayQueue(folder, listOf(a, b, c, d, e)), c)
        q.setShuffle(true, currentUuid = "c")
        assertTrue(q.shuffled)
        assertEquals(c, q.stations.first())
        assertEquals(setOf(a, b, c, d, e), q.stations.toSet())
        q.setShuffle(false, currentUuid = "c")
        assertEquals(listOf(a, b, c, d, e), q.stations)
        assertEquals(d, q.next("c", +1, fallback = emptyList()))
    }

    @Test
    fun update_while_shuffled_reconciles() {
        val q = QueueState(Random(1))
        q.start(PlayQueue(folder, listOf(a, b, c)), a)
        q.setShuffle(true, currentUuid = "a")
        val shuffledOrder = q.stations
        q.update(folder, listOf(a, b, c, d), currentUuid = "a")                 // в папку добавили d
        assertEquals(shuffledOrder + d, q.stations)
        q.setShuffle(false, currentUuid = "a")
        assertEquals(listOf(a, b, c, d), q.stations)
    }

    @Test
    fun new_start_resets_shuffle() {
        val q = QueueState(Random(3))
        q.start(PlayQueue(folder, listOf(a, b, c)), a)
        q.setShuffle(true, currentUuid = "a")
        q.start(PlayQueue(PlaySource.History, listOf(c, b, a)), b)
        assertFalse(q.shuffled)
        assertEquals(listOf(c, b, a), q.stations)
    }
}
