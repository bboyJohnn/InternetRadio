package com.tohn95.internetradio.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CountryDto(val name: String = "", val iso_3166_1: String = "", val stationcount: Int = 0)

@Serializable
data class NameCountDto(val name: String = "", val stationcount: Int = 0)

/** Ответ radio-browser на голос: ok=false + message при повторе («too often») или ошибке. */
@Serializable
data class VoteDto(val ok: Boolean = false, val message: String = "")
