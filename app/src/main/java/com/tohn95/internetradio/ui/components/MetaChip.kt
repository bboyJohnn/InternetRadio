package com.tohn95.internetradio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.ui.theme.LocalPalette
import com.tohn95.internetradio.ui.theme.Palette

enum class ChipKind { GENRE, BITRATE, COUNTRY }

private val ChipShape = RoundedCornerShape(4.dp)

/**
 * Цвета плашек в стиле radioMii: ВСЕ плашки одного спокойного тонового цвета из темы
 * (у них secondaryContainer / onSecondaryContainer). Пёстрые разноцветные плашки выглядели дёшево.
 */
private fun Palette.chipBg(): Color = lerp(cardBg, primary, if (isDark) 0.22f else 0.28f)
private fun Palette.chipFg(): Color = lerp(text, primary, if (isDark) 0.30f else 0.45f)

/** Плашка метаданных: прямоугольная, почти без скругления, 12sp — как у radioMii. */
@Composable
fun MetaChip(text: String, kind: ChipKind, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Text(
        text = text,
        color = palette.chipFg(),
        fontSize = 12.sp,
        lineHeight = 14.sp,
        // Кодек+битрейт — главная техническая плашка, чуть плотнее остальных.
        fontWeight = if (kind == ChipKind.BITRATE) FontWeight.Medium else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            // Потолок ширины: длинный тег обрезается многоточием, а не растягивает строку.
            .widthIn(max = 120.dp)
            .background(palette.chipBg(), ChipShape)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

/** Плашка со значком (замок https, наушники + число слушателей). */
@Composable
private fun IconChip(icon: ImageVector, label: String? = null) {
    val palette = LocalPalette.current
    Row(
        Modifier.background(palette.chipBg(), ChipShape).padding(horizontal = 4.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = palette.chipFg(), modifier = Modifier.size(14.dp))
        if (label != null) {
            Text(
                label, color = palette.chipFg(), fontSize = 12.sp, lineHeight = 14.sp, maxLines = 1,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

/** «MP3 128k» / «AAC+ 64k» / «128k»; неизвестный кодек не показываем. */
fun codecLabel(station: Station): String? {
    val codec = station.codec.trim().uppercase().takeIf { it.isNotEmpty() && it != "UNKNOWN" }
    val kbps = station.bitrate.takeIf { it in 1..999 }
    return when {
        codec != null && kbps != null -> "$codec ${kbps}k"
        kbps != null -> "${kbps}k"
        else -> codec
    }
}

/** Компактное число слушателей для плашки: 950 / 1.2k / 15k / 1.1M (как у radioMii). */
private fun compactCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(java.util.Locale.ROOT, n / 1_000_000.0).replace(".0M", "M")
    n >= 10_000 -> "${n / 1000}k"
    n >= 1_000 -> "%.1fk".format(java.util.Locale.ROOT, n / 1000.0).replace(".0k", "k")
    else -> n.toString()
}

/**
 * Ряд тегов станции в ОДНУ строку: [слушатели], кодек+битрейт, код страны, замок https, жанры.
 * Что не влезло — обрезается, на вторую строку не переносится.
 */
@Composable
fun MetaChipRow(
    station: Station,
    maxGenres: Int = 3,
    modifier: Modifier = Modifier,
    showListeners: Boolean = false,
) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        maxLines = 1,
    ) {
        if (showListeners && station.clickCount > 0) IconChip(Icons.Outlined.Headphones, compactCount(station.clickCount))
        codecLabel(station)?.let { MetaChip(it, ChipKind.BITRATE) }
        val country = station.countryCode.ifBlank { station.country }
        if (country.isNotBlank()) MetaChip(country.uppercase().take(3), ChipKind.COUNTRY)
        if (station.streamUrl.startsWith("https", ignoreCase = true)) IconChip(Icons.Outlined.Lock)
        station.tags.take(maxGenres).forEach { tag -> MetaChip(tag, ChipKind.GENRE) }
    }
}
