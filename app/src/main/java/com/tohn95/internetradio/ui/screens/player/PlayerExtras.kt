package com.tohn95.internetradio.ui.screens.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.tohn95.internetradio.R
import com.tohn95.internetradio.ui.theme.LocalPalette
import java.net.URLEncoder

/**
 * «Найти песню» (как у radioMii): ICY-строка трека уходит поиском в Spotify / YouTube Music
 * (откроется приложение, если стоит, иначе сайт) + «скопировать название».
 */
@Composable
fun TrackActions(track: String, accent: Color, onCopied: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val query = remember(track) { URLEncoder.encode(track, "UTF-8").replace("+", "%20") }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionPill(Icons.Filled.MusicNote, "Spotify", accent) {
            open(context, "https://open.spotify.com/search/$query")
        }
        ActionPill(Icons.Filled.PlayCircle, "YouTube Music", accent) {
            open(context, "https://music.youtube.com/search?q=$query")
        }
        ActionPill(Icons.Filled.ContentCopy, stringResource(R.string.player_copy), accent) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(track, track))
            onCopied()
        }
    }
}

private fun open(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Маленькая «пилюля»-кнопка в цвет станции. */
@Composable
private fun ActionPill(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.height(32.dp).clip(shape).background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Text(label, color = palette.text, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
    }
}

/** Сообщение поверх плеера: плашка в цвет станции с иконкой. */
@Composable
fun MessagePill(message: PlayerMessage, accent: Color, onAccent: Color) {
    val (icon, text) = when (message) {
        PlayerMessage.VOTE_OK -> Icons.Filled.CheckCircle to R.string.vote_ok
        PlayerMessage.VOTE_ALREADY -> Icons.Filled.Info to R.string.vote_already
        PlayerMessage.VOTE_ERROR -> Icons.Filled.ErrorOutline to R.string.vote_error
        PlayerMessage.COPIED -> Icons.Filled.ContentCopy to R.string.player_copied
        PlayerMessage.LOGO_SET -> Icons.Filled.CheckCircle to R.string.logo_set
    }
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.shadow(10.dp, shape, spotColor = Color.Black).clip(shape).background(accent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = onAccent, modifier = Modifier.size(18.dp))
        Text(stringResource(text), color = onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * Поток не играет: нет сети — «продолжим, когда сеть вернётся»; переподключаемся — крутится;
 * сдались (формат не поддерживается / попытки кончились) — «не воспроизводится» + «Повторить».
 */
@Composable
fun StreamIssue(failed: Boolean, online: Boolean, accent: Color, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val (icon, text) = when {
        !online -> Icons.Filled.WifiOff to R.string.err_stream_offline
        failed -> Icons.Filled.ErrorOutline to R.string.err_stream_failed
        else -> Icons.Filled.Sync to R.string.err_stream_retrying
    }
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(palette.cardBg.copy(alpha = 0.9f))
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (failed && online) palette.red else accent, modifier = Modifier.size(18.dp))
        Text(
            stringResource(text), color = palette.text, fontSize = 12.sp, lineHeight = 15.sp,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
        if (failed && online) {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry), color = accent, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** Окно «Логотип станции»: картинки с сайта станции на белых плитках; тап — поставить. */
@Composable
fun LogoPickerDialog(search: LogoSearch, site: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(palette.cardBg).padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(palette.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.ImageSearch, null, tint = palette.primary) }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(stringResource(R.string.logo_title), color = palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(site, color = palette.textMuted, fontSize = 12.sp, maxLines = 1)
                }
            }
            when {
                search.loading -> Row(Modifier.padding(vertical = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = palette.primary, strokeWidth = 3.dp)
                    Text(
                        stringResource(R.string.logo_searching), color = palette.textMuted, fontSize = 14.sp,
                        modifier = Modifier.padding(start = 14.dp),
                    )
                }
                search.results.isEmpty() -> Text(
                    stringResource(R.string.logo_none), color = palette.textMuted, fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                else -> {
                    Text(
                        stringResource(R.string.logo_pick_hint), color = palette.textMuted, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
                    )
                    // 3 в ряд: белая плитка — прозрачные логотипы видно на любой теме.
                    search.results.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { url ->
                                Box(
                                    Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp))
                                        .background(Color.White).clickable { onPick(url) }.padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize())
                                }
                            }
                            repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    }
}
