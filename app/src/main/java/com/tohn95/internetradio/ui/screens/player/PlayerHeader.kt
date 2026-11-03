package com.tohn95.internetradio.ui.screens.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.R
import com.tohn95.internetradio.playback.PlaySource
import com.tohn95.internetradio.ui.theme.LocalPalette

enum class HeaderKind { FOLDER, FAVORITES, SEARCH, GENRE, SECTION, HISTORY, LIVE }

/** Две строки шапки: что за источник (мелко) и его название; [title] — уже готовый текст. */
data class HeaderText(val kind: HeaderKind, val title: String)

/** Подпись «откуда играет», как «Из плейлиста RokLiafe» у Spotify. [fallback] — «страна · жанр» станции. */
fun headerText(source: PlaySource, fallback: String): HeaderText = when (source) {
    is PlaySource.Folder -> HeaderText(HeaderKind.FOLDER, source.name)
    PlaySource.Favorites -> HeaderText(HeaderKind.FAVORITES, fallback)
    is PlaySource.Search -> HeaderText(HeaderKind.SEARCH, source.query.ifBlank { fallback })
    is PlaySource.Genre -> HeaderText(HeaderKind.GENRE, source.title)
    is PlaySource.Section -> HeaderText(HeaderKind.SECTION, source.title)
    PlaySource.History -> HeaderText(HeaderKind.HISTORY, fallback)
    PlaySource.None -> HeaderText(HeaderKind.LIVE, fallback)
}

private fun HeaderKind.label(): Int = when (this) {
    HeaderKind.FOLDER -> R.string.player_from_folder
    HeaderKind.FAVORITES -> R.string.player_from_favorites
    HeaderKind.SEARCH -> R.string.player_from_search
    HeaderKind.GENRE -> R.string.player_from_genre
    HeaderKind.SECTION -> R.string.player_from_collection
    HeaderKind.HISTORY -> R.string.player_from_history
    HeaderKind.LIVE -> R.string.player_live_now
}

/** Шапка экрана станции: ⌄ закрыть · откуда играет · ⋮ (поделиться, найти логотип, о станции). */
@Composable
fun PlayerHeader(
    source: PlaySource,
    fallback: String,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onFindLogo: () -> Unit,
    onInfo: () -> Unit,
) {
    val palette = LocalPalette.current
    val text = headerText(source, fallback)
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.back), tint = palette.text)
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(text.kind.label()).toUpperCase(Locale.current), color = palette.textMuted,
                fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium, maxLines = 1,
            )
            if (text.title.isNotBlank()) {
                Text(
                    text.title, color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        var menu by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.fav_more), tint = palette.text)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.info_share)) },
                    leadingIcon = { Icon(Icons.Filled.Share, null) },
                    onClick = { menu = false; onShare() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.logo_find)) },
                    leadingIcon = { Icon(Icons.Filled.ImageSearch, null) },
                    onClick = { menu = false; onFindLogo() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.station_info)) },
                    leadingIcon = { Icon(Icons.Outlined.Info, null) },
                    onClick = { menu = false; onInfo() },
                )
            }
        }
    }
}
