package com.tohn95.internetradio.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tohn95.internetradio.data.StationRepository
import com.tohn95.internetradio.data.VoteOutcome
import com.tohn95.internetradio.data.remote.LogoFinder
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.playback.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

/** Короткое сообщение-«пилюля» поверх плеера (голос, копирование и т.п.). */
enum class PlayerMessage { VOTE_OK, VOTE_ALREADY, VOTE_ERROR, COPIED, LOGO_SET }

/** Поиск логотипа на сайте станции: null — окно закрыто. */
data class LogoSearch(val loading: Boolean = true, val results: List<String> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlayerViewModel @Inject constructor(
    val player: PlayerController,
    private val repository: StationRepository,
    private val logoFinder: LogoFinder,
) : ViewModel() {
    private val _logoSearch = MutableStateFlow<LogoSearch?>(null)
    val logoSearch: StateFlow<LogoSearch?> = _logoSearch

    /** «Найти логотип»: собираем картинки с сайта станции. */
    fun findLogo() {
        val hp = player.currentStation.value?.homepage ?: return
        _logoSearch.value = LogoSearch(loading = true)
        viewModelScope.launch {
            val found = runCatching { logoFinder.find(hp) }.getOrDefault(emptyList())
            if (_logoSearch.value != null) _logoSearch.value = LogoSearch(loading = false, results = found)
        }
    }

    fun closeLogoSearch() { _logoSearch.value = null }

    /** Выбрали картинку — запоминаем для станции и сразу показываем везде. */
    fun pickLogo(url: String) {
        val s = player.currentStation.value ?: return
        repository.setLogo(s.uuid, url)
        player.updateLogo(s.uuid, url)
        _logoSearch.value = null
        _messages.tryEmit(PlayerMessage.LOGO_SET)
    }

    val isFavorite: StateFlow<Boolean> = player.currentStation
        .flatMapLatest { s -> if (s == null) flowOf(false) else repository.isFavorite(s.uuid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Голосовали ли за текущую станцию в этом запуске — палец 👍 закрашен. */
    val hasVoted: StateFlow<Boolean> = combine(player.currentStation, repository.votedUuids) { s, voted ->
        s != null && s.uuid in voted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Остаток таймера сна в минутах (вверх): экран обновляется раз в минуту, а не каждую секунду. */
    val sleepMinutes: StateFlow<Int?> = player.sleepRemainingMs
        .map { ms -> ms?.let { ceil(it / 60_000.0).toInt() } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _messages = MutableSharedFlow<PlayerMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<PlayerMessage> = _messages

    fun toggleFavorite() {
        val s: Station = player.currentStation.value ?: return
        viewModelScope.launch { repository.toggleFavorite(s) }
    }

    fun vote() {
        val s = player.currentStation.value ?: return
        if (s.isCustom) return
        viewModelScope.launch {
            _messages.emit(
                when (repository.vote(s.uuid)) {
                    VoteOutcome.OK -> PlayerMessage.VOTE_OK
                    VoteOutcome.ALREADY -> PlayerMessage.VOTE_ALREADY
                    VoteOutcome.ERROR -> PlayerMessage.VOTE_ERROR
                }
            )
        }
    }

    fun notify(message: PlayerMessage) {
        _messages.tryEmit(message)
    }
}
