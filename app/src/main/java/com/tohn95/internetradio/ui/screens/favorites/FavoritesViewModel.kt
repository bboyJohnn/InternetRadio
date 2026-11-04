package com.tohn95.internetradio.ui.screens.favorites

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tohn95.internetradio.data.FolderCoverStore
import com.tohn95.internetradio.data.StationRepository
import com.tohn95.internetradio.domain.model.Folder
import com.tohn95.internetradio.domain.model.Station
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: StationRepository,
    private val covers: FolderCoverStore,
    private val saved: SavedStateHandle,
) : ViewModel() {
    val favorites: StateFlow<List<Station>> = repository.favorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders: StateFlow<List<Folder>> = repository.folders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val addedAt: StateFlow<Map<String, Long>> = repository.favoriteAddedAt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val filter = saved.getStateFlow("filter", LibraryFilter.ALL)
    val sort = saved.getStateFlow("sort", FavSort.CUSTOM)
    val query = saved.getStateFlow("query", "")

    private data class Inputs(val folders: List<Folder>, val stations: List<Station>, val addedAt: Map<String, Long>)
    private data class Controls(val filter: LibraryFilter, val sort: FavSort, val query: String)
    private val inputs = combine(folders, favorites, addedAt, ::Inputs)
    private val controls = combine(filter, sort, query, ::Controls)

    val library: StateFlow<Library> = combine(inputs, controls) { input, control ->
        buildLibrary(input.folders, input.stations, input.addedAt, control.filter, control.sort, control.query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Library(emptyList(), emptyList()))

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice
    fun noticeShown() { _notice.value = null }

    fun setFilter(value: LibraryFilter) { saved["filter"] = if (filter.value == value) LibraryFilter.ALL else value }
    fun setSort(value: FavSort) { saved["sort"] = value }
    fun setQuery(value: String) { saved["query"] = value }

    /** Создать папку; если [thenAdd] задан — сразу положить в неё эту станцию. */
    fun createFolder(name: String, thenAdd: Station? = null) = viewModelScope.launch {
        val id = repository.createFolder(name)
        thenAdd?.let { repository.addToFolder(id, it.uuid) }
    }

    fun renameFolder(id: Long, name: String) = viewModelScope.launch { repository.renameFolder(id, name) }
    fun deleteFolder(id: Long) = viewModelScope.launch {
        val oldCover = folders.value.firstOrNull { it.id == id }?.coverPath
        repository.deleteFolder(id)
        covers.delete(oldCover)
    }
    fun setFolders(station: Station, folderIds: Set<Long>) =
        viewModelScope.launch { repository.setFolders(station.uuid, folderIds) }
    fun removeFavorite(station: Station) = viewModelScope.launch { repository.toggleFavorite(station) }
    fun addCustomStation(station: Station) = viewModelScope.launch { repository.addCustomStation(station) }

    fun reorderStations(fromUuid: String, toUuid: String) = viewModelScope.launch {
        repository.reorderFavorites(favorites.value.moveAfter(fromUuid, toUuid) { it.uuid })
    }

    fun reorderFolders(fromId: Long, toId: Long) = viewModelScope.launch {
        repository.reorderFolders(folders.value.moveAfter(fromId, toId) { it.id })
    }

    fun dropInto(uuid: String, folderId: Long) = viewModelScope.launch {
        repository.addToFolder(folderId, uuid)
        _notice.value = folders.value.firstOrNull { it.id == folderId }?.name
    }

    private fun <T, K> List<T>.moveAfter(from: K, to: K, key: (T) -> K): List<K> {
        val item = firstOrNull { key(it) == from } ?: return map(key)
        val remaining = filterNot { key(it) == from }
        val target = remaining.indexOfFirst { key(it) == to }
        if (target < 0) return map(key)
        return (remaining.take(target + 1) + item + remaining.drop(target + 1)).map(key)
    }
}
