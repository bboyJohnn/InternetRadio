package com.tohn95.internetradio.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationDtoMappingTest {
    @Test fun `maps new fields language homepage lastCheckOk`() {
        val dto = StationDto(
            stationuuid = "u1", name = "Radio X", url_resolved = "http://x/s",
            language = "russian", homepage = "https://radiox.example", lastcheckok = 1,
        )
        val s = dto.toDomain()
        assertEquals("russian", s.language)
        assertEquals("https://radiox.example", s.homepage)
        assertTrue(s.lastCheckOk)
    }

    @Test fun `blank homepage becomes null and lastcheckok zero is false`() {
        val s = StationDto(stationuuid = "u2", name = "Y", url = "http://y", homepage = "", lastcheckok = 0).toDomain()
        assertNull(s.homepage)
        assertEquals(false, s.lastCheckOk)
    }
}
