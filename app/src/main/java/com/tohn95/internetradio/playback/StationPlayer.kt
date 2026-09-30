package com.tohn95.internetradio.playback

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * Обёртка плеера для медиасессии (уведомление, экран блокировки, наушники, машина):
 *  1) Текст как у Spotify: трек известен — крупно трек, ниже станция; нет — крупно станция,
 *     ниже «В эфире · страна · жанр». Раньше ExoPlayer подставлял ICY-заголовок поверх, и часто
 *     выходило «станция / станция».
 *  2) ⏮ ⏭ переключают станции из Избранного (через [skipHandler]) — у радио нет плейлиста,
 *     а кнопки на наушниках и в машине так начинают работать.
 */
@OptIn(UnstableApi::class)
class StationPlayer(player: Player) : ForwardingPlayer(player) {
    /** Переключить станцию: +1 вперёд, -1 назад. */
    var skipHandler: ((Int) -> Unit)? = null

    /** Можно ли переключать (есть куда). Смена — сразу сообщаем сессии, чтобы кнопки появились/исчезли. */
    var canSkip: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            val commands = availableCommands
            wrappers.values.toList().forEach { it.onAvailableCommandsChanged(commands) }
        }

    /**
     * Текущий трек из ICY-заголовка потока (StreamTitle). Берём из «сырых» метаданных ([IcyInfo]):
     * в собранных ExoPlayer метаданных название нашего MediaItem (станция) ПЕРЕКРЫВАЕТ ICY-заголовок,
     * и трек до сессии и приложения не доходил. Смена — сразу рассылаем обновлённые метаданные.
     */
    var icyTitle: String? = null
        set(value) {
            val v = value?.trim()?.takeIf { it.isNotEmpty() }
            if (field == v) return
            field = v
            val raw = super.getMediaMetadata()
            wrappers.values.toList().forEach { it.onMediaMetadataChanged(raw) }
        }

    private val wrappers = LinkedHashMap<Player.Listener, Player.Listener>()

    override fun addListener(listener: Player.Listener) {
        val wrapper = DisplayListener(listener)
        wrappers[listener] = wrapper
        super.addListener(wrapper)
    }

    override fun removeListener(listener: Player.Listener) {
        wrappers.remove(listener)?.let { super.removeListener(it) }
    }

    override fun getMediaMetadata(): MediaMetadata = display(super.getMediaMetadata())

    override fun getAvailableCommands(): Player.Commands = withSkip(super.getAvailableCommands())

    override fun isCommandAvailable(command: Int): Boolean =
        (canSkip && command in SKIP) || super.isCommandAvailable(command)

    override fun hasNextMediaItem(): Boolean = canSkip || super.hasNextMediaItem()
    override fun hasPreviousMediaItem(): Boolean = canSkip || super.hasPreviousMediaItem()
    override fun seekToNext() = skip(+1)
    override fun seekToNextMediaItem() = skip(+1)
    override fun seekToPrevious() = skip(-1)
    override fun seekToPreviousMediaItem() = skip(-1)

    private fun skip(direction: Int) {
        if (canSkip) skipHandler?.invoke(direction)
    }

    /**
     * Долгая пауза → продолжаем с «живого» края эфира, а не со старого буфера: иначе после паузы
     * звучало то, что шло минуту назад, а потом сервер, закрывший соединение, обрывал звук.
     * Короткая пауза — продолжаем с того же места. Работает для всех кнопок: приложение,
     * уведомление, наушники, машина.
     */
    private var pausedAt = 0L
    private var pausedStation: String? = null

    override fun pause() {
        markPaused()
        super.pause()
    }

    override fun play() {
        goLiveIfStale()
        super.play()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) goLiveIfStale() else markPaused()
        super.setPlayWhenReady(playWhenReady)
    }

    private fun markPaused() {
        pausedAt = SystemClock.elapsedRealtime()
        pausedStation = currentMediaItem?.mediaId
    }

    /** Только для той же станции: новую и так включают с живого края — повторный переход лишь переоткрыл бы поток. */
    private fun goLiveIfStale() {
        val at = pausedAt
        val sameStation = pausedStation != null && pausedStation == currentMediaItem?.mediaId
        pausedAt = 0L
        pausedStation = null
        if (at != 0L && sameStation && SystemClock.elapsedRealtime() - at > LIVE_AFTER_PAUSE_MS &&
            playbackState != Player.STATE_IDLE
        ) super.seekToDefaultPosition()
    }

    private fun withSkip(commands: Player.Commands): Player.Commands =
        if (!canSkip) commands else commands.buildUpon().addAll(*SKIP).build()

    /** Метаданные для показа: [merged] — то, что собрал ExoPlayer (ICY-заголовок поверх нашего). */
    private fun display(merged: MediaMetadata): MediaMetadata {
        val item = currentMediaItem?.mediaMetadata ?: return merged
        val station = item.title?.toString()?.trim().orEmpty()
        if (station.isEmpty()) return merged
        val track = icyTitle?.takeIf { !it.equals(station, ignoreCase = true) }
        val live = item.subtitle?.toString() ?: station
        val title = track ?: station
        val artist = if (track != null) station else live
        return merged.buildUpon()
            .setTitle(title).setDisplayTitle(title)
            .setArtist(artist).setSubtitle(artist)
            .build()
    }

    /**
     * Слушатель сессии, которому отдаём уже «показные» метаданные и команды с ⏮ ⏭.
     * ВНИМАНИЕ: пересылка явная, метод за методом. Делегирование Kotlin (`by d`) НЕ пересылает
     * default-методы Java-интерфейса — сессия переставала узнавать о старте воспроизведения,
     * висела в «буферизации», и уведомление не появлялось.
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    private inner class DisplayListener(private val d: Player.Listener) : Player.Listener {
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = d.onMediaMetadataChanged(display(mediaMetadata))
        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) =
            d.onAvailableCommandsChanged(withSkip(availableCommands))
        override fun onEvents(player: Player, events: Player.Events) = d.onEvents(this@StationPlayer, events)

        override fun onTimelineChanged(timeline: Timeline, reason: Int) = d.onTimelineChanged(timeline, reason)
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = d.onMediaItemTransition(mediaItem, reason)
        override fun onTracksChanged(tracks: Tracks) = d.onTracksChanged(tracks)
        override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) = d.onPlaylistMetadataChanged(mediaMetadata)
        override fun onIsLoadingChanged(isLoading: Boolean) = d.onIsLoadingChanged(isLoading)
        override fun onLoadingChanged(isLoading: Boolean) = d.onLoadingChanged(isLoading)
        override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) =
            d.onTrackSelectionParametersChanged(parameters)
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) =
            d.onPlayerStateChanged(playWhenReady, playbackState)
        override fun onPlaybackStateChanged(playbackState: Int) = d.onPlaybackStateChanged(playbackState)
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = d.onPlayWhenReadyChanged(playWhenReady, reason)
        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) =
            d.onPlaybackSuppressionReasonChanged(playbackSuppressionReason)
        override fun onIsPlayingChanged(isPlaying: Boolean) = d.onIsPlayingChanged(isPlaying)
        override fun onRepeatModeChanged(repeatMode: Int) = d.onRepeatModeChanged(repeatMode)
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = d.onShuffleModeEnabledChanged(shuffleModeEnabled)
        override fun onPlayerError(error: PlaybackException) = d.onPlayerError(error)
        override fun onPlayerErrorChanged(error: PlaybackException?) = d.onPlayerErrorChanged(error)
        override fun onPositionDiscontinuity(reason: Int) = d.onPositionDiscontinuity(reason)
        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) =
            d.onPositionDiscontinuity(oldPosition, newPosition, reason)
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) =
            d.onPlaybackParametersChanged(playbackParameters)
        override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) = d.onSeekBackIncrementChanged(seekBackIncrementMs)
        override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) = d.onSeekForwardIncrementChanged(seekForwardIncrementMs)
        override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) =
            d.onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs)
        override fun onAudioSessionIdChanged(audioSessionId: Int) = d.onAudioSessionIdChanged(audioSessionId)
        override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) = d.onAudioAttributesChanged(audioAttributes)
        override fun onVolumeChanged(volume: Float) = d.onVolumeChanged(volume)
        override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) = d.onSkipSilenceEnabledChanged(skipSilenceEnabled)
        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) = d.onDeviceInfoChanged(deviceInfo)
        override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) = d.onDeviceVolumeChanged(volume, muted)
        override fun onVideoSizeChanged(videoSize: VideoSize) = d.onVideoSizeChanged(videoSize)
        override fun onSurfaceSizeChanged(width: Int, height: Int) = d.onSurfaceSizeChanged(width, height)
        override fun onRenderedFirstFrame() = d.onRenderedFirstFrame()
        override fun onCues(cues: List<Cue>) = d.onCues(cues)
        override fun onCues(cueGroup: CueGroup) = d.onCues(cueGroup)
        override fun onMetadata(metadata: Metadata) = d.onMetadata(metadata)
    }

    private companion object {
        val SKIP = intArrayOf(
            Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        )

        // Пауза дольше — продолжаем с живого края эфира.
        const val LIVE_AFTER_PAUSE_MS = 30_000L
    }
}
