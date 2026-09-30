package com.tohn95.internetradio.ui.screens.favorites

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.tohn95.internetradio.ui.components.stationsCount
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
import com.tohn95.internetradio.playback.PlayerController
import com.tohn95.internetradio.ui.components.LocalBottomBarInset
import com.tohn95.internetradio.ui.components.MetaChipRow
import com.tohn95.internetradio.ui.components.StationImage
import com.tohn95.internetradio.ui.components.marquee
import com.tohn95.internetradio.ui.components.rememberCurrentUuid
import com.tohn95.internetradio.ui.theme.LocalPalette

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
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    var openFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dialog by remember { mutableStateOf<FavDialog?>(null) }
    val openFolder = folders.firstOrNull { it.id == openFolderId }
    val play: (Station) -> Unit = { playerController?.play(it) }
    val currentUuid = rememberCurrentUuid(playerController)

    BackHandler(enabled = openFolder != null) { openFolderId = null }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = if (openFolder != null) 4.dp else 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (openFolder != null) {
                IconButton(onClick = { openFolderId = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = palette.text)
                }
            }
            Text(
                openFolder?.name ?: stringResource(R.string.fav_title), color = palette.text, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (openFolder == null) {
                AddMenu(onNewFolder = { dialog = FavDialog.Create() }, onCustom = { dialog = FavDialog.Custom })
            } else {
                FolderMenu(
                    onRename = { dialog = FavDialog.Rename(openFolder) },
                    onDelete = { dialog = FavDialog.Delete(openFolder) },
                )
            }
        }

        val listPadding = PaddingValues(bottom = 12.dp + LocalBottomBarInset.current)
        if (openFolder == null) {
            if (favorites.isEmpty() && folders.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.empty_favorites), color = palette.textMuted)
                }
            } else LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding) {
                item(key = "folders_h") { SubHeader(stringResource(R.string.fav_folders)) }
                items(folders, key = { "f${it.id}" }) { f -> FolderRow(f, onClick = { openFolderId = f.id }) }
                item(key = "new_folder") { NewFolderRow(onClick = { dialog = FavDialog.Create() }) }
                item(key = "stations_h") {
                    SubHeader(stringResource(R.string.fav_stations) + " · " + favorites.size)
                }
                items(favorites, key = { "s${it.uuid}" }) { s ->
                    FavoriteRow(
                        s, onPlay = { play(s) },
                        onAddToFolder = { dialog = FavDialog.Pick(s) },
                        onRemoveFavorite = { viewModel.removeFavorite(s) },
                        isCurrent = s.uuid == currentUuid,
                    )
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = listPadding) {
                item(key = "count") {
                    Text(
                        stationsCount(openFolder.stations.size),
                        color = palette.textMuted, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    )
                }
                if (openFolder.stations.isEmpty()) item(key = "empty") {
                    Text(stringResource(R.string.folder_empty), color = palette.textMuted, modifier = Modifier.padding(24.dp))
                }
                items(openFolder.stations, key = { it.uuid }) { s ->
                    FavoriteRow(
                        s, onPlay = { play(s) },
                        onAddToFolder = { dialog = FavDialog.Pick(s) },
                        onRemoveFromFolder = { viewModel.removeFromFolder(openFolder.id, s) },
                        onRemoveFavorite = { viewModel.removeFavorite(s) },
                        isCurrent = s.uuid == currentUuid,
                    )
                }
            }
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
                TextButton(onClick = {
                    viewModel.deleteFolder(d.folder.id)
                    if (openFolderId == d.folder.id) openFolderId = null
                    dialog = null
                }) { Text(stringResource(R.string.dialog_delete), color = palette.red) }
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

/** Обложка папки: мозаика 2×2 из логотипов (как плейлист Spotify), один логотип или значок папки. */
@Composable
private fun FolderCover(folder: Folder, size: Dp) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(10.dp)
    val logos = folder.stations.map { it.faviconUrl }
    when {
        logos.size >= 4 -> Column(Modifier.size(size).clip(shape)) {
            logos.take(4).chunked(2).forEach { row ->
                Row { row.forEach { StationImage(model = it, size = size / 2, shape = RoundedCornerShape(0.dp)) } }
            }
        }
        logos.isNotEmpty() -> StationImage(model = logos.first(), size = size, shape = shape)
        else -> Box(
            Modifier.size(size).clip(shape)
                .background(Brush.linearGradient(listOf(lerp(palette.primary, Color.Black, 0.35f), palette.primary))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Folder, null, tint = Color.White, modifier = Modifier.size(size * 0.45f))
        }
    }
}

@Composable
private fun FolderRow(folder: Folder, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FolderCover(folder, 56.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(folder.name, color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(
                    R.string.fav_folder_label,
                    stationsCount(folder.stations.size),
                ),
                color = palette.textMuted, fontSize = 13.sp,
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
) {
    val palette = LocalPalette.current
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
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
private fun NameDialog(
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
private fun PickFoldersDialog(
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
