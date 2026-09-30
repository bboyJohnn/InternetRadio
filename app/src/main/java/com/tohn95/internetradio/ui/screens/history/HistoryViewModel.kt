package com.tohn95.internetradio.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tohn95.internetradio.data.StationRepository
import com.tohn95.internetradio.domain.model.HistoryEntry
import com.tohn95.internetradio.domain.model.Station
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(private val repository: StationRepository) : ViewModel() {
    val history: StateFlow<List<HistoryEntry>> = repository.history()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteUuids: StateFlow<Set<String>> = repository.favoriteUuids()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleFavorite(station: Station) = viewModelScope.launch { repository.toggleFavorite(station) }
}
