package com.tohn95.internetradio.ui.screens.history

import com.tohn95.internetradio.domain.model.HistoryEntry
import com.tohn95.internetradio.domain.model.Station
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class HistoryGroupingTest {
    private fun st(uuid: String) = Station(uuid, "S$uuid", "http://x", null, emptyList(), "", "", 0, "", 0, 0, false)
    private fun at(date: String, hour: Int) =
        LocalDate.parse(date).atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test fun `groups into today yesterday and dates preserving order`() {
        val today = LocalDate.parse("2026-07-20")
        val entries = listOf(
            HistoryEntry(st("a"), at("2026-07-20", 9)),
            HistoryEntry(st("b"), at("2026-07-20", 8)),
            HistoryEntry(st("c"), at("2026-07-19", 23)),
            HistoryEntry(st("d"), at("2026-07-17", 12)),
        )
        val sections = groupByDay(entries, today, ZoneOffset.UTC)
        assertEquals(3, sections.size)
        assertEquals(DayLabel.Today, sections[0].label)
        assertEquals(listOf("a", "b"), sections[0].entries.map { it.station.uuid })
        assertEquals(DayLabel.Yesterday, sections[1].label)
        assertEquals(DayLabel.Other(LocalDate.parse("2026-07-17")), sections[2].label)
    }

    @Test fun `midnight boundary splits correctly`() {
        val today = LocalDate.parse("2026-07-20")
        val entries = listOf(
            HistoryEntry(st("x"), at("2026-07-20", 0)),   // 00:00 сегодня
            HistoryEntry(st("y"), at("2026-07-19", 23)),  // 23:00 вчера
        )
        val sections = groupByDay(entries, today, ZoneOffset.UTC)
        assertEquals(listOf(DayLabel.Today, DayLabel.Yesterday), sections.map { it.label })
    }

    @Test fun `empty input gives empty sections`() {
        assertEquals(0, groupByDay(emptyList(), LocalDate.parse("2026-07-20"), ZoneOffset.UTC).size)
    }
}
