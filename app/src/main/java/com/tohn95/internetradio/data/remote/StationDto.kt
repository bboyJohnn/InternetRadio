package com.tohn95.internetradio.data.remote

import com.tohn95.internetradio.domain.model.Station
import kotlinx.serialization.Serializable

@Serializable
data class StationDto(
    val stationuuid: String = "",
    val name: String = "",
    val url: String = "",
    val url_resolved: String = "",
    val favicon: String = "",
    val homepage: String = "",
    val tags: String = "",
    val country: String = "",
    val countrycode: String = "",
    val language: String = "",
    val votes: Int = 0,
    val clickcount: Int = 0,
    val codec: String = "",
    val bitrate: Int = 0,
    val lastcheckok: Int = 0,
    val hls: Int = 0,
)

fun StationDto.toDomain() = Station(
    uuid = stationuuid,
    name = name.trim().ifEmpty { "Radio" },
    streamUrl = url_resolved.ifBlank { url },
    faviconUrl = favicon.takeIf { it.startsWith("http") },
    tags = tags.split(',').map { it.trim() }.filter { it.isNotBlank() },
    country = country, countryCode = countrycode,
    bitrate = bitrate, codec = codec, votes = votes, clickCount = clickcount,
    isHls = hls == 1,
    language = language,
    homepage = homepage.takeIf { it.startsWith("http") },
    lastCheckOk = lastcheckok == 1,
)
