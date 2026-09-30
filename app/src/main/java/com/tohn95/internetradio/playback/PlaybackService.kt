package com.tohn95.internetradio.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.tohn95.internetradio.MainActivity
import com.tohn95.internetradio.R
import com.tohn95.internetradio.data.StationRepository
import com.tohn95.internetradio.util.withAppLocale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Фоновое воспроизведение + медиасессия. Уведомление/системный плеер в стиле приложения (как у Spotify):
 * фирменная обложка ([BrandedArtworkLoader]), «трек / станция» без дублей и ⏮ ⏭ по Избранному
 * ([StationPlayer]), ♡ «Мне нравится» и наш значок-наушники.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var repository: StationRepository
    @Inject lateinit var playerController: PlayerController

    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val currentUuid = MutableStateFlow<String?>(null)
    private val favoriteCommand = SessionCommand(ACTION_FAVORITE, Bundle.EMPTY)

    override fun onCreate() {
        super.onCreate()
        // Потоки часто переезжают с http на https (и обратно) и отвечают 301/302. По умолчанию ExoPlayer
        // такие переходы не проходит — станции не играли вовсе («Source error: 301») и зря переподключались.
        val http = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(this, http)))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            // Выдернули наушники / отключился Bluetooth — пауза, а не громко в динамик.
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        val player = StationPlayer(exo).apply { skipHandler = { dir -> playerController.skip(dir) } }
        exo.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentUuid.value = mediaItem?.mediaId
                player.icyTitle = null            // новая станция — старый трек не показываем
            }

            // ICY StreamTitle приходит сюда «сырым» — только так трек не теряется (см. StationPlayer.icyTitle).
            override fun onMetadata(metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    (metadata.get(i) as? IcyInfo)?.let { player.icyTitle = it.title }
                }
            }
        })

        val sessionIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.notif_channel)
                .build()
                .apply { setSmallIcon(R.drawable.ic_stat_radio) },
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionIntent)
            .setBitmapLoader(BrandedArtworkLoader(this))
            .setCallback(sessionCallback)
            .setMediaButtonPreferences(listOf(heartButton(false)))
            .build()

        // ♡ в уведомлении отражает избранное (в т.ч. если сердечко нажали в приложении),
        // а ⏮ ⏭ доступны, только когда в Избранном есть куда переключаться.
        scope.launch {
            combine(currentUuid, repository.favoriteUuids()) { uuid, favs -> uuid to favs }
                .collect { (uuid, favs) ->
                    val isFav = uuid != null && uuid in favs
                    mediaSession?.setMediaButtonPreferences(listOf(heartButton(isFav)))
                    player.canSkip = favs.size >= 2 || (favs.isNotEmpty() && !isFav)
                }
        }
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult =
            MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().add(favoriteCommand).build(),
                )
                .build()

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_FAVORITE) {
                currentUuid.value?.let { uuid -> scope.launch { repository.toggleFavoriteByUuid(uuid) } }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    private fun heartButton(isFavorite: Boolean): CommandButton =
        CommandButton.Builder(if (isFavorite) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED)
            .setDisplayName(withAppLocale().getString(if (isFavorite) R.string.info_remove_favorite else R.string.info_add_favorite))
            .setSessionCommand(favoriteCommand)
            .build()

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.run { player.release(); release() }
        mediaSession = null
        super.onDestroy()
    }

    private companion object {
        const val ACTION_FAVORITE = "com.tohn95.internetradio.FAVORITE"
    }
}
