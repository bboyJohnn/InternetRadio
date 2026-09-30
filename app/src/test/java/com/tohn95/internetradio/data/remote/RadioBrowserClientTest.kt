package com.tohn95.internetradio.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class RadioBrowserClientTest {
    private val stationJson = """
        [{"stationuuid":"abc-123","name":"Радио Джаз","url":"http://x/a","url_resolved":"http://x/stream",
          "favicon":"http://x/f.png","tags":"jazz, smooth","country":"Russia","countrycode":"RU",
          "language":"russian","votes":42,"clickcount":100,"codec":"MP3","bitrate":128,
          "lastcheckok":1,"hls":0}]
    """.trimIndent()

    @Test fun `parses stations and maps to domain`() = runTest {
        val server = MockWebServer().apply { enqueue(MockResponse().setBody(stationJson)); start() }
        val client = RadioBrowserClient(ServerProvider(null), overrideBaseUrls = listOf(server.url("/").toString()))
        val result = client.topByClicks(limit = 20, offset = 0)
        assertEquals(1, result.size)
        assertEquals("abc-123", result[0].uuid)
        assertEquals("http://x/stream", result[0].streamUrl)       // url_resolved, не url
        assertEquals(listOf("jazz", "smooth"), result[0].tags)     // CSV -> список
        val req = server.takeRequest()
        assertEquals("InternetRadio/1.2 (tohn95@gmail.com)", req.getHeader("User-Agent"))
        assert(req.path!!.contains("hidebroken=true"))
        assert(req.path!!.contains("limit=20"))
        server.shutdown()
    }

    @Test fun `fails over to second mirror on 500`() = runTest {
        val bad = MockWebServer().apply { enqueue(MockResponse().setResponseCode(500)); start() }
        val good = MockWebServer().apply { enqueue(MockResponse().setBody(stationJson)); start() }
        val client = RadioBrowserClient(ServerProvider(null),
            overrideBaseUrls = listOf(bad.url("/").toString(), good.url("/").toString()))
        val result = client.topByClicks(limit = 20, offset = 0)
        assertEquals(1, result.size)
        bad.shutdown(); good.shutdown()
    }
}
