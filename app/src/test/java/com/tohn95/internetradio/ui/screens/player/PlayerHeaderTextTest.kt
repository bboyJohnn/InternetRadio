package com.tohn95.internetradio.ui.screens.player

import com.tohn95.internetradio.playback.PlaySource
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerHeaderTextTest {
    private val fallback = "Germany · news"

    @Test
    fun folder_shows_folder_name() {
        assertEquals(HeaderText(HeaderKind.FOLDER, "Rock"), headerText(PlaySource.Folder(3, "Rock"), fallback))
    }

    @Test
    fun favorites_and_history_show_station_line() {
        assertEquals(HeaderText(HeaderKind.FAVORITES, fallback), headerText(PlaySource.Favorites, fallback))
        assertEquals(HeaderText(HeaderKind.HISTORY, fallback), headerText(PlaySource.History, fallback))
    }

    @Test
    fun search_shows_query_or_station_line_when_blank() {
        assertEquals(HeaderText(HeaderKind.SEARCH, "jazz"), headerText(PlaySource.Search("jazz"), fallback))
        assertEquals(HeaderText(HeaderKind.SEARCH, fallback), headerText(PlaySource.Search(""), fallback))
    }

    @Test
    fun genre_and_section_show_their_titles() {
        assertEquals(HeaderText(HeaderKind.GENRE, "Rock"), headerText(PlaySource.Genre("Rock"), fallback))
        assertEquals(HeaderText(HeaderKind.SECTION, "Popular"), headerText(PlaySource.Section("Popular"), fallback))
    }

    @Test
    fun no_source_is_live_now() {
        assertEquals(HeaderText(HeaderKind.LIVE, fallback), headerText(PlaySource.None, fallback))
    }
}
