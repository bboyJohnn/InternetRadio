package com.tohn95.internetradio.data.remote

import com.tohn95.internetradio.domain.model.Country
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.domain.model.TagOption
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Singleton
class RadioBrowserClient constructor(
    private val serverProvider: ServerProvider,
    private val overrideBaseUrls: List<String>? = null,
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "InternetRadio/1.2 (tohn95@gmail.com)")
                    .build()
            )
        }
        .build()
    private val apis = ConcurrentHashMap<String, RadioBrowserApi>()

    private fun apiFor(baseUrl: String): RadioBrowserApi = apis.getOrPut(baseUrl) {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(http)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RadioBrowserApi::class.java)
    }

    /** Перебор зеркал до первого успеха; иначе — последняя ошибка (идея RadioWave). */
    private suspend fun <T> withMirrors(block: suspend (RadioBrowserApi) -> T): T {
        val urls = overrideBaseUrls ?: serverProvider.baseUrls()
        var last: Exception? = null
        for (u in urls) {
            try { return block(apiFor(u)) } catch (e: Exception) { last = e }
        }
        throw last ?: IllegalStateException("no mirrors")
    }

    suspend fun topByClicks(limit: Int, offset: Int): List<Station> =
        withMirrors { it.search(order = "clickcount", limit = limit, offset = offset) }.map { it.toDomain() }

    suspend fun topByVotes(limit: Int, offset: Int): List<Station> =
        withMirrors { it.search(order = "votes", limit = limit, offset = offset) }.map { it.toDomain() }

    suspend fun searchStations(
        name: String?, countryCode: String?, tag: String?, language: String?,
        codec: String? = null, bitrateMin: Int? = null,
        order: String = "clickcount", reverse: Boolean = true,
        limit: Int, offset: Int,
        bitrateMax: Int? = null,
    ): List<Station> = withMirrors {
        it.search(
            name = name?.takeIf { n -> n.isNotBlank() },
            countryCode = countryCode, tag = tag, language = language,
            codec = codec, bitrateMin = bitrateMin, bitrateMax = bitrateMax, order = order, reverse = reverse,
            limit = limit, offset = offset,
        )
    }.map { it.toDomain() }

    suspend fun countries(): List<Country> =
        withMirrors { it.countries() }
            .filter { it.iso_3166_1.isNotBlank() && it.name.isNotBlank() }
            .map { Country(it.name, it.iso_3166_1, it.stationcount) }

    suspend fun tags(): List<TagOption> =
        withMirrors { it.tags() }
            .filter { it.name.isNotBlank() }
            .map { TagOption(it.name, it.stationcount) }

    suspend fun codecs(): List<TagOption> =
        withMirrors { it.codecs() }
            .filter { it.name.isNotBlank() }
            .map { TagOption(it.name, it.stationcount) }

    suspend fun trackClick(uuid: String) {
        runCatching { withMirrors { it.trackClick(uuid) } }   // fire-and-forget
    }

    suspend fun vote(uuid: String): VoteDto = withMirrors { it.vote(uuid) }
}
