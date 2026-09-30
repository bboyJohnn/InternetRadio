package com.tohn95.internetradio.ui.screens.stationlist

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.tohn95.internetradio.R
import com.tohn95.internetradio.data.StationRepository
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.ui.navigation.ListKind
import com.tohn95.internetradio.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

enum class ListSort(val order: String, val reverse: Boolean, @StringRes val label: Int) {
    TREND("clicktrend", true, R.string.sort_trend),
    LISTENERS_DESC("clickcount", true, R.string.sort_listeners_desc),
    LISTENERS_ASC("clickcount", false, R.string.sort_listeners_asc),
    NAME_ASC("name", false, R.string.sort_name_asc),
    NAME_DESC("name", true, R.string.sort_name_desc),
}

data class StationListState(
    val stations: List<Station> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: Boolean = false,
)

/**
 * Жанр и страна — «все станции» с сортировкой на сервере и подгрузкой страницами.
 * Популярное/тренды/HQ — сначала отбираем топ (он и есть «популярное»), потом сортируем его
 * локально: иначе «А–Я» по всему каталогу выдал бы случайные станции на «!» и цифры.
 */
@HiltViewModel
class StationListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: StationRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<Route.StationList>()
    val title: String = route.title
    private val kind = runCatching { ListKind.valueOf(route.kind) }.getOrDefault(ListKind.POPULAR)
    private val paged = kind == ListKind.TAG || kind == ListKind.COUNTRY

    val sorts: List<ListSort> = if (kind == ListKind.TREND) ListSort.entries else ListSort.entries - ListSort.TREND
    private val _sort = MutableStateFlow(sorts.first())
    val sort: StateFlow<ListSort> = _sort

    private val _state = MutableStateFlow(StationListState())
    val state: StateFlow<StationListState> = _state

    /** Поиск по названию внутри списка. */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private var queryJob: Job? = null

    private var pool: List<Station>? = null      // для не-постраничных видов: топ, который сортируем локально
    private var job: Job? = null
    private val collator = Collator.getInstance(Locale.getDefault())

    init { reload() }

    fun setSort(s: ListSort) {
        if (s == _sort.value) return
        _sort.value = s
        if (paged) reload() else pool?.let { _state.value = _state.value.copy(stations = local(it)) } ?: reload()
    }

    /**
     * Жанр/страна — ищем на сервере (имя + тот же тег/страна), с задержкой 400 мс на набор.
     * Популярное/тренды/HQ — фильтруем уже загруженный топ прямо на телефоне, мгновенно.
     */
    fun setQuery(q: String) {
        _query.value = q
        queryJob?.cancel()
        if (paged) {
            queryJob = viewModelScope.launch { delay(400); reload() }
        } else {
            pool?.let { _state.value = _state.value.copy(stations = local(it)) }
        }
    }

    private fun local(base: List<Station>): List<Station> {
        val q = _query.value.trim()
        val filtered = if (q.isEmpty()) base else base.filter { it.name.contains(q, ignoreCase = true) }
        return sortLocal(filtered, _sort.value)
    }

    fun reload() {
        job?.cancel()
        _state.value = StationListState(loading = true)
        job = viewModelScope.launch {
            try {
                if (paged) {
                    val page = fetch(offset = 0)
                    _state.value = StationListState(loading = false, stations = byName(page), endReached = page.size < PAGE)
                } else {
                    val base = when (kind) {
                        ListKind.TREND -> repository.browse(order = "clicktrend", limit = 200).filter { it.clickCount > 0 }
                        ListKind.HQ -> repository.browse(bitrateMin = 256, bitrateMax = 320, limit = 300)
                        else -> repository.browse(order = "clickcount", limit = 300)
                    }
                    pool = base
                    _state.value = StationListState(loading = false, stations = local(base), endReached = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = StationListState(loading = false, error = true)
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (!paged || s.loading || s.loadingMore || s.endReached || s.error) return
        _state.value = s.copy(loadingMore = true)
        job = viewModelScope.launch {
            try {
                val page = fetch(offset = s.stations.size)
                val merged = byName((s.stations + page).distinctBy { it.uuid })
                _state.value = s.copy(stations = merged, loadingMore = false, endReached = page.size < PAGE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = s.copy(loadingMore = false)
            }
        }
    }

    private suspend fun fetch(offset: Int): List<Station> {
        val s = _sort.value
        return repository.browse(
            tag = route.arg.takeIf { kind == ListKind.TAG },
            countryCode = route.arg.takeIf { kind == ListKind.COUNTRY },
            name = _query.value.trim().takeIf { it.length >= 2 },
            order = s.order, reverse = s.reverse, limit = PAGE, offset = offset,
        )
    }

    private fun sortLocal(list: List<Station>, s: ListSort): List<Station> = when (s) {
        ListSort.TREND -> list
        ListSort.LISTENERS_DESC -> list.sortedByDescending { it.clickCount }
        ListSort.LISTENERS_ASC -> list.sortedBy { it.clickCount }
        ListSort.NAME_ASC -> list.sortedWith(compareBy(collator) { nameKey(it) })
        ListSort.NAME_DESC -> list.sortedWith(compareByDescending(collator) { nameKey(it) })
    }

    // Сервер сортирует «сырые» имена: станции с пробелом или символом в начале названия выскакивают
    // первыми. Поэтому при сортировке по имени пересортировываем загруженное локально.
    private fun byName(list: List<Station>): List<Station> =
        if (_sort.value == ListSort.NAME_ASC || _sort.value == ListSort.NAME_DESC) sortLocal(list, _sort.value) else list

    private fun nameKey(s: Station): String = s.name.trim().trimStart { !it.isLetterOrDigit() }

    private companion object { const val PAGE = 40 }
}
