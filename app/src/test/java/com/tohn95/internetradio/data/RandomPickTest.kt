package com.tohn95.internetradio.data

import com.tohn95.internetradio.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RandomPickTest {
    private fun pool(n: Int) = (1..n).map {
        Station("u$it", "S$it", "http://s$it", null, emptyList(), "", "", 0, "", 0, 0, false)
    }

    @Test fun `takes requested count from pool`() {
        val picked = randomPick(pool(300), take = 30, random = Random(1))
        assertEquals(30, picked.size)
        assertEquals(30, picked.map { it.uuid }.toSet().size)   // без повторов
    }

    @Test fun `same seed gives same picks, different seed differs`() {
        val a = randomPick(pool(300), 30, Random(42))
        val b = randomPick(pool(300), 30, Random(42))
        val c = randomPick(pool(300), 30, Random(7))
        assertEquals(a.map { it.uuid }, b.map { it.uuid })
        assertNotEquals(a.map { it.uuid }, c.map { it.uuid })
    }

    @Test fun `pool smaller than take returns whole pool`() {
        val picked = randomPick(pool(10), take = 30, random = Random(1))
        assertEquals(10, picked.size)
    }

    @Test fun `empty pool returns empty`() {
        assertTrue(randomPick(emptyList(), 30, Random(1)).isEmpty())
    }
}
