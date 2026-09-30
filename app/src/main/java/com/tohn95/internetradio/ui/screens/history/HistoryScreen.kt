package com.tohn95.internetradio.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tohn95.internetradio.R
import com.tohn95.internetradio.playback.PlayerController
import com.tohn95.internetradio.ui.components.LocalBottomBarInset
import com.tohn95.internetradio.ui.components.SearchField
import com.tohn95.internetradio.ui.components.StationCard
import com.tohn95.internetradio.ui.components.rememberCurrentUuid
import com.tohn95.internetradio.ui.theme.LocalPalette
import android.text.format.DateFormat
import androidx.compose.ui.platform.LocalLocale
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    playerController: PlayerController? = null,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val history by viewModel.history.collectAsStateWithLifecycle()
    val currentUuid = rememberCurrentUuid(playerController)
    val zone = remember { ZoneId.systemDefault() }
    // «28 сентября», «September 28», «9月28日» — порядок и форма по правилам языка приложения.
    val locale = LocalLocale.current.platformLocale
    val dateFmt = remember(locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale)
    }
    var query by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.history_title), color = palette.text, fontSize = 26.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 10.dp),
        )
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.empty_history), color = palette.textMuted)
            }
            return@Column
        }
        SearchField(
            value = query, onValueChange = { query = it },
            placeholder = stringResource(R.string.history_search_hint),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        // Поиск по названию — фильтруем ДО группировки, чтобы пустые дни не показывали заголовок.
        val q = query.trim()
        val shown = if (q.isEmpty()) history else history.filter { it.station.name.contains(q, ignoreCase = true) }
        if (shown.isEmpty()) {
            Text(
                stringResource(R.string.empty_search), color = palette.textMuted,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
            return@Column
        }
        val sections = groupByDay(shown, LocalDate.now(zone), zone)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp + LocalBottomBarInset.current),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            sections.forEach { section ->
                item(key = "h-${section.label}") {
                    Text(
                        when (val l = section.label) {
                            is DayLabel.Today -> stringResource(R.string.day_today)
                            is DayLabel.Yesterday -> stringResource(R.string.day_yesterday)
                            is DayLabel.Other -> l.date.format(dateFmt)
                        },
                        color = palette.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                items(section.entries, key = { it.station.uuid }) { e ->
                    StationCard(
                        e.station, onClick = { playerController?.play(e.station) },
                        isCurrent = e.station.uuid == currentUuid,
                    )
                }
            }
        }
    }
}
