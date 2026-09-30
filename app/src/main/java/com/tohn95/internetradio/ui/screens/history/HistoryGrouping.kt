package com.tohn95.internetradio.ui.screens.history

import com.tohn95.internetradio.domain.model.HistoryEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed interface DayLabel {
    data object Today : DayLabel
    data object Yesterday : DayLabel
    data class Other(val date: LocalDate) : DayLabel
}

data class DaySection(val label: DayLabel, val entries: List<HistoryEntry>)

/** Чистая группировка: список уже отсортирован по убыванию времени (DAO). */
fun groupByDay(entries: List<HistoryEntry>, today: LocalDate, zone: ZoneId): List<DaySection> =
    entries
        .groupBy { Instant.ofEpochMilli(it.playedAtMillis).atZone(zone).toLocalDate() }
        .toList()
        .sortedByDescending { it.first }
        .map { (date, list) ->
            val label = when (date) {
                today -> DayLabel.Today
                today.minusDays(1) -> DayLabel.Yesterday
                else -> DayLabel.Other(date)
            }
            DaySection(label, list)
        }
