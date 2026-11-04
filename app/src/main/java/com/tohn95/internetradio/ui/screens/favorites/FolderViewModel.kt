package com.tohn95.internetradio.ui.screens.favorites

import android.net.Uri
import androidx.compose.ui.unit.IntRect
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tohn95.internetradio.data.FolderCoverStore
import com.tohn95.internetradio.data.StationRepository
import com.tohn95.internetradio.domain.model.Folder
import com.tohn95.internetradio.domain.model.Station
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val repository: StationRepository,
    private val covers: FolderCoverStore,
    saved: SavedStateHandle,
) : ViewModel() {
    private val folderId: Long = saved.get<Long>("id") ?: 0L

    val folder: StateFlow<Folder?> = repository.folders()
        .map { folders -> folders.firstOrNull { it.id == folderId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val favorites: StateFlow<List<Station>> = repository.favorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val folders: StateFlow<List<Folder>> = repository.folders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun rename(name: String) = viewModelScope.launch { repository.renameFolder(folderId, name) }

    fun delete() = viewModelScope.launch {
        val oldCover = folder.value?.coverPath
        repository.deleteFolder(folderId)
        covers.delete(oldCover)
    }

    fun setStations(selected: Set<String>) = viewModelScope.launch {
        val current = folder.value?.stations?.mapTo(HashSet()) { it.uuid }.orEmpty()
        favorites.value.forEach { station ->
            when {
                station.uuid in selected && station.uuid !in current -> repository.addToFolder(folderId, station.uuid)
                station.uuid !in selected && station.uuid in current -> repository.removeFromFolder(folderId, station.uuid)
            }
        }
    }

    fun remove(station: Station) = viewModelScope.launch { repository.removeFromFolder(folderId, station.uuid) }

    fun setFolders(station: Station, ids: Set<Long>) = viewModelScope.launch {
        repository.setFolders(station.uuid, ids)
    }

    fun createFolder(name: String, station: Station) = viewModelScope.launch {
        val id = repository.createFolder(name)
        repository.addToFolder(id, station.uuid)
    }

    fun reorder(fromUuid: String, toUuid: String) = viewModelScope.launch {
        val stations = folder.value?.stations ?: return@launch
        val moved = stations.firstOrNull { it.uuid == fromUuid } ?: return@launch
        val remaining = stations.filterNot { it.uuid == fromUuid }
        val target = remaining.indexOfFirst { it.uuid == toUuid }
        if (target < 0) return@launch
        val ordered = remaining.take(target + 1) + moved + remaining.drop(target + 1)
        repository.reorderFolder(folderId, ordered.map { it.uuid })
    }

    fun changeCover(source: Uri, crop: IntRect) = viewModelScope.launch {
        runCatching { covers.save(folderId, source, crop) }.getOrNull()?.let { path ->
            val old = folder.value?.coverPath
            repository.setFolderCover(folderId, path)
            if (old != path) covers.delete(old)
        }
    }

    fun resetCover() = viewModelScope.launch {
        val old = folder.value?.coverPath
        repository.setFolderCover(folderId, null)
        covers.delete(old)
    }
}
