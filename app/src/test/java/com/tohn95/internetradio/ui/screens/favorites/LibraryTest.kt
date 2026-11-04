package com.tohn95.internetradio.ui.screens.favorites

import com.tohn95.internetradio.domain.model.Folder
import com.tohn95.internetradio.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryTest {
    private fun st(id: String, name: String = id, tags: List<String> = emptyList()) = Station(
        uuid = id, name = name, streamUrl = "https://$id.test", faviconUrl = null,
        tags = tags, country = "", countryCode = "", bitrate = 128, codec = "MP3",
        votes = 0, clickCount = 0, isHls = false,
    )

    @Test fun filter_folders_hides_stations() {
        val result = buildLibrary(listOf(Folder(1, "Jazz", emptyList())), listOf(st("a")), emptyMap(), LibraryFilter.FOLDERS, FavSort.CUSTOM, "")
        assertEquals(1, result.folders.size)
        assertEquals(emptyList<Station>(), result.stations)
    }

    @Test fun filter_stations_hides_folders() {
        val result = buildLibrary(listOf(Folder(1, "Jazz", emptyList())), listOf(st("a")), emptyMap(), LibraryFilter.STATIONS, FavSort.CUSTOM, "")
        assertEquals(emptyList<Folder>(), result.folders)
        assertEquals(1, result.stations.size)
    }

    @Test fun sort_name_ignores_case() {
        val result = buildLibrary(emptyList(), listOf(st("1", "b"), st("2", "A"), st("3", "c")), emptyMap(), LibraryFilter.ALL, FavSort.NAME, "")
        assertEquals(listOf("A", "b", "c"), result.stations.map { it.name })
    }

    @Test fun sort_recent_newest_first_stable() {
        val folders = listOf(Folder(1, "old", emptyList(), createdAt = 1), Folder(2, "new", emptyList(), createdAt = 5))
        val stations = listOf(st("a"), st("b"), st("c"))
        val result = buildLibrary(folders, stations, mapOf("a" to 2L, "b" to 2L, "c" to 9L), LibraryFilter.ALL, FavSort.RECENT, "")
        assertEquals(listOf(2L, 1L), result.folders.map { it.id })
        assertEquals(listOf("c", "a", "b"), result.stations.map { it.uuid })
    }

    @Test fun query_matches_tag_and_folder_name() {
        val result = buildLibrary(
            listOf(Folder(1, "Evening Jazz", emptyList()), Folder(2, "Rock", emptyList())),
            listOf(st("a", "One", listOf("Smooth Jazz")), st("b", "Two", listOf("rock"))),
            emptyMap(), LibraryFilter.ALL, FavSort.CUSTOM, "jazz",
        )
        assertEquals(listOf(1L), result.folders.map { it.id })
        assertEquals(listOf("a"), result.stations.map { it.uuid })
    }

    @Test fun custom_keeps_given_order() {
        val result = buildLibrary(emptyList(), listOf(st("c"), st("a"), st("b")), mapOf("a" to 99L), LibraryFilter.ALL, FavSort.CUSTOM, "")
        assertEquals(listOf("c", "a", "b"), result.stations.map { it.uuid })
    }
}
