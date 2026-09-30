package com.tohn95.internetradio.data

import android.content.Context
import androidx.core.content.edit
import com.tohn95.internetradio.domain.model.Station
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Логотипы, которые пользователь сам выбрал для станций («Найти логотип»): uuid → ссылка на картинку.
 * Маленький словарь — хватает SharedPreferences, без миграции базы.
 */
@Singleton
class LogoOverrides @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("logo_overrides", Context.MODE_PRIVATE)
    private val _map = MutableStateFlow(prefs.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap())
    val map: StateFlow<Map<String, String>> = _map

    fun set(uuid: String, url: String) {
        prefs.edit { putString(uuid, url) }
        _map.value = _map.value + (uuid to url)
    }

    /** Станция с выбранным пользователем логотипом (если он есть). */
    fun apply(station: Station): Station = _map.value[station.uuid]?.let { station.copy(faviconUrl = it) } ?: station
    fun apply(stations: List<Station>): List<Station> = if (_map.value.isEmpty()) stations else stations.map(::apply)
}
