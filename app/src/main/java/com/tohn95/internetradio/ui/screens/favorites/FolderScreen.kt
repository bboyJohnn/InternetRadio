package com.tohn95.internetradio.ui.screens.favorites

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tohn95.internetradio.R
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.playback.PlayQueue
import com.tohn95.internetradio.playback.PlaySource
import com.tohn95.internetradio.playback.PlayerController
import com.tohn95.internetradio.ui.components.LocalBottomBarInset
import com.tohn95.internetradio.ui.components.MetaChipRow
import com.tohn95.internetradio.ui.components.StationImage
import com.tohn95.internetradio.ui.components.dragDropContainer
import com.tohn95.internetradio.ui.components.dragDropItem
import com.tohn95.internetradio.ui.components.marquee
import com.tohn95.internetradio.ui.components.rememberCurrentUuid
import com.tohn95.internetradio.ui.components.rememberDominantColor
import com.tohn95.internetradio.ui.components.rememberDragDropState
import com.tohn95.internetradio.ui.components.stationsCount
import com.tohn95.internetradio.ui.theme.LocalPalette

private sealed interface FolderDialog {
    data object Add : FolderDialog
    data object Rename : FolderDialog
    data object Delete : FolderDialog
    data class Move(val station: Station) : FolderDialog
    data class Create(val station: Station) : FolderDialog
}

@Composable
fun FolderScreen(
    folderId: Long,
    playerController: PlayerController,
    onBack: () -> Unit,
    viewModel: FolderViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.folder_share)
    val folder by viewModel.folder.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val currentUuid = rememberCurrentUuid(playerController)
    val currentSource by playerController.currentSource.collectAsStateWithLifecycle()
    val playRequested by playerController.playRequested.collectAsStateWithLifecycle()
    val shuffle by playerController.shuffle.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<FolderDialog?>(null) }
    var menu by remember { mutableStateOf(false) }
    val current = folder ?: return
    val stations = current.stations
    val source = PlaySource.Folder(folderId, current.name)
    val fromThisFolder = (currentSource as? PlaySource.Folder)?.id == folderId
    val coverModel = current.coverPath?.let { "file://$it" } ?: stations.firstOrNull()?.faviconUrl
    val dominant = rememberDominantColor(coverModel)
    val listState = rememberLazyListState()
    val dragState = rememberDragDropState(listState, { false }, { true })
    val pickCover = rememberCoverPicker(viewModel::changeCover)

    LaunchedEffect(source, stations) { playerController.updateQueue(source, stations) }

    fun play(station: Station) = playerController.play(station, PlayQueue(source, stations))
    fun share() {
        val text = buildString {
            append(current.name)
            stations.forEach { append("\n").append(it.name).append(" — ").append(it.homepage ?: it.streamUrl) }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, shareTitle))
    }

    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(lerp(palette.pageBg, dominant ?: palette.primary, 0.42f), palette.pageBg)),
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
            }
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))) {
                    Icon(Icons.Filled.MoreVert, stringResource(R.string.fav_more), tint = Color.White)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.folder_rename)) }, onClick = { menu = false; dialog = FolderDialog.Rename })
                    DropdownMenuItem(text = { Text(stringResource(R.string.folder_change_cover)) }, onClick = { menu = false; pickCover() })
                    if (current.coverPath != null) DropdownMenuItem(
                        text = { Text(stringResource(R.string.folder_reset_cover)) },
                        onClick = { menu = false; viewModel.resetCover() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.folder_delete), color = palette.red) },
                        onClick = { menu = false; dialog = FolderDialog.Delete },
                    )
                }
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            FolderCover(current, 200.dp, Modifier.clip(RoundedCornerShape(10.dp)).clickable { pickCover() })
            Text(
                current.name, color = palette.text, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            Text(stationsCount(stations.size), color = palette.textMuted, fontSize = 14.sp)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { dialog = FolderDialog.Add }) { Icon(Icons.Filled.Add, stringResource(R.string.folder_add_stations), tint = palette.text) }
            IconButton(onClick = { dialog = FolderDialog.Rename }) { Icon(Icons.Filled.Edit, stringResource(R.string.folder_rename), tint = palette.text) }
            IconButton(onClick = ::share, enabled = stations.isNotEmpty()) { Icon(Icons.Filled.Share, stringResource(R.string.folder_share), tint = palette.text) }
            Box(Modifier.weight(1f))
            IconButton(
                onClick = {
                    if (fromThisFolder) playerController.setShuffle(!shuffle)
                    else stations.randomOrNull()?.let { play(it); playerController.setShuffle(true) }
                },
                enabled = stations.isNotEmpty() || fromThisFolder,
            ) {
                Icon(
                    Icons.Filled.Shuffle, stringResource(R.string.folder_shuffle),
                    tint = if (fromThisFolder && shuffle) palette.primary else palette.textMuted,
                )
            }
            IconButton(
                onClick = {
                    if (fromThisFolder) playerController.togglePlayPause()
                    else stations.firstOrNull()?.let(::play)
                },
                enabled = stations.isNotEmpty() || fromThisFolder,
                modifier = Modifier.size(56.dp).clip(CircleShape).background(palette.primary),
            ) {
                Icon(
                    if (fromThisFolder && playRequested) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    stringResource(R.string.folder_play), tint = palette.pageBg, modifier = Modifier.size(32.dp),
                )
            }
        }
        if (stations.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.folder_empty), color = palette.textMuted)
                TextButton(onClick = { dialog = FolderDialog.Add }) { Text(stringResource(R.string.folder_add_stations)) }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).dragDropContainer(
                    state = dragState,
                    onReorder = { from, to -> viewModel.reorder(from.toString(), to.toString()) },
                    onDropInto = { _, _ -> },
                ),
                contentPadding = PaddingValues(bottom = 12.dp + LocalBottomBarInset.current),
            ) {
                items(stations, key = { it.uuid }) { station ->
                    FolderStationRow(
                        station = station,
                        isCurrent = station.uuid == currentUuid,
                        onPlay = { play(station) },
                        onMove = { dialog = FolderDialog.Move(station) },
                        onRemove = { viewModel.remove(station) },
                        modifier = Modifier.dragDropItem(dragState, station.uuid),
                    )
                }
            }
        }
    }

    when (val d = dialog) {
        FolderDialog.Add -> AddStationsDialog(
            favorites = favorites, initiallySelected = stations.mapTo(HashSet()) { it.uuid },
            onDismiss = { dialog = null },
            onDone = { viewModel.setStations(it); dialog = null },
        )
        FolderDialog.Rename -> NameDialog(
            title = stringResource(R.string.folder_rename), initial = current.name,
            confirm = stringResource(R.string.dialog_save), onDismiss = { dialog = null },
            onConfirm = { viewModel.rename(it); dialog = null },
        )
        FolderDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.folder_delete)) },
            text = { Text(stringResource(R.string.folder_delete_confirm, current.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(); dialog = null; onBack() }) {
                    Text(stringResource(R.string.dialog_delete), color = palette.red)
                }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.dialog_cancel)) } },
        )
        is FolderDialog.Move -> PickFoldersDialog(
            station = d.station, folders = folders, onDismiss = { dialog = null },
            onNewFolder = { dialog = FolderDialog.Create(d.station) },
            onDone = { viewModel.setFolders(d.station, it); dialog = null },
        )
        is FolderDialog.Create -> NameDialog(
            title = stringResource(R.string.fav_new_folder), initial = "",
            confirm = stringResource(R.string.dialog_create), onDismiss = { dialog = null },
            onConfirm = { viewModel.createFolder(it, d.station); dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun FolderStationRow(
    station: Station,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onMove: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().clickable(onClick = onPlay).padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationImage(station.faviconUrl, 52.dp, RoundedCornerShape(10.dp))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent) Icon(Icons.Filled.GraphicEq, null, tint = palette.primary, modifier = Modifier.size(18.dp))
                Text(
                    station.name, color = if (isCurrent) palette.primary else palette.text,
                    fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.marquee(),
                )
            }
            MetaChipRow(station, maxGenres = 2, modifier = Modifier.padding(top = 4.dp))
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.fav_more), tint = palette.textMuted) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.folder_move_to)) }, onClick = { menu = false; onMove() })
                DropdownMenuItem(text = { Text(stringResource(R.string.fav_remove_from_folder)) }, onClick = { menu = false; onRemove() })
            }
        }
    }
}
