package com.tohn95.internetradio.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tohn95.internetradio.data.NetworkMonitor
import com.tohn95.internetradio.data.StationRepository
import com.tohn95.internetradio.domain.model.Station
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Состояние уровня каркаса: избранное для ♡ в мини-плеере. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: StationRepository,
    network: NetworkMonitor,
) : ViewModel() {
    /** Есть ли интернет — для плашки «Нет интернета» и понятных ошибок. */
    val online: StateFlow<Boolean> = network.online

    val favoriteUuids: StateFlow<Set<String>> = repository.favoriteUuids()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleFavorite(station: Station) = viewModelScope.launch { repository.toggleFavorite(station) }
}
