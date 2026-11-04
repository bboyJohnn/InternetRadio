package com.tohn95.internetradio.ui.screens.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tohn95.internetradio.R
import com.tohn95.internetradio.domain.model.Folder
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.playback.PlayQueue
import com.tohn95.internetradio.playback.PlaySource
import com.tohn95.internetradio.playback.PlayerController
import com.tohn95.internetradio.ui.components.LocalBottomBarInset
import com.tohn95.internetradio.ui.components.MetaChipRow
import com.tohn95.internetradio.ui.components.SearchField
import com.tohn95.internetradio.ui.components.StationImage
import com.tohn95.internetradio.ui.components.dragDropContainer
import com.tohn95.internetradio.ui.components.dragDropItem
import com.tohn95.internetradio.ui.components.marquee
import com.tohn95.internetradio.ui.components.rememberCurrentUuid
import com.tohn95.internetradio.ui.components.rememberDragDropState
import com.tohn95.internetradio.ui.components.stationsCount
import com.tohn95.internetradio.ui.theme.LocalPalette
import kotlinx.coroutines.delay

private sealed interface FavDialog {
    data class Create(val thenAdd: Station? = null) : FavDialog
    data class Rename(val folder: Folder) : FavDialog
    data class Delete(val folder: Folder) : FavDialog
    data class Pick(val station: Station) : FavDialog
    data object Custom : FavDialog
}

/**
 * Избранное как «Моя медиатека» Spotify: сверху папки (обложка-мозаика из логотипов),
 * ниже все станции строками. Станция может лежать в нескольких папках.
 */
@Composable
fun FavoritesScreen(
    playerController: PlayerController? = null,
    onOpenFolder: (Long) -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val library by viewModel.library.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val currentUuid = rememberCurrentUuid(playerController)
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<FavDialog?>(null) }
    val listState = rememberLazyListState()
    val folderKey = remember { { key: Any -> key is String && key.startsWith("folder:") } }
    val dragState = rememberDragDropState(listState, folderKey) {
        sort == FavSort.CUSTOM && query.isBlank()
    }

    LaunchedEffect(library.stations) {
        playerController?.updateQueue(PlaySource.Favorites, library.stations)
    }
    LaunchedEffect(notice) {
        if (notice != null) { delay(2200); viewModel.noticeShown() }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.fav_title), color = palette.text, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { searchVisible = !searchVisible; if (!searchVisible) viewModel.setQuery("") }) {
                Icon(Icons.Filled.Search, stringResource(R.string.fav_search_hint), tint = palette.text)
            }
            AddMenu(onNewFolder = { dialog = FavDialog.Create() }, onCustom = { dialog = FavDialog.Custom })
        }
        if (searchVisible || query.isNotEmpty()) {
            SearchField(
                value = query, onValueChange = viewModel::setQuery,
                placeholder = stringResource(R.string.fav_search_hint),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = filter == LibraryFilter.FOLDERS,
                onClick = { viewModel.setFilter(LibraryFilter.FOLDERS) },
                label = { Text(stringResource(R.string.fav_folders)) },
            )
            FilterChip(
                selected = filter == LibraryFilter.STATIONS,
                onClick = { viewModel.setFilter(LibraryFilter.STATIONS) },
                label = { Text(stringResource(R.string.fav_stations)) },
            )
            Box(Modifier.weight(1f))
            SortMenu(sort, viewModel::setSort)
        }

        if (library.isEmpty) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_favorites), color = palette.textMuted)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).dragDropContainer(
                    state = dragState,
                    onReorder = { from, to ->
                        val a = from.toString(); val b = to.toString()
                        if (a.startsWith("folder:") && b.startsWith("folder:")) {
                            viewModel.reorderFolders(a.removePrefix("folder:").toLong(), b.removePrefix("folder:").toLong())
                        } else if (a.startsWith("station:") && b.startsWith("station:")) {
                            viewModel.reorderStations(a.removePrefix("station:"), b.removePrefix("station:"))
                        }
                    },
                    onDropInto = { station, folder ->
                        viewModel.dropInto(station.toString().removePrefix("station:"), folder.toString().removePrefix("folder:").toLong())
                    },
                ),
                contentPadding = PaddingValues(bottom = 12.dp + LocalBottomBarInset.current),
            ) {
                items(library.folders, key = { "folder:${it.id}" }) { folder ->
                    val key = "folder:${folder.id}"
                    FolderRow(
                        folder = folder,
                        onClick = { onOpenFolder(folder.id) },
                        highlighted = (dragState.target as? DragTarget.Into)?.folderKey == key,
                        modifier = Modifier.dragDropItem(dragState, key),
                    )
                }
                if (filter != LibraryFilter.STATIONS && query.isBlank()) {
                    item(key = "new_folder") { NewFolderRow(onClick = { dialog = FavDialog.Create() }) }
                }
                items(library.stations, key = { "station:${it.uuid}" }) { station ->
                    val key = "station:${station.uuid}"
                    FavoriteRow(
                        station = station,
                        onPlay = { playerController?.play(station, PlayQueue(PlaySource.Favorites, library.stations)) },
                        onAddToFolder = { dialog = FavDialog.Pick(station) },
                        onRemoveFavorite = { viewModel.removeFavorite(station) },
                        isCurrent = station.uuid == currentUuid,
                        modifier = Modifier.dragDropItem(dragState, key),
                    )
                }
            }
        }
        if (notice != null) {
            Text(
                stringResource(R.string.fav_added_to_folder, notice.orEmpty()),
                color = palette.primary, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }

    when (val d = dialog) {
        is FavDialog.Create -> NameDialog(
            title = stringResource(R.string.fav_new_folder), initial = "", confirm = stringResource(R.string.dialog_create),
            onDismiss = { dialog = null },
            onConfirm = { name -> viewModel.createFolder(name, d.thenAdd); dialog = null },
        )
        is FavDialog.Rename -> NameDialog(
            title = stringResource(R.string.folder_rename), initial = d.folder.name, confirm = stringResource(R.string.dialog_save),
            onDismiss = { dialog = null },
            onConfirm = { name -> viewModel.renameFolder(d.folder.id, name); dialog = null },
        )
        is FavDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.folder_delete)) },
            text = { Text(stringResource(R.string.folder_delete_confirm, d.folder.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFolder(d.folder.id); dialog = null }) {
                    Text(stringResource(R.string.dialog_delete), color = palette.red)
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.dialog_cancel)) } },
        )
        is FavDialog.Pick -> PickFoldersDialog(
            station = d.station, folders = folders,
            onDismiss = { dialog = null },
            onNewFolder = { dialog = FavDialog.Create(thenAdd = d.station) },
            onDone = { ids -> viewModel.setFolders(d.station, ids); dialog = null },
        )
        FavDialog.Custom -> CustomStationDialog(
            onDismiss = { dialog = null },
            onTest = { playerController?.play(it) },
            onAdd = { viewModel.addCustomStation(it); dialog = null },
        )
        null -> {}
    }
}

@Composable
private fun SortMenu(current: FavSort, onSelect: (FavSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = when (current) {
        FavSort.CUSTOM -> R.string.fav_sort_custom
        FavSort.NAME -> R.string.fav_sort_name
        FavSort.RECENT -> R.string.fav_sort_recent
    }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, null, modifier = Modifier.size(18.dp))
            Text(stringResource(label), maxLines = 1)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(FavSort.CUSTOM, FavSort.NAME, FavSort.RECENT).forEach { item ->
                val title = when (item) {
                    FavSort.CUSTOM -> R.string.fav_sort_custom
                    FavSort.NAME -> R.string.fav_sort_name
                    FavSort.RECENT -> R.string.fav_sort_recent
                }
                DropdownMenuItem(
                    text = { Text(stringResource(title)) },
                    onClick = { onSelect(item); open = false },
                )
            }
        }
    }
}

/** «+» в заголовке Избранного: новая папка или своя станция по ссылке. */
@Composable
private fun AddMenu(onNewFolder: () -> Unit, onCustom: () -> Unit) {
    val palette = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.AddCircle, stringResource(R.string.fav_add), tint = palette.primary, modifier = Modifier.size(28.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.fav_new_folder)) },
                leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null, tint = palette.primary) },
                onClick = { open = false; onNewFolder() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.custom_menu)) },
                leadingIcon = { Icon(Icons.Filled.Link, null, tint = palette.primary) },
                onClick = { open = false; onCustom() },
            )
        }
    }
}

@Composable
private fun SubHeader(text: String) {
    Text(
        text, color = LocalPalette.current.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
    )
}

/** Обложка папки: свой снимок, мозаика 2×2, один логотип или значок папки. */
@Composable
internal fun FolderCover(folder: Folder, size: Dp, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(10.dp)
    val logos = folder.stations.map { it.faviconUrl }
    when {
        !folder.coverPath.isNullOrBlank() -> StationImage(
            model = "file://${folder.coverPath}", size = size, shape = shape, modifier = modifier,
        )
        logos.size >= 4 -> Column(modifier.size(size).clip(shape)) {
            logos.take(4).chunked(2).forEach { row ->
                Row { row.forEach { StationImage(model = it, size = size / 2, shape = RoundedCornerShape(0.dp)) } }
            }
        }
        logos.isNotEmpty() -> StationImage(model = logos.first(), size = size, shape = shape, modifier = modifier)
        else -> Box(
            modifier.size(size).clip(shape)
                .background(Brush.linearGradient(listOf(lerp(palette.primary, Color.Black, 0.35f), palette.primary))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Folder, null, tint = Color.White, modifier = Modifier.size(size * 0.45f))
        }
    }
}

@Composable
private fun FolderRow(
    folder: Folder,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(
        modifier.fillMaxWidth()
            .then(if (highlighted) Modifier.border(2.dp, palette.primary, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolderCover(folder, 56.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(folder.name, color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (highlighted) stringResource(R.string.fav_drop_hint)
                else stringResource(R.string.fav_folder_label, stationsCount(folder.stations.size)),
                color = if (highlighted) palette.primary else palette.textMuted, fontSize = 13.sp,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.textMuted)
    }
}

@Composable
private fun NewFolderRow(onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, palette.cardBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, null, tint = palette.primary, modifier = Modifier.size(28.dp))
        }
        Text(
            stringResource(R.string.fav_new_folder), color = palette.text, fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

/** Строка станции в избранном: логотип, бегущее название, теги, меню ⋮. */
@Composable
private fun FavoriteRow(
    station: Station,
    onPlay: () -> Unit,
    onAddToFolder: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onRemoveFromFolder: (() -> Unit)? = null,
    isCurrent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().clickable(onClick = onPlay).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationImage(model = station.faviconUrl, size = 52.dp, shape = RoundedCornerShape(10.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
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
            MetaChipRow(station, maxGenres = 2, modifier = Modifier.padding(top = 4.dp))
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.fav_more), tint = palette.textMuted)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.fav_add_to_folder)) },
                    onClick = { menu = false; onAddToFolder() },
                )
                if (onRemoveFromFolder != null) DropdownMenuItem(
                    text = { Text(stringResource(R.string.fav_remove_from_folder)) },
                    onClick = { menu = false; onRemoveFromFolder() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.fav_remove), color = palette.red) },
                    onClick = { menu = false; onRemoveFavorite() },
                )
            }
        }
    }
}

@Composable
private fun FolderMenu(onRename: () -> Unit, onDelete: () -> Unit) {
    val palette = LocalPalette.current
    var menu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menu = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.fav_more), tint = palette.text)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.folder_rename)) }, onClick = { menu = false; onRename() })
            DropdownMenuItem(
                text = { Text(stringResource(R.string.folder_delete), color = palette.red) },
                onClick = { menu = false; onDelete() },
            )
        }
    }
}

@Composable
internal fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable(title, initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                placeholder = { Text(stringResource(R.string.folder_name_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

/** Выбор папок для станции галочками; можно сразу создать новую (станция попадёт в неё). */
@Composable
internal fun PickFoldersDialog(
    station: Station,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onNewFolder: () -> Unit,
    onDone: (Set<Long>) -> Unit,
) {
    val palette = LocalPalette.current
    var selected by remember(station.uuid) {
        mutableStateOf(folders.filter { f -> f.stations.any { it.uuid == station.uuid } }.map { it.id }.toSet())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_pick_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(station.name, color = palette.textMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (folders.isEmpty()) {
                    Text(stringResource(R.string.folder_pick_empty), modifier = Modifier.padding(vertical = 8.dp))
                }
                folders.forEach { f ->
                    val checked = f.id in selected
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable { selected = if (checked) selected - f.id else selected + f.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { selected = if (it) selected + f.id else selected - f.id })
                        Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                TextButton(onClick = onNewFolder) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.fav_new_folder), modifier = Modifier.padding(start = 6.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(selected) }) { Text(stringResource(R.string.dialog_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}
