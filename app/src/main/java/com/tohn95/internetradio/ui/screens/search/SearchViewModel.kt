package com.tohn95.internetradio.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tohn95.internetradio.data.FilterCatalog
import com.tohn95.internetradio.data.StationRepository
import com.tohn95.internetradio.domain.model.Country
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.domain.model.TagOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

data class SearchFilters(
    val countryCode: String? = null,
    val countryName: String? = null,
    val tag: String? = null,
    val language: String? = null,
    val codec: String? = null,
    val bitrateMin: Int? = null,
) {
    val active: Boolean
        get() = countryCode != null || tag != null || language != null || codec != null || bitrateMin != null
}

enum class SortMode(val order: String, val reverse: Boolean) {
    LISTENERS_DESC("clickcount", true),
    LISTENERS_ASC("clickcount", false),
    VOTES_DESC("votes", true),
    NAME_ASC("name", false),
    NAME_DESC("name", true),
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: StationRepository,
    private val filterCatalog: FilterCatalog,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters
    private val _sort = MutableStateFlow(SortMode.LISTENERS_DESC)
    val sort: StateFlow<SortMode> = _sort
    private val _results = MutableStateFlow<List<Station>>(emptyList())
    val results: StateFlow<List<Station>> = _results
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error

    private val _countryOptions = MutableStateFlow<List<Country>>(emptyList())
    val countryOptions: StateFlow<List<Country>> = _countryOptions
    private val _tagOptions = MutableStateFlow<List<TagOption>>(emptyList())
    val tagOptions: StateFlow<List<TagOption>> = _tagOptions
    private val _codecOptions = MutableStateFlow<List<TagOption>>(emptyList())
    val codecOptions: StateFlow<List<TagOption>> = _codecOptions

    val favoriteUuids: StateFlow<Set<String>> = repository.favoriteUuids()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleFavorite(station: Station) = viewModelScope.launch { repository.toggleFavorite(station) }

    private var offset = 0
    private var pagingJob: Job? = null
    private val pageSize = 30

    init {
        combine(
            _query.debounce(500).distinctUntilChanged(),
            _filters,
            _sort,
        ) { q, f, s -> Triple(q.trim(), f, s) }
            .onEach { (q, f, s) ->
                val textOk = q.length >= 3
                if (!textOk && !f.active) {
                    pagingJob?.cancel()
                    _results.value = emptyList()
                    _error.value = false
                    _loading.value = false
                    return@onEach
                }
                newSearch(if (textOk) q else null, f, s)
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(q: String) { _query.value = q }
    fun setCountry(code: String?, name: String?) { _filters.value = _filters.value.copy(countryCode = code, countryName = name) }
    fun setTag(tag: String?) { _filters.value = _filters.value.copy(tag = tag) }
    fun setCodec(codec: String?) { _filters.value = _filters.value.copy(codec = codec) }
    fun setBitrate(min: Int?) { _filters.value = _filters.value.copy(bitrateMin = min) }
    fun setSort(mode: SortMode) { _sort.value = mode }

    fun loadOptions() {
        viewModelScope.launch {
            if (_countryOptions.value.isEmpty()) _countryOptions.value = filterCatalog.countries()
            if (_tagOptions.value.isEmpty()) _tagOptions.value = filterCatalog.tags()
            if (_codecOptions.value.isEmpty()) _codecOptions.value = filterCatalog.codecs()
        }
    }

    /** «Повторить» из карточки ошибки: тот же запрос с теми же фильтрами. */
    fun retry() {
        val q = _query.value.trim()
        newSearch(if (q.length >= 3) q else null, _filters.value, _sort.value)
    }

    private fun newSearch(name: String?, f: SearchFilters, s: SortMode) {
        pagingJob?.cancel()
        offset = 0
        _error.value = false
        pagingJob = viewModelScope.launch {
            _loading.value = true
            try {
                val r = repository.searchStations(
                    name, f.countryCode, f.tag, f.language, f.codec, f.bitrateMin, s.order, s.reverse, pageSize, 0,
                )
                _results.value = r.stations.distinctBy { it.uuid }
                _error.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _results.value = emptyList()
                _error.value = true
            }
            _loading.value = false
        }
    }

    fun loadMore() {
        val q = _query.value.trim()
        val f = _filters.value
        val s = _sort.value
        val textOk = q.length >= 3
        if (!textOk && !f.active) return
        if (_loading.value) return
        if (pagingJob?.isActive == true) return
        val requestedOffset = offset + pageSize
        pagingJob = viewModelScope.launch {
            try {
                val r = repository.searchStations(
                    if (textOk) q else null, f.countryCode, f.tag, f.language, f.codec, f.bitrateMin,
                    s.order, s.reverse, pageSize, requestedOffset,
                )
                _results.value = (_results.value + r.stations).distinctBy { it.uuid }
                offset = requestedOffset
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // пагинация не удалась — offset не двигаем
            }
        }
    }
}
