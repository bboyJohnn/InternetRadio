package com.tohn95.internetradio.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tohn95.internetradio.data.StationRepository
import com.tohn95.internetradio.domain.model.Folder
import com.tohn95.internetradio.domain.model.Station
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(private val repository: StationRepository) : ViewModel() {
    val favorites: StateFlow<List<Station>> = repository.favorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = repository.folders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Создать папку; если [thenAdd] задан — сразу положить в неё эту станцию. */
    fun createFolder(name: String, thenAdd: Station? = null) = viewModelScope.launch {
        val id = repository.createFolder(name)
        thenAdd?.let { repository.addToFolder(id, it.uuid) }
    }

    fun renameFolder(id: Long, name: String) = viewModelScope.launch { repository.renameFolder(id, name) }
    fun deleteFolder(id: Long) = viewModelScope.launch { repository.deleteFolder(id) }
    fun removeFromFolder(folderId: Long, station: Station) =
        viewModelScope.launch { repository.removeFromFolder(folderId, station.uuid) }
    fun setFolders(station: Station, folderIds: Set<Long>) =
        viewModelScope.launch { repository.setFolders(station.uuid, folderIds) }
    fun removeFavorite(station: Station) = viewModelScope.launch { repository.toggleFavorite(station) }

    /** Своя станция по ссылке — сразу в Избранное. */
    fun addCustomStation(station: Station) = viewModelScope.launch { repository.addCustomStation(station) }
}
