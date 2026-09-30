package com.tohn95.internetradio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.R
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.ui.theme.LocalPalette
import com.tohn95.internetradio.ui.theme.Palette
import com.tohn95.internetradio.util.countryLabel
import android.icu.text.CompactDecimalFormat
import java.util.Locale

/**
 * Короткое число для подписей на языке приложения (ICU): ru «1,2 тыс.», en «1.2K», zh «1.2万»
 * (Locale.getDefault() — AppCompat выставляет его под выбранный язык).
 */
fun formatShort(n: Int): String =
    if (n < 1000) n.toString()
    else CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT)
        .apply { maximumFractionDigits = 1 }.format(n.toLong())

/** Цвет значка поверх primary: в тёмной теме primary светлый — нужен тёмный значок. */
fun Palette.onPrimary(): Color = if (isDark) pageBg else Color.White

/** Круглая кнопка ▶ в цвет темы. */
@Composable
fun PlayCircle(size: Dp, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val palette = LocalPalette.current
    Box(
        modifier.size(size).clip(CircleShape).background(palette.primary)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.PlayArrow, stringResource(R.string.play), tint = palette.onPrimary(), modifier = Modifier.size(size * 0.62f))
    }
}

/** Заголовок секции Главной; с [onMore] вся строка кликабельна и справа «Все ›». */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    topPadding: Dp = 22.dp,
    onMore: (() -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Row(
        modifier.fillMaxWidth()
            .then(if (onMore != null) Modifier.clickable(onClick = onMore) else Modifier)
            .padding(start = 16.dp, end = 8.dp, top = topPadding, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Длинный заголовок обрезается многоточием и не наезжает на «Все ›».
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, color = palette.text, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            )
            if (badge != null) {
                Text(badge, color = palette.red, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (onMore != null) {
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_see_all), color = palette.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.primary)
        }
    }
}

fun tileSubtitle(s: Station): String =
    listOfNotNull(s.tags.firstOrNull(), s.countryLabel().takeIf { it.isNotBlank() }).joinToString(" · ")

/** Квадратная плитка для горизонтальных каруселей. */
@Composable
fun StationTile(
    station: Station,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 136.dp,
) {
    val palette = LocalPalette.current
    Column(modifier.width(size).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)) {
        StationImage(model = station.faviconUrl, size = size, shape = RoundedCornerShape(16.dp))
        Text(
            station.name, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp).marquee(),
        )
        Text(
            tileSubtitle(station), color = palette.textMuted, fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

/** Круглая плитка «Недавно слушал» — как исполнители в Spotify. */
@Composable
fun RecentTile(station: Station, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier.width(84.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StationImage(model = station.faviconUrl, size = 76.dp, shape = CircleShape)
        Text(
            station.name, color = palette.text, fontSize = 12.sp, textAlign = TextAlign.Center,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Карточка сетки «Популярное» 2×2: логотип, ▶, название, слушатели, теги. */
@Composable
fun PopularCard(station: Station, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(palette.cardBg)
            .clickable(onClick = onClick).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            StationImage(model = station.faviconUrl, size = 52.dp, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.weight(1f))
            PlayCircle(34.dp)
        }
        Text(
            station.name, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, modifier = Modifier.padding(top = 10.dp).marquee(),
        )
        Text(
            stringResource(R.string.listeners_short, formatShort(station.clickCount)),
            color = palette.textMuted, fontSize = 12.sp, maxLines = 1,
        )
        MetaChipRow(station, maxGenres = 1, modifier = Modifier.padding(top = 6.dp))
    }
}

/** «Новая станция» — по мотивам карточки «Новый релиз» Spotify: крупный логотип, теги, «+», ▶. */
@Composable
fun NewStationCard(
    station: Station,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.cardBg)
            .clickable(onClick = onPlay).padding(14.dp),
    ) {
        StationImage(model = station.faviconUrl, size = 108.dp, shape = RoundedCornerShape(14.dp))
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                stringResource(R.string.home_new_badge), color = palette.primary, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            )
            Text(
                station.name, color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (station.country.isNotBlank()) {
                Text(station.countryLabel(), color = palette.textMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MetaChipRow(station, maxGenres = 2, modifier = Modifier.padding(top = 6.dp))
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (isFavorite) Icons.Filled.CheckCircle else Icons.Outlined.AddCircleOutline,
                        contentDescription = stringResource(if (isFavorite) R.string.info_remove_favorite else R.string.info_add_favorite),
                        tint = if (isFavorite) palette.primary else palette.textMuted,
                        modifier = Modifier.size(28.dp),
                    )
                }
                PlayCircle(44.dp, onClick = onPlay)
            }
        }
    }
}

/**
 * «1 станция / 3 станции / 5 станций» — plurals по правилам языка приложения (у каждого языка свои формы).
 * Раньше склоняли вручную: ресурсы были только русские, а правила брались из языка системы.
 */
@Composable
fun stationsCount(n: Int): String = pluralStringResource(R.plurals.stations_count, n, n)
