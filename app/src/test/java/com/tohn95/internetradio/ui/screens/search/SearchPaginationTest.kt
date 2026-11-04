package com.tohn95.internetradio.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPaginationTest {
    @Test fun `starts from first page`() {
        val paging = SearchPagination(pageSize = 30)
        assertEquals(0, paging.nextOffset)
        assertFalse(paging.endReached)
    }

    @Test fun `full pages advance offset without reaching end`() {
        val paging = SearchPagination(pageSize = 30)
        paging.onPageReceived(30)
        paging.onPageReceived(30)
        assertEquals(60, paging.nextOffset)
        assertFalse(paging.endReached)
    }

    @Test fun `short page marks end after advancing once`() {
        val paging = SearchPagination(pageSize = 30)
        paging.onPageReceived(30)
        paging.onPageReceived(7)
        assertEquals(60, paging.nextOffset)
        assertTrue(paging.endReached)
    }

    @Test fun `new search resets offset and end`() {
        val paging = SearchPagination(pageSize = 30)
        paging.onPageReceived(2)
        paging.reset()
        assertEquals(0, paging.nextOffset)
        assertFalse(paging.endReached)
    }
}
