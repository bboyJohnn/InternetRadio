package com.tohn95.internetradio.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tohn95.internetradio.data.FilterCatalog
import com.tohn95.internetradio.data.StationRepository
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.ui.screens.genres.Genre
import com.tohn95.internetradio.ui.screens.genres.GenreCatalog
import com.tohn95.internetradio.util.deviceCountryCode
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import javax.inject.Inject

/** Микс жанра: жанр + несколько названий его станций для подписи «A, B и другие». */
data class Mix(val genre: Genre, val stationNames: List<String>)

data class HomeState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: Boolean = false,
    val fromCache: Boolean = false,
    val quickGenres: List<Genre> = GenreCatalog.all.take(6),
    val popular: List<Station> = emptyList(),
    val newStation: Station? = null,
    val forYou: List<Station> = emptyList(),
    val mixes: List<Mix> = emptyList(),
    val trending: List<Station> = emptyList(),
    val moods: List<Mood> = emptyList(),
    val local: List<Station> = emptyList(),
    val world: List<WorldCountry> = emptyList(),
    val hq: List<Station> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: StationRepository,
    private val catalog: FilterCatalog,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    val favoriteUuids: StateFlow<Set<String>> = repository.favoriteUuids()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /** «Недавно слушал» — живой поток из истории, без сети. */
    val recent: StateFlow<List<Station>> = repository.history()
        .map { l -> l.map { it.station }.distinctBy { it.uuid }.take(12) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Страна устройства для секции «Эфир страны»: код ISO (регион системы) и имя на языке приложения. */
    val countryCode: String = context.deviceCountryCode()
    val countryName: String get() = localCountryName(countryCode)

    fun toggleFavorite(station: Station) = viewModelScope.launch { repository.toggleFavorite(station) }

    private var refreshJob: Job? = null

    init { refresh() }

    /**
     * Первый запуск — шиммер; повторный (свайп вниз) — индикатор поверх старого контента.
     * Каждое обновление — новый случайный набор: жанры, миксы, настроения, страны, «Эфир» и «Высокое качество».
     */
    fun refresh() {
        refreshJob?.cancel()
        val first = _state.value.popular.isEmpty()
        _state.value = _state.value.copy(loading = first, refreshing = !first, error = false)
        refreshJob = viewModelScope.launch {
            // Жанры: 6 быстрых + 4 для миксов, без повторов между блоками.
            val genres = GenreCatalog.all.shuffled()
            val quick = genres.take(6)
            val mixGenres = genres.drop(6).take(4)
            coroutineScope {
                val popular = async { safe { repository.randomPopular(poolSize = 40, take = 4) } }
                val newStation = async { safe { repository.newStation() } }
                val forYou = async { safe { forYou() } }
                val trending = async { safe { repository.browse(order = "clicktrend", limit = 40).filter { it.clickCount > 0 }.take(15) } }
                // «Эфир» и «Высокое качество» — случайная выборка из широкого топа, а не каждый раз одни и те же.
                val local = async {
                    safe {
                        if (countryCode.length == 2) repository.browse(countryCode = countryCode, limit = 60).shuffled().take(15)
                        else emptyList()
                    }
                }
                val hq = async { safe { repository.browse(bitrateMin = 256, bitrateMax = 320, limit = 150).shuffled().take(12) } }
                val mixes = mixGenres.map { g ->
                    async {
                        val names = safe { repository.browse(tag = g.tag, limit = 12) }.orEmpty()
                            .map { it.name.trim() }.filter { it.isNotEmpty() }.shuffled().take(3)
                        Mix(g, names)
                    }
                }
                val world = async { safe { worldCountries() } }

                val p = popular.await()
                val shown = p?.stations.orEmpty().map { it.uuid }.toSet()
                _state.value = HomeState(
                    loading = false,
                    refreshing = false,
                    error = p == null,
                    fromCache = p?.fromCache ?: false,
                    quickGenres = quick,
                    popular = p?.stations?.distinctBy { it.uuid }.orEmpty(),
                    newStation = newStation.await(),
                    forYou = forYou.await().orEmpty(),
                    mixes = mixes.awaitAll(),
                    // Чарт без станций, уже показанных в «Популярном» — иначе одни и те же сверху и снизу.
                    trending = trending.await().orEmpty().filter { it.uuid !in shown },
                    moods = HomeCatalog.moods.shuffled().take(4),
                    local = local.await().orEmpty(),
                    world = world.await().orEmpty(),
                    hq = hq.await().orEmpty(),
                )
            }
        }
    }

    /** «Мне повезёт»: случайная станция из топ-400 популярных; сеть подвела — из загруженных популярных. */
    fun lucky(onStation: (Station) -> Unit) = viewModelScope.launch {
        val s = safe { repository.browse(limit = 1, offset = Random.nextInt(0, 400)).firstOrNull() }
            ?: (_state.value.popular + _state.value.forYou).randomOrNull()
        s?.let(onStation)
    }

    /**
     * «Для тебя»: полуслучайная подборка. Берём 3 самых частых жанра из истории, по каждому — популярные
     * станции, убираем уже слушанные, перемешиваем. Истории нет — случайные из второго эшелона популярных.
     */
    private suspend fun forYou(): List<Station> {
        val history = repository.history().first().map { it.station }
        val heard = history.map { it.uuid }.toSet()
        val topTags = history.take(30).flatMap { it.tags.take(3) }
            .map { it.lowercase().trim() }.filter { it.length > 1 }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3).map { it.key }
        val pool = if (topTags.isEmpty()) {
            repository.browse(limit = 200, offset = 40)
        } else {
            coroutineScope {
                topTags.map { t -> async { safe { repository.browse(tag = t, limit = 40) }.orEmpty() } }
                    .flatMap { it.await() }
            }
        }
        return pool.distinctBy { it.uuid }.filter { it.uuid !in heard }.shuffled().take(12)
    }

    /** «Радио мира»: 8 случайных стран из 30 крупнейших по числу станций (кроме своей). */
    private suspend fun worldCountries(): List<WorldCountry> =
        catalog.countries()
            .filter { it.code.length == 2 && !it.code.equals(countryCode, ignoreCase = true) }
            .sortedByDescending { it.stationCount }.take(30).shuffled().take(8)
            .map { WorldCountry(it.code.uppercase(), localCountryName(it.code).ifBlank { it.name }, flagEmoji(it.code)) }

    private fun localCountryName(code: String): String =
        Locale("", code.uppercase()).getDisplayCountry(Locale.getDefault()).ifBlank { code.uppercase() }

    private suspend fun <T> safe(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
