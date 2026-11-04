package com.tohn95.internetradio.ui.screens.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.tohn95.internetradio.R
import com.tohn95.internetradio.util.countryLabel
import com.tohn95.internetradio.playback.MetadataState
import com.tohn95.internetradio.ui.components.LiveDot
import com.tohn95.internetradio.ui.components.LocalOnline
import com.tohn95.internetradio.ui.components.VinylCover
import com.tohn95.internetradio.ui.components.codecLabel
import com.tohn95.internetradio.ui.components.formatShort
import com.tohn95.internetradio.ui.components.marquee
import com.tohn95.internetradio.ui.components.rememberDominantColor
import com.tohn95.internetradio.ui.components.swipeToSkip
import com.tohn95.internetradio.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Экран станции по референсам (Spotify-плеер + круглый плеер с дугой громкости):
 * фон подкрашен цветом логотипа, винил в дуге громкости, «волна эфира», ряд 🌙 ⏮ ⏯ ⏭ 👍, ♡ у названия.
 * Сверху — откуда играет станция; ⏮ ⏭ и свайп по винилу листают тот же список. Свайп вниз — закрыть.
 * Ниже листается полная информация о станции — её верх выглядывает под плеером.
 */
@Composable
fun PlayerScreen(
    onClose: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val station by viewModel.player.currentStation.collectAsStateWithLifecycle()
    val isPlaying by viewModel.player.isPlaying.collectAsStateWithLifecycle()
    val playRequested by viewModel.player.playRequested.collectAsStateWithLifecycle()
    val streamAlive by viewModel.player.streamAlive.collectAsStateWithLifecycle()
    val metadata by viewModel.player.metadata.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val hasVoted by viewModel.hasVoted.collectAsStateWithLifecycle()
    val logoSearch by viewModel.logoSearch.collectAsStateWithLifecycle()
    val playbackFailed by viewModel.player.playbackFailed.collectAsStateWithLifecycle()
    val online = LocalOnline.current
    val sleepMinutes by viewModel.sleepMinutes.collectAsStateWithLifecycle()
    val source by viewModel.player.currentSource.collectAsStateWithLifecycle()
    val canSkip by viewModel.player.canSkip.collectAsStateWithLifecycle()
    var sleepDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVol.toFloat()) }
    // Громкость могли поменять кнопками телефона — подтягиваем, только пока экран на виду (свёрнутое
    // приложение не будим). Если системный шаг тот же, что под пальцем, не трогаем — ползунок не дёргается.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val step = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (step != (volume * maxVol).roundToInt()) volume = step / maxVol.toFloat()
                delay(500)
            }
        }
    }

    LaunchedEffect(station) { if (station == null) onClose() }
    val s = station ?: return

    val accent by animateColorAsState(rememberDominantColor(s.faviconUrl) ?: palette.primary, label = "accent")
    val onAccent = if (accent.luminance() > 0.55f) Color.Black else Color.White
    val bgTop by animateColorAsState(lerp(palette.pageBg, accent, if (palette.isDark) 0.42f else 0.30f), label = "bgTop")
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val share: () -> Unit = {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${s.name}\n${s.homepage ?: s.streamUrl}")
        }
        runCatching { context.startActivity(Intent.createChooser(intent, null)) }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(palette.pageBg)) {
        // Первый «экран» — плеер; снизу остаётся полоса InfoPeekHeight под «язык» карточки информации.
        // Нижней навигации и мини-плеера здесь нет — весь экран отдан плееру.
        val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val pageHeight = maxHeight - statusBar - navBar - InfoPeekHeight
        val pagePx = with(LocalDensity.current) { pageHeight.toPx() }
        // Винил — что останется от высоты после остальных блоков (~430dp), но не меньше 150 и не больше 320.
        val ringSize = min(min(maxWidth - 40.dp, 320.dp), max(150.dp, pageHeight - 430.dp))
        val toInfo: () -> Unit = { scope.launch { scroll.animateScrollTo(pagePx.roundToInt()) } }

        Box(Modifier.fillMaxWidth().height(maxHeight * 0.8f).background(Brush.verticalGradient(listOf(bgTop, palette.pageBg))))
        Column(
            Modifier.fillMaxSize().statusBarsPadding()
                .pullDownToClose(scroll, onClose)
                .verticalScroll(scroll),
        ) {
            Column(
                Modifier.fillMaxWidth().height(pageHeight).padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val sub = listOfNotNull(s.countryLabel().takeIf { it.isNotBlank() }, s.tags.firstOrNull()).joinToString(" · ")
                PlayerHeader(
                    source = source, fallback = sub, onClose = onClose, onShare = share,
                    onFindLogo = { viewModel.findLogo() }, onInfo = toInfo,
                )

                Spacer(Modifier.height(16.dp))
                Spacer(Modifier.weight(0.6f))
                // Смахнуть винил — соседняя станция; у самой дуги палец крутит громкость (дуга забирает жест себе).
                ArcVolumeRing(
                    volume = { volume },
                    onVolumeChange = { v ->
                        volume = v
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (v * maxVol).roundToInt(), 0)
                    },
                    accent = accent, track = palette.text.copy(alpha = 0.15f), iconTint = palette.textMuted,
                    size = ringSize,
                    modifier = Modifier.swipeToSkip(canSkip) { viewModel.player.skip(it) },
                ) {
                    VinylCover(faviconUrl = s.faviconUrl, isPlaying = isPlaying, modifier = Modifier.size(ringSize - 60.dp))
                }
                Spacer(Modifier.height(28.dp))
                Spacer(Modifier.weight(0.4f))

                // Название и трек слева, ♡ справа — как ✓ у Spotify.
                // Трек = то, что станция прислала в ICY, если это не её же название.
                val track = (metadata as? MetadataState.Available)?.text?.trim()
                    ?.takeIf { it.isNotEmpty() && !it.equals(s.name.trim(), ignoreCase = true) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.name, color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, modifier = Modifier.marquee(),
                        )
                        val metaText = track ?: when (metadata) {
                            is MetadataState.Detecting -> stringResource(R.string.detecting)
                            else -> stringResource(R.string.no_metadata)
                        }
                        Text(
                            metaText, color = palette.textMuted, fontSize = 15.sp, maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp).marquee(),
                        )
                    }
                    IconButton(onClick = { viewModel.toggleFavorite() }, modifier = Modifier.padding(start = 8.dp).size(48.dp)) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            stringResource(if (isFavorite) R.string.info_remove_favorite else R.string.info_add_favorite),
                            tint = if (isFavorite) palette.red else palette.text, modifier = Modifier.size(28.dp),
                        )
                    }
                }
                // Слушатели · формат · «В эфире»; справа — остаток таймера сна.
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Headphones, null, tint = palette.textMuted, modifier = Modifier.size(14.dp))
                    val info = listOfNotNull(
                        stringResource(R.string.listeners_short, formatShort(s.clickCount)),
                        codecLabel(s),
                    ).joinToString(" · ")
                    Text(info, color = palette.textMuted, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(start = 4.dp, end = 10.dp))
                    if (streamAlive && online) LiveDot(alive = streamAlive, playing = isPlaying)
                    Spacer(Modifier.weight(1f))
                    sleepMinutes?.let { min ->
                        Icon(Icons.Filled.Bedtime, null, tint = accent, modifier = Modifier.size(14.dp))
                        Text(
                            stringResource(R.string.sleep_timer_remaining, min),
                            color = accent, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
                // Трек известен — «найти песню» в музыкальных сервисах и «скопировать».
                if (track != null) {
                    TrackActions(
                        track = track, accent = accent,
                        onCopied = { viewModel.notify(PlayerMessage.COPIED) },
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }

                LiveWaveform(
                    playing = isPlaying && streamAlive, accent = accent, muted = palette.textMuted.copy(alpha = 0.35f),
                    modifier = Modifier.padding(top = 18.dp),
                )
                // Поток не играет — понятное объяснение вместо «В эфире».
                if (!streamAlive || !online) {
                    StreamIssue(
                        failed = playbackFailed, online = online, accent = accent,
                        onRetry = { viewModel.player.retry() }, modifier = Modifier.padding(top = 6.dp),
                    )
                }

                Spacer(Modifier.height(20.dp))
                Spacer(Modifier.weight(0.5f))
                // 🌙 · ⏮ · большой ⏯ · ⏭ · 👍. Листать некуда — ⏮ ⏭ приглушены и не нажимаются.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { sleepDialog = true }, modifier = Modifier.size(52.dp)) {
                        Icon(
                            Icons.Filled.Bedtime, stringResource(R.string.sleep_timer_title),
                            tint = if (sleepMinutes != null) accent else palette.text, modifier = Modifier.size(26.dp),
                        )
                    }
                    IconButton(onClick = { viewModel.player.skip(-1) }, enabled = canSkip, modifier = Modifier.size(52.dp)) {
                        Icon(
                            Icons.Filled.SkipPrevious, stringResource(R.string.station_prev),
                            tint = palette.text.copy(alpha = if (canSkip) 1f else 0.35f), modifier = Modifier.size(34.dp),
                        )
                    }
                    Box(
                        Modifier.size(76.dp)
                            .shadow(18.dp, CircleShape, ambientColor = accent, spotColor = accent)
                            .clip(CircleShape).background(accent)
                            .clickable { viewModel.player.togglePlayPause() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (playRequested) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            stringResource(R.string.play), tint = onAccent, modifier = Modifier.size(40.dp),
                        )
                    }
                    IconButton(onClick = { viewModel.player.skip(+1) }, enabled = canSkip, modifier = Modifier.size(52.dp)) {
                        Icon(
                            Icons.Filled.SkipNext, stringResource(R.string.station_next),
                            tint = palette.text.copy(alpha = if (canSkip) 1f else 0.35f), modifier = Modifier.size(34.dp),
                        )
                    }
                    // Голос за станцию в каталоге radio-browser. У своих станций по ссылке голосовать некуда —
                    // кнопка остаётся на месте (ряд не перекашивается), но приглушена и не нажимается.
                    IconButton(onClick = { viewModel.vote() }, enabled = !s.isCustom, modifier = Modifier.size(52.dp)) {
                        Icon(
                            if (hasVoted) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            stringResource(R.string.player_vote),
                            tint = when {
                                s.isCustom -> palette.text.copy(alpha = 0.3f)
                                hasVoted -> accent
                                else -> palette.text
                            },
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Spacer(Modifier.weight(0.4f))
            }

            StationInfoSection(
                s, accent = accent, onAccent = onAccent, modifier = Modifier.padding(horizontal = 12.dp),
                onFindLogo = { viewModel.findLogo() },
            )
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }

        logoSearch?.let { search ->
            LogoPickerDialog(
                search, site = s.homepage?.substringAfter("//")?.substringBefore("/").orEmpty(),
                onPick = { viewModel.pickLogo(it) }, onDismiss = { viewModel.closeLogoSearch() },
            )
        }

        // Короткое сообщение-«пилюля» в цвет станции (голос, копирование): само прячется через 2,5 с.
        var message by remember { mutableStateOf<PlayerMessage?>(null) }
        LaunchedEffect(Unit) {
            viewModel.messages.collect { m ->
                message = m
                delay(2500)
                if (message == m) message = null
            }
        }
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp),
        ) {
            val m = message ?: PlayerMessage.COPIED
            MessagePill(m, accent = accent, onAccent = onAccent)
        }
    }

    if (sleepDialog) {
        AlertDialog(
            onDismissRequest = { sleepDialog = false },
            title = { Text(stringResource(R.string.sleep_timer_title)) },
            text = {
                Column {
                    listOf(1, 15, 30, 45, 60).forEach { min ->
                        TextButton(onClick = {
                            viewModel.player.startSleepTimer(min)
                            sleepDialog = false
                        }) { Text(stringResource(R.string.sleep_timer_minutes, min)) }
                    }
                    TextButton(onClick = {
                        viewModel.player.cancelSleepTimer()
                        sleepDialog = false
                    }) { Text(stringResource(R.string.sleep_timer_off), color = palette.red) }
                }
            },
            confirmButton = {},
        )
    }
}
