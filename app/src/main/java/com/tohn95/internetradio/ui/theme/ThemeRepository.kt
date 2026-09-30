package com.tohn95.internetradio.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ThemeSettings(
    val hue: Int = 60,
    val saturation: Int = 100,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val wavesAnimated: Boolean = true,
    /** Чисто чёрный фон в тёмной теме (OLED — экономит батарею). */
    val trueBlack: Boolean = false,
    /** Цвет из обоев (Material You, Android 12+) вместо ползунка оттенка. */
    val dynamicColor: Boolean = false,
)

private val Context.themeDataStore by preferencesDataStore("theme")

@Singleton
class ThemeRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val hueKey = intPreferencesKey("hue")
    private val satKey = intPreferencesKey("saturation")
    private val modeKey = stringPreferencesKey("mode")
    private val wavesKey = booleanPreferencesKey("waves")
    private val trueBlackKey = booleanPreferencesKey("true_black")
    private val dynamicKey = booleanPreferencesKey("dynamic_color")

    val settings: Flow<ThemeSettings> = context.themeDataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
        ThemeSettings(
            hue = (p[hueKey] ?: 60).coerceIn(0, 360),
            saturation = (p[satKey] ?: 100).coerceIn(50, 160),
            mode = runCatching { ThemeMode.valueOf(p[modeKey] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            wavesAnimated = p[wavesKey] ?: true,
            trueBlack = p[trueBlackKey] ?: false,
            dynamicColor = p[dynamicKey] ?: false,
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Текущие настройки для первого кадра: null — ещё не прочитаны (только при холодном старте).
     * Раньше первый кадр рисовался стандартной янтарной темой и мигал — при запуске, смене языка, повороте.
     */
    val current: StateFlow<ThemeSettings?> = settings.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setHue(v: Int) = context.themeDataStore.edit { it[hueKey] = v.coerceIn(0, 360) }
    suspend fun setSaturation(v: Int) = context.themeDataStore.edit { it[satKey] = v.coerceIn(50, 160) }
    suspend fun setMode(m: ThemeMode) = context.themeDataStore.edit { it[modeKey] = m.name }
    suspend fun setWavesAnimated(b: Boolean) = context.themeDataStore.edit { it[wavesKey] = b }
    suspend fun setTrueBlack(b: Boolean) = context.themeDataStore.edit { it[trueBlackKey] = b }
    suspend fun setDynamicColor(b: Boolean) = context.themeDataStore.edit { it[dynamicKey] = b }
}
