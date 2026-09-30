package com.tohn95.internetradio.domain.model

data class Country(val name: String, val code: String, val stationCount: Int = 0)

/** Пункт справочника жанров: название + число станций (для полноэкранного пикера). */
data class TagOption(val name: String, val stationCount: Int = 0)
