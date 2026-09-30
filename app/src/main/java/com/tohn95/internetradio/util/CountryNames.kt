package com.tohn95.internetradio.util

import com.tohn95.internetradio.domain.model.Station
import java.util.Locale

/**
 * Название страны на языке приложения по ISO-коду («RU» → «Россия»). Каталог radio-browser отдаёт
 * его по-английски («The Russian Federation»), и в русском интерфейсе это выглядело чужеродно.
 * Нет кода или система его не знает — [fallback] (название из каталога).
 */
fun countryDisplayName(code: String?, fallback: String): String {
    val c = code?.trim()?.uppercase().orEmpty()
    if (c.length != 2) return fallback
    return Locale("", c).getDisplayCountry(Locale.getDefault())
        .takeIf { it.isNotBlank() && !it.equals(c, ignoreCase = true) } ?: fallback
}

/** Страна станции на языке приложения. */
fun Station.countryLabel(): String = countryDisplayName(countryCode, country)
