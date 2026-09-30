package com.tohn95.internetradio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.ui.theme.LocalPalette

/**
 * Строка станции (Поиск, История, списки): плоская, как в Избранном и у radioMii —
 * логотип, бегущее название, ряд плашек, сердечко. Играющая сейчас станция подсвечена.
 */
@Composable
fun StationCard(
    station: Station,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    isCurrent: Boolean = false,
    showListeners: Boolean = false,
) {
    val palette = LocalPalette.current
    Row(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationImage(model = station.faviconUrl, size = 52.dp, shape = RoundedCornerShape(10.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent) {
                    Icon(
                        Icons.Filled.GraphicEq, contentDescription = null, tint = palette.primary,
                        modifier = Modifier.padding(end = 4.dp).size(16.dp),
                    )
                }
                Text(
                    station.name, color = if (isCurrent) palette.primary else palette.text, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.marquee(),
                )
            }
            MetaChipRow(station, modifier = Modifier.padding(top = 5.dp), showListeners = showListeners)
        }
        if (onToggleFavorite != null) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) palette.red else palette.textMuted,
                )
            }
        }
    }
}
