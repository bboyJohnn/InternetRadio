package com.tohn95.internetradio.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tohn95.internetradio.ui.theme.ThemeMode
import com.tohn95.internetradio.ui.theme.ThemeRepository
import com.tohn95.internetradio.ui.theme.ThemeSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: ThemeRepository,
) : ViewModel() {
    val settings: StateFlow<ThemeSettings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeSettings())

    fun setHue(v: Int) = viewModelScope.launch { repo.setHue(v) }
    fun setSaturation(v: Int) = viewModelScope.launch { repo.setSaturation(v) }
    fun setMode(m: ThemeMode) = viewModelScope.launch { repo.setMode(m) }
    fun setWaves(b: Boolean) = viewModelScope.launch { repo.setWavesAnimated(b) }
    fun setTrueBlack(b: Boolean) = viewModelScope.launch { repo.setTrueBlack(b) }
    fun setDynamicColor(b: Boolean) = viewModelScope.launch { repo.setDynamicColor(b) }
}
