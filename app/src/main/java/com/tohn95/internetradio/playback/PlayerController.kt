package com.tohn95.internetradio.playback

import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.tohn95.internetradio.R
import com.tohn95.internetradio.data.StationRepository
import com.tohn95.internetradio.util.withAppLocale
import com.tohn95.internetradio.data.remote.RadioBrowserClient
import com.tohn95.internetradio.domain.model.Station
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StationRepository,
    private val api: RadioBrowserClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentStation = MutableStateFlow<Station?>(null)
    val currentStation: StateFlow<Station?> = _currentStation
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    /**
     * Пользователь хочет, чтобы играло (playWhenReady): true и во время подгрузки/переподключения.
     * По нему рисуем кнопку ⏯ — иначе, пока поток грузится, горел бы ▶, а нажатие ставило бы паузу.
     */
    private val _playRequested = MutableStateFlow(false)
    val playRequested: StateFlow<Boolean> = _playRequested
    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering
    private val _streamAlive = MutableStateFlow(true)
    val streamAlive: StateFlow<Boolean> = _streamAlive

    private val tracker = MetadataTracker(scope)
    val metadata: StateFlow<MetadataState> = tracker.state

    // Повтор — с «живого» края: после обрыва или конца потока догонять старый буфер незачем.
    private val reconnect = ReconnectScheduler(scope) {
        controller?.let { c -> c.seekToDefaultPosition(); c.prepare(); c.play() }
    }

    private val sleepTimer = SleepTimer(
        scope,
        setVolume = { v -> controller?.volume = v },
        onExpired = { controller?.pause() },
    )
    val sleepRemainingMs: StateFlow<Long?> = sleepTimer.remainingMs

    fun startSleepTimer(minutes: Int) = sleepTimer.start(minutes * 60_000L)
    fun cancelSleepTimer() = sleepTimer.cancel()

    private var controller: MediaController? = null
    private var connecting: ListenableFuture<MediaController>? = null
    private var pendingAction: ((MediaController) -> Unit)? = null

    /**
     * Выполнить действие с плеером сервиса. Подключаемся по первому требованию: сервис с ExoPlayer
     * не создаётся на старте приложения (быстрее запуск), а нажатие станции сразу после запуска
     * не теряется — ждёт подключения. Если сервис пропал, подключаемся заново.
     */
    private fun withController(action: (MediaController) -> Unit) {
        controller?.takeIf { it.isConnected }?.let { action(it); return }
        pendingAction = action              // из нескольких нажатий до подключения важно последнее
        if (connecting != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    if (this@PlayerController.controller === controller) this@PlayerController.controller = null
                    _isPlaying.value = false
                    _playRequested.value = false
                }
            })
            .buildAsync()
        connecting = future
        future.addListener({
            connecting = null
            val c = runCatching { future.get() }.getOrNull()
            if (c == null) { pendingAction = null; return@addListener }
            c.addListener(listener)
            controller = c
            pendingAction?.let { pendingAction = null; it(c) }
        }, ContextCompat.getMainExecutor(context))
    }

    init {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    if (_currentStation.value != null && !_streamAlive.value) reconnect.onNetworkAvailable()
                }
            }
        })
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _playRequested.value = playWhenReady
            // Пауза (кнопкой, из уведомления, наушниками, таймером сна, выдернули наушники) —
            // отменяем запланированное переподключение, иначе оно само включило бы звук.
            if (!playWhenReady) { reconnect.reset(); _streamAlive.value = true }
        }

        override fun onPlaybackStateChanged(state: Int) {
            _isBuffering.value = state == Player.STATE_BUFFERING
            if (state == Player.STATE_BUFFERING) tracker.onBuffering()
            if (state == Player.STATE_READY) { _streamAlive.value = true; reconnect.reset() }
            // У эфира нет конца: сервер закрыл соединение (перезапуск, «медленный клиент» после долгой
            // паузы) — раньше радио просто замолкало. Переподключаемся, как при обрыве.
            if (state == Player.STATE_ENDED && _currentStation.value != null) {
                _streamAlive.value = false
                reconnect.onError()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _streamAlive.value = false
            if (error.isHopeless()) reconnect.giveUp() else reconnect.onError()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            tracker.onIcyTitle(mediaMetadata.title?.toString())
        }
    }

    fun play(station: Station) = withController { c ->
        _currentStation.value = station
        _streamAlive.value = true
        reconnect.reset()
        tracker.onStreamStarted()
        val item = MediaItem.Builder()
            .setUri(station.streamUrl.toUri())
            .setMediaId(station.uuid)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.name)
                    .setStation(station.name)
                    // Вторая строка уведомления, пока станция не прислала трек: «В эфире · Германия · news».
                    .setSubtitle(liveLine(station))
                    .setGenre(station.tags.firstOrNull())
                    .setArtworkUri(station.faviconUrl?.takeIf { it.isNotBlank() }?.toUri())
                    .build()
            )
            .build()
        c.setMediaItem(item)
        c.prepare()
        c.play()
        scope.launch(Dispatchers.IO) {
            repository.recordPlayed(station)
            if (!station.isCustom) api.trackClick(station.uuid)
        }
    }

    /** Избранное для ⏮ ⏭ в уведомлении/на наушниках: станции листаются по кругу в порядке списка. */
    private val favorites: StateFlow<List<Station>> = repository.favorites()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Переключить станцию по Избранному: +1 вперёд, -1 назад. Не из Избранного — к первой/последней. */
    fun skip(direction: Int) {
        val list = favorites.value
        if (list.isEmpty()) return
        val idx = list.indexOfFirst { it.uuid == _currentStation.value?.uuid }
        val next = if (idx < 0) (if (direction > 0) list.first() else list.last())
        else list[(idx + direction).mod(list.size)]
        if (next.uuid != _currentStation.value?.uuid) play(next)
    }

    private fun liveLine(station: Station): String = listOfNotNull(
        context.withAppLocale().getString(R.string.notif_live),
        station.countryCode.takeIf { it.length == 2 }
            ?.let { Locale("", it.uppercase()).getDisplayCountry(Locale.getDefault()) }
            ?.takeIf { it.isNotBlank() } ?: station.country.takeIf { it.isNotBlank() },
        station.tags.firstOrNull(),
    ).joinToString(" · ")

    /** Станция не играет и попытки кончились (или ошибка безнадёжная) — показать «не воспроизводится». */
    val playbackFailed: StateFlow<Boolean> = reconnect.gaveUp

    /**
     * Пользователь выбрал логотип для станции: обновляем играющую сейчас (плеер, мини-плеер) и обложку
     * уведомления — через replaceMediaItem с тем же адресом (ExoPlayer меняет только метаданные, без перезапуска).
     */
    fun updateLogo(uuid: String, url: String) {
        val s = _currentStation.value?.takeIf { it.uuid == uuid } ?: return
        _currentStation.value = s.copy(faviconUrl = url)
        val c = controller ?: return
        val item = c.currentMediaItem?.takeIf { it.mediaId == uuid } ?: return
        c.replaceMediaItem(
            c.currentMediaItemIndex,
            item.buildUpon().setMediaMetadata(item.mediaMetadata.buildUpon().setArtworkUri(url.toUri()).build()).build(),
        )
    }

    /** «Повторить» из плеера: заново подключиться к потоку текущей станции. */
    fun retry() {
        val s = _currentStation.value ?: return
        val c = controller?.takeIf { it.isConnected && it.mediaItemCount > 0 } ?: return play(s)
        reconnect.reset()
        _streamAlive.value = true
        c.seekToDefaultPosition()
        c.prepare()
        c.play()
    }

    /**
     * Повторы бессмысленны: формат/кодек не поддерживается, поток не разбирается, адрес отвечает 4xx.
     * Сетевые сбои и ответы 5xx — временные, их переподключаем.
     */
    private fun PlaybackException.isHopeless(): Boolean {
        val http = (cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode
        return errorCode in 3000..4999 ||
            errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
            (errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS && http != null && http in 400..499)
    }

    /**
     * ⏯ по намерению (playWhenReady), а не по «звучит ли сейчас»: во время подгрузки или
     * переподключения нажатие ставит паузу, а не запускает поток повторно.
     * Сервис пропал (плеер пуст) — запускаем текущую станцию заново.
     */
    fun togglePlayPause() {
        val s = _currentStation.value ?: return
        val c = controller?.takeIf { it.isConnected && it.mediaItemCount > 0 } ?: return play(s)
        if (c.playWhenReady) c.pause() else { c.prepare(); c.play() }
    }

    fun stop() {
        controller?.stop()
        _currentStation.value = null
        reconnect.reset()
    }
}
