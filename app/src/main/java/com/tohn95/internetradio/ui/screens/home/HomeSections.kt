package com.tohn95.internetradio.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.R
import com.tohn95.internetradio.util.countryDisplayName
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.ui.components.ChipKind
import com.tohn95.internetradio.ui.components.MetaChip
import com.tohn95.internetradio.ui.components.StationImage
import com.tohn95.internetradio.ui.components.WordFitText
import com.tohn95.internetradio.ui.components.codecLabel
import com.tohn95.internetradio.ui.components.formatShort
import com.tohn95.internetradio.ui.components.marquee
import com.tohn95.internetradio.ui.components.onPrimary
import com.tohn95.internetradio.ui.theme.LocalPalette
import com.tohn95.internetradio.ui.theme.Oklch

/** Широкий баннер-«пилюля» «Мне повезёт» (по мотивам «Your likes»): кубик слева, перемешать справа. */
@Composable
fun LuckyBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val deep = lerp(palette.primaryActive, Color.Black, 0.4f)
    Row(
        modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(32.dp))
            .background(Brush.horizontalGradient(listOf(deep, palette.primaryActive, palette.primary)))
            .clickable(onClick = onClick).padding(start = 8.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Casino, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(stringResource(R.string.home_lucky), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.home_lucky_sub), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1)
        }
        Icon(Icons.Filled.Shuffle, stringResource(R.string.home_lucky), tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

/** Карточка микса (как «Твои лучшие миксы» Spotify): фото жанра, значок «МИКС», цветная полоса снизу. */
@Composable
fun MixCard(mix: Mix, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val title = stringResource(mix.genre.title)
    Column(Modifier.width(168.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)) {
        Box(Modifier.size(168.dp).clip(RoundedCornerShape(12.dp))) {
            Image(painterResource(mix.genre.image), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f))))
            Text(
                stringResource(R.string.home_mix_badge), color = palette.onPrimary(), fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(50)).background(palette.primary)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
            Text(
                title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = 14.dp),
            )
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(5.dp).background(palette.primary))
        }
        Text(
            stringResource(R.string.home_mix_title, title), color = palette.text, fontSize = 13.sp,
            fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 8.dp, start = 2.dp),
        )
        if (mix.stationNames.isNotEmpty()) {
            Text(
                stringResource(R.string.home_and_more, mix.stationNames.joinToString(", ")),
                color = palette.textMuted, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

/** «В тренде» как чарт: панель с крупными номерами 1–5 (не ещё одна карусель плиток). */
@Composable
fun ChartPanel(stations: List<Station>, onPlay: (Station) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(palette.cardBg).padding(vertical = 6.dp),
    ) {
        stations.take(5).forEachIndexed { i, s ->
            Row(
                Modifier.fillMaxWidth().clickable { onPlay(s) }.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${i + 1}", color = if (i == 0) palette.primary else palette.textMuted, fontSize = 26.sp,
                    fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp),
                )
                StationImage(model = s.faviconUrl, size = 46.dp, shape = RoundedCornerShape(10.dp), modifier = Modifier.padding(start = 6.dp))
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(s.name, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.marquee())
                    Text(
                        stringResource(R.string.listeners_short, formatShort(s.clickCount)),
                        color = palette.textMuted, fontSize = 12.sp, maxLines = 1,
                    )
                }
                Icon(Icons.Filled.PlayArrow, stringResource(R.string.play), tint = palette.primary)
            }
        }
    }
}

/** «Под настроение»: цветные блоки 2×2 с наклонённой картинкой в углу (как «Start browsing»). */
@Composable
fun MoodGrid(moods: List<Mood>, onClick: (Mood) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        moods.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { m -> MoodBlock(m, onClick = { onClick(m) }, modifier = Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MoodBlock(mood: Mood, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Насыщенный фиксированный цвет у каждого настроения — белый текст читается в обеих темах.
    val bg = Oklch.toColor(0.56, 0.14, mood.hue)
    Box(modifier.height(92.dp).clip(RoundedCornerShape(14.dp)).background(bg).clickable(onClick = onClick)) {
        WordFitText(
            stringResource(mood.title),
            style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 19.sp),
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, end = 64.dp),
        )
        Image(
            painterResource(mood.image), null, contentScale = ContentScale.Crop,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 14.dp, y = 8.dp).rotate(22f).size(70.dp)
                .shadow(8.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)),
        )
    }
}

/** «Радио мира»: круглые флаги стран. */
@Composable
fun WorldRow(countries: List<WorldCountry>, onClick: (WorldCountry) -> Unit) {
    val palette = LocalPalette.current
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(countries, key = { "w" + it.code }) { c ->
            Column(
                Modifier.width(78.dp).clip(RoundedCornerShape(12.dp)).clickable { onClick(c) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(70.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(palette.itemBg, palette.btnBg)))
                        .border(2.dp, palette.primary.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(c.flag, fontSize = 34.sp) }
                WordFitText(
                    countryDisplayName(c.code, c.name),   // на текущем языке: модель экрана переживает смену языка
                    style = TextStyle(color = palette.text, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 14.sp),
                    minLines = 2, minSize = 9.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** «Хиты десятилетий»: высокие градиентные карточки с огромной цифрой и хэштегом (как «Explore your genres»). */
@Composable
fun DecadeRow(decades: List<Decade>, onClick: (Decade) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(decades, key = { "d" + it.tag }) { d ->
            Box(
                Modifier.width(118.dp).height(168.dp).clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Oklch.toColor(0.58, 0.15, d.hue), Oklch.toColor(0.30, 0.10, d.hue + 25.0)),
                        )
                    )
                    .clickable { onClick(d) },
            ) {
                Text(
                    d.big, color = Color.White.copy(alpha = 0.22f), fontSize = 86.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-10).dp),
                )
                Text(
                    stringResource(R.string.decade_label, d.label), color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                )
            }
        }
    }
}

/** «Высокое качество»: лента «пилюль» в два ряда (логотип + название + кодек). */
@Composable
fun HqPillGrid(stations: List<Station>, onPlay: (Station) -> Unit) {
    val palette = LocalPalette.current
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(60.dp * 2 + 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(stations, key = { "hq" + it.uuid }) { s ->
            Row(
                Modifier.width(236.dp).height(60.dp).clip(RoundedCornerShape(12.dp)).background(palette.cardBg)
                    .clickable { onPlay(s) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StationImage(model = s.faviconUrl, size = 60.dp, shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(s.name, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.marquee())
                    codecLabel(s)?.let { MetaChip(it, ChipKind.BITRATE, modifier = Modifier.padding(top = 3.dp)) }
                }
            }
        }
    }
}
