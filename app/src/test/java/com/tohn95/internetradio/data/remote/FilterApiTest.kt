package com.tohn95.internetradio.data.remote

import com.tohn95.internetradio.domain.model.Country
import com.tohn95.internetradio.domain.model.TagOption
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterApiTest {
    private val stationJson = """
        [{"stationuuid":"u1","name":"S","url":"http://x/a","url_resolved":"http://x/s",
          "favicon":"","tags":"jazz","country":"France","countrycode":"FR","language":"french",
          "votes":1,"clickcount":2,"codec":"MP3","bitrate":128,"lastcheckok":1,"hls":0}]
    """.trimIndent()

    @Test fun `search passes filters as query params and omits nulls`() = runTest {
        val server = MockWebServer().apply { enqueue(MockResponse().setBody(stationJson)); start() }
        val client = RadioBrowserClient(ServerProvider(null), overrideBaseUrls = listOf(server.url("/").toString()))
        client.searchStations(name = null, countryCode = "FR", tag = "jazz", language = null, limit = 30, offset = 0)
        val path = server.takeRequest().path!!
        assertTrue(path.contains("countrycode=FR"))
        assertTrue(path.contains("tag=jazz"))
        assertFalse(path.contains("language="))
        assertFalse(path.contains("name="))
        assertTrue(path.contains("hidebroken=true"))
        server.shutdown()
    }

    @Test fun `countries and tags parse`() = runTest {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody("""[{"name":"France","iso_3166_1":"FR","stationcount":100}]"""))
            enqueue(MockResponse().setBody("""[{"name":"jazz","stationcount":500}]"""))
            start()
        }
        val client = RadioBrowserClient(ServerProvider(null), overrideBaseUrls = listOf(server.url("/").toString()))
        assertEquals(listOf(Country("France", "FR", 100)), client.countries())
        assertEquals(listOf(TagOption("jazz", 500)), client.tags())
        server.shutdown()
    }
}
