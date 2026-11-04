package com.tohn95.internetradio.ui.components

import com.tohn95.internetradio.ui.screens.favorites.DragTarget
import com.tohn95.internetradio.ui.screens.favorites.ItemBounds
import com.tohn95.internetradio.ui.screens.favorites.resolveDragTarget
import org.junit.Assert.assertEquals
import org.junit.Test

class DragTargetTest {
    private val items = listOf(
        ItemBounds("folder", 0, 100, true),
        ItemBounds("a", 100, 180, false),
        ItemBounds("b", 180, 260, false),
    )

    @Test fun station_over_folder_middle_goes_into() =
        assertEquals(DragTarget.Into("folder"), resolveDragTarget("a", false, 50, items, true))

    @Test fun station_over_folder_edge_is_none() =
        assertEquals(DragTarget.None, resolveDragTarget("a", false, 10, items, true))

    @Test fun station_past_neighbor_middle_reorders() =
        assertEquals(DragTarget.Reorder("b"), resolveDragTarget("a", false, 230, items, true))

    @Test fun reorder_disallowed_gives_none_for_station_over_station() =
        assertEquals(DragTarget.None, resolveDragTarget("a", false, 230, items, false))

    @Test fun folder_never_goes_into_folder() =
        assertEquals(DragTarget.Reorder("folder"), resolveDragTarget("other", true, 60, items, true))

    @Test fun folder_reorders_only_among_folders() =
        assertEquals(DragTarget.None, resolveDragTarget("other", true, 220, items, true))
}
