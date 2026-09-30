package com.tohn95.internetradio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tohn95.internetradio.domain.model.Station

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val uuid: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?,
    val tagsCsv: String,
    val country: String,
    val countryCode: String,
    val bitrate: Int,
    val codec: String,
    val votes: Int,
    val clickCount: Int,
    val isHls: Boolean,
    val language: String = "",
    val homepage: String? = null,
    val lastCheckOk: Boolean = true,
    val isFavorite: Boolean = false,
    val cachedAt: Long = 0,
    val lastPlayedAt: Long? = null,
)

fun StationEntity.toDomain() = Station(
    uuid, name, streamUrl, faviconUrl,
    tagsCsv.split(',').filter { it.isNotBlank() },
    country, countryCode, bitrate, codec, votes, clickCount, isHls,
    language, homepage, lastCheckOk,
)

fun Station.toEntity(cachedAt: Long, isFavorite: Boolean = false, lastPlayedAt: Long? = null) = StationEntity(
    uuid, name, streamUrl, faviconUrl, tags.joinToString(","),
    country, countryCode, bitrate, codec, votes, clickCount, isHls,
    language, homepage, lastCheckOk,
    isFavorite, cachedAt, lastPlayedAt,
)
