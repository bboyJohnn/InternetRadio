package com.tohn95.internetradio.ui.screens.favorites

import com.tohn95.internetradio.domain.model.Folder
import com.tohn95.internetradio.domain.model.Station
import java.util.Locale

enum class LibraryFilter { ALL, FOLDERS, STATIONS }
enum class FavSort { CUSTOM, NAME, RECENT }

data class Library(val folders: List<Folder>, val stations: List<Station>) {
    val isEmpty: Boolean get() = folders.isEmpty() && stations.isEmpty()
}

fun buildLibrary(
    folders: List<Folder>,
    stations: List<Station>,
    addedAt: Map<String, Long>,
    filter: LibraryFilter,
    sort: FavSort,
    query: String,
): Library {
    val needle = query.trim().lowercase(Locale.ROOT)
    fun matchesFolder(folder: Folder) = needle.isEmpty() || folder.name.lowercase(Locale.ROOT).contains(needle)
    fun matchesStation(station: Station) = needle.isEmpty() ||
        station.name.lowercase(Locale.ROOT).contains(needle) ||
        station.tags.any { it.lowercase(Locale.ROOT).contains(needle) }

    val visibleFolders = if (filter == LibraryFilter.STATIONS) emptyList() else folders.filter(::matchesFolder)
    val visibleStations = if (filter == LibraryFilter.FOLDERS) emptyList() else stations.filter(::matchesStation)

    val sortedFolders = when (sort) {
        FavSort.CUSTOM -> visibleFolders
        FavSort.NAME -> visibleFolders.sortedBy { it.name.lowercase(Locale.ROOT) }
        FavSort.RECENT -> visibleFolders.sortedByDescending { it.createdAt }
    }
    val sortedStations = when (sort) {
        FavSort.CUSTOM -> visibleStations
        FavSort.NAME -> visibleStations.sortedBy { it.name.lowercase(Locale.ROOT) }
        FavSort.RECENT -> visibleStations.sortedByDescending { addedAt[it.uuid] ?: 0L }
    }
    return Library(sortedFolders, sortedStations)
}

data class ItemBounds(val key: Any, val top: Int, val bottom: Int, val isFolder: Boolean)

sealed interface DragTarget {
    data class Reorder(val toKey: Any) : DragTarget
    data class Into(val folderKey: Any) : DragTarget
    data object None : DragTarget
}

fun resolveDragTarget(
    draggedKey: Any,
    draggedIsFolder: Boolean,
    centerY: Int,
    items: List<ItemBounds>,
    reorderAllowed: Boolean,
): DragTarget {
    val item = items.firstOrNull { it.key != draggedKey && centerY >= it.top && centerY < it.bottom }
        ?: return DragTarget.None
    val middle = (item.top + item.bottom) / 2

    if (draggedIsFolder) {
        return if (reorderAllowed && item.isFolder && centerY > middle) DragTarget.Reorder(item.key) else DragTarget.None
    }
    if (item.isFolder) {
        val height = item.bottom - item.top
        val inMiddleHalf = centerY >= item.top + height / 4 && centerY <= item.bottom - height / 4
        return if (inMiddleHalf) DragTarget.Into(item.key) else DragTarget.None
    }
    return if (reorderAllowed && centerY > middle) DragTarget.Reorder(item.key) else DragTarget.None
}
