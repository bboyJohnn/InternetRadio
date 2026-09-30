package com.tohn95.internetradio.ui.screens.player

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.tohn95.internetradio.R
import com.tohn95.internetradio.util.countryLabel
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.ui.components.ChipKind
import com.tohn95.internetradio.ui.components.MetaChip
import com.tohn95.internetradio.ui.components.StationImage
import com.tohn95.internetradio.ui.components.formatShort
import com.tohn95.internetradio.ui.screens.home.flagEmoji
import com.tohn95.internetradio.ui.theme.LocalPalette

/** Высота «языка» первой карточки, который выглядывает снизу экрана плеера. */
val InfoPeekHeight = 68.dp

/**
 * Полная информация о станции под плеером (листается вниз, как «Об исполнителе» у Spotify).
 * Верх первой карточки с ручкой виден под плеером — пользователь понимает, что ниже есть ещё.
 */
@Composable
fun StationInfoSection(
    station: Station,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
    onFindLogo: (() -> Unit)? = null,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val s = station

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard {
            // «Язык» карточки: ручка + заголовок + подсказка — это и выглядывает под плеером.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(palette.textMuted.copy(alpha = 0.5f)))
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.station_info), color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.player_scroll_hint), color = palette.textMuted, fontSize = 12.sp)
                }
                Icon(Icons.Filled.KeyboardArrowUp, null, tint = palette.textMuted)
            }
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                StationImage(model = s.faviconUrl, size = 64.dp, shape = RoundedCornerShape(14.dp))
                Column(Modifier.padding(start = 14.dp)) {
                    Text(s.name, color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
                    val place = listOfNotNull(
                        s.countryCode.takeIf { it.isNotBlank() }?.let { flagEmoji(it) + " " + countryName(s) },
                        s.language.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() },
                    ).joinToString(" · ")
                    if (place.isNotBlank()) Text(place, color = palette.textMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (s.tags.isNotEmpty()) {
                FlowRow(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) { s.tags.take(12).forEach { MetaChip(it, ChipKind.GENRE) } }
            }
            s.homepage?.takeIf { it.isNotBlank() }?.let { url ->
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) } }) {
                        Icon(Icons.Filled.Language, null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.info_homepage), modifier = Modifier.padding(start = 8.dp))
                    }
                    // Логотип с сайта станции (идея radioMii): полезно, когда своего логотипа у станции нет.
                    if (onFindLogo != null) OutlinedButton(onClick = onFindLogo) {
                        Icon(Icons.Filled.ImageSearch, null, modifier = Modifier.size(18.dp))
                        Text(
                            stringResource(if (s.faviconUrl.isNullOrBlank()) R.string.logo_find else R.string.logo_change),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }

        // Своя станция по ссылке в каталоге не числится — популярности у неё нет.
        if (!s.isCustom) InfoCard {
            Text(stringResource(R.string.info_popularity), color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                BigNumber(formatShort(s.clickCount), stringResource(R.string.info_listeners_day), accent, Modifier.weight(1f))
                BigNumber(formatShort(s.votes), stringResource(R.string.info_votes_short), accent, Modifier.weight(1f))
            }
        }

        InfoCard {
            Text(stringResource(R.string.info_tech), color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.padding(top = 8.dp)) {
                if (s.codec.isNotBlank()) InfoRow(stringResource(R.string.info_codec), s.codec.uppercase())
                if (s.bitrate > 0) InfoRow(stringResource(R.string.filter_bitrate), stringResource(R.string.bitrate_kbps, s.bitrate))
                if (s.isHls) InfoRow(stringResource(R.string.info_hls), stringResource(R.string.info_hls_yes))
                if (!s.isCustom) InfoRow(
                    stringResource(R.string.info_stream),
                    stringResource(if (s.lastCheckOk) R.string.info_check_ok else R.string.info_check_failed),
                )
                Text(stringResource(R.string.info_stream_url), color = palette.textMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                Text(s.streamUrl, color = palette.text, fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
            }
        }

        Button(
            onClick = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${s.name}\n${s.homepage ?: s.streamUrl}")
                }
                runCatching { context.startActivity(Intent.createChooser(share, null)) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Filled.Share, null, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.info_share), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(palette.cardBg.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
private fun BigNumber(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(modifier) {
        Text(value, color = accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(label, color = palette.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = palette.textMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun countryName(s: Station): String = s.countryLabel()
