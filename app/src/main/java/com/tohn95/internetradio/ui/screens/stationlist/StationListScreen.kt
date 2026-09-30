package com.tohn95.internetradio.ui.screens.stationlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tohn95.internetradio.R
import com.tohn95.internetradio.playback.PlayerController
import com.tohn95.internetradio.ui.components.CatalogError
import com.tohn95.internetradio.ui.components.FilterCapsule
import com.tohn95.internetradio.ui.components.LocalBottomBarInset
import com.tohn95.internetradio.ui.components.SearchField
import com.tohn95.internetradio.ui.components.ShimmerCard
import com.tohn95.internetradio.ui.components.StationCard
import com.tohn95.internetradio.ui.components.rememberCurrentUuid
import com.tohn95.internetradio.ui.theme.LocalPalette

/**
 * Полный список станций (жанр, страна, популярное, тренды, HQ): поиск по названию внутри списка
 * и сортировка капсулой, как в Поиске. Сердечек в строках нет — «Мне нравится» в плеерах.
 */
@Composable
fun StationListScreen(
    onBack: () -> Unit,
    playerController: PlayerController? = null,
    viewModel: StationListViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val currentUuid = rememberCurrentUuid(playerController)
    var sortMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = palette.text)
            }
            Text(
                viewModel.title, color = palette.text, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        SearchField(
            value = query, onValueChange = viewModel::setQuery,
            placeholder = stringResource(R.string.list_search_hint, viewModel.title),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                // Не по умолчанию — подсвечена; ✕ возвращает сортировку по умолчанию.
                FilterCapsule(
                    label = stringResource(sort.label),
                    icon = Icons.AutoMirrored.Filled.Sort,
                    selected = sort != viewModel.sorts.first(),
                    onClick = { sortMenu = true },
                    onClear = { viewModel.setSort(viewModel.sorts.first()) },
                )
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    viewModel.sorts.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(stringResource(s.label)) },
                            trailingIcon = if (s == sort) {
                                { Icon(Icons.Filled.Check, null, tint = palette.primary) }
                            } else null,
                            onClick = { sortMenu = false; viewModel.setSort(s) },
                        )
                    }
                }
            }
            if (!state.loading && state.stations.isNotEmpty()) {
                Text(
                    stringResource(R.string.list_count, state.stations.size) + if (state.endReached) "" else "+",
                    color = palette.textMuted, fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp + LocalBottomBarInset.current),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when {
                state.loading -> items(List(6) { it }) { ShimmerCard(Modifier.padding(vertical = 4.dp)) }
                state.error -> item { CatalogError(onRetry = { viewModel.reload() }, modifier = Modifier.padding(vertical = 12.dp)) }
                state.stations.isEmpty() -> item {
                    Text(
                        stringResource(R.string.list_empty), color = palette.textMuted,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
                else -> {
                    itemsIndexed(state.stations, key = { _, s -> s.uuid }) { index, s ->
                        if (index == state.stations.lastIndex) LaunchedEffect(state.stations.size) { viewModel.loadMore() }
                        StationCard(
                            s, onClick = { playerController?.play(s) },
                            isCurrent = s.uuid == currentUuid,
                            showListeners = true,
                        )
                    }
                    if (state.loadingMore) item {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(28.dp), color = palette.primary)
                        }
                    }
                }
            }
        }
    }
}
