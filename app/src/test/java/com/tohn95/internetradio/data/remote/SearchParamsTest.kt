package com.tohn95.internetradio.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchParamsTest {
    @Test fun `sends bitrateMin order and reverse`() = runTest {
        val server = MockWebServer().apply { enqueue(MockResponse().setBody("[]")); start() }
        val client = RadioBrowserClient(ServerProvider(null), overrideBaseUrls = listOf(server.url("/").toString()))
        client.searchStations(
            name = null, countryCode = null, tag = null, language = null,
            bitrateMin = 128, order = "name", reverse = false, limit = 30, offset = 0,
        )
        val path = server.takeRequest().path!!
        assertTrue(path.contains("bitrateMin=128"))
        assertTrue(path.contains("order=name"))
        assertTrue(path.contains("reverse=false"))
        server.shutdown()
    }

    @Test fun `omits bitrateMin when null`() = runTest {
        val server = MockWebServer().apply { enqueue(MockResponse().setBody("[]")); start() }
        val client = RadioBrowserClient(ServerProvider(null), overrideBaseUrls = listOf(server.url("/").toString()))
        client.searchStations(
            name = "jazz", countryCode = null, tag = null, language = null,
            bitrateMin = null, order = "clickcount", reverse = true, limit = 30, offset = 0,
        )
        val path = server.takeRequest().path!!
        assertFalse(path.contains("bitrateMin"))
        assertTrue(path.contains("order=clickcount"))
        server.shutdown()
    }
}
