package com.tohn95.internetradio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.R
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.ui.theme.LocalPalette

/** Бегущая строка для названий, которые не влезают: стоит 1,5 с, потом едет по кругу. */
fun Modifier.marquee(): Modifier = basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1500, repeatDelayMillis = 1500)

/**
 * Плавающий мини-плеер: объёмная карточка с тенью и подсветкой в цвет темы,
 * логотип, бегущие строки станции и трека, ♡ слева от паузы.
 */
@Composable
fun MiniPlayerBar(
    station: Station,
    isPlaying: Boolean,
    // Кнопка ⏯ — по намерению: во время подгрузки уже ⏸.
    playRequested: Boolean,
    streamAlive: Boolean,
    trackText: String?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    val tinted = lerp(palette.cardBg, palette.primary, 0.28f)
    Row(
        modifier.fillMaxWidth().height(68.dp)
            .shadow(10.dp, shape, spotColor = Color.Black)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(tinted, palette.cardBg)))
            .border(1.dp, palette.primary.copy(alpha = 0.22f), shape)
            .clickable(onClick = onOpen)
            .padding(start = 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationImage(model = station.faviconUrl, size = 48.dp, shape = RoundedCornerShape(12.dp))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                station.name, color = palette.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, modifier = Modifier.marquee(),
            )
            if (trackText != null) {
                Text(trackText, color = palette.textMuted, fontSize = 12.sp, maxLines = 1, modifier = Modifier.marquee())
            } else {
                LiveDot(alive = streamAlive, playing = isPlaying)
            }
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(if (isFavorite) R.string.info_remove_favorite else R.string.info_add_favorite),
                tint = if (isFavorite) palette.red else palette.textMuted,
            )
        }
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(palette.primary).clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playRequested) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null, tint = palette.onPrimary(), modifier = Modifier.size(26.dp),
            )
        }
    }
}
