package com.tohn95.internetradio.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tohn95.internetradio.R
import com.tohn95.internetradio.util.countryDisplayName
import com.tohn95.internetradio.playback.PlayerController
import com.tohn95.internetradio.ui.components.CatalogError
import com.tohn95.internetradio.ui.components.FilterCapsule
import com.tohn95.internetradio.ui.components.LocalBottomBarInset
import com.tohn95.internetradio.ui.components.SearchField
import com.tohn95.internetradio.ui.components.ShimmerCard
import com.tohn95.internetradio.ui.components.StationCard
import com.tohn95.internetradio.ui.components.rememberCurrentUuid
import com.tohn95.internetradio.ui.screens.genres.GenreCard
import com.tohn95.internetradio.ui.screens.genres.GenreCatalog
import com.tohn95.internetradio.ui.theme.LocalPalette

private enum class FilterKind { COUNTRY, TAG, CODEC, BITRATE, SORT }

private val sortLabels = linkedMapOf(
    SortMode.LISTENERS_DESC to R.string.sort_listeners_desc,
    SortMode.LISTENERS_ASC to R.string.sort_listeners_asc,
    SortMode.VOTES_DESC to R.string.sort_votes_desc,
    SortMode.NAME_ASC to R.string.sort_name_asc,
    SortMode.NAME_DESC to R.string.sort_name_desc,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    playerController: PlayerController? = null,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val countryOptions by viewModel.countryOptions.collectAsStateWithLifecycle()
    val tagOptions by viewModel.tagOptions.collectAsStateWithLifecycle()
    val codecOptions by viewModel.codecOptions.collectAsStateWithLifecycle()
    val currentUuid = rememberCurrentUuid(playerController)

    var sheet by remember { mutableStateOf<FilterKind?>(null) }
    val searching = query.trim().length >= 3 || filters.active

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "title") {
            Text(
                stringResource(R.string.search_title), color = palette.text, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 10.dp),
            )
        }
        item(key = "field") {
            SearchField(
                value = query, onValueChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.search_hint),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "filters") {
            // FlowRow: выбранные фильтры переносятся на новую строку, а не уезжают за экран.
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterCapsule(
                    label = filters.countryName ?: stringResource(R.string.filter_country),
                    icon = Icons.Filled.Public,
                    selected = filters.countryCode != null,
                    onClick = { viewModel.loadOptions(); sheet = FilterKind.COUNTRY },
                    onClear = { viewModel.setCountry(null, null) },
                )
                // Для жанров из каталога показываем русское имя («Хип-хоп»), для прочих — тег как есть.
                val catalogGenre = filters.tag?.let { t -> GenreCatalog.all.firstOrNull { it.tag.equals(t, ignoreCase = true) } }
                val tagTitle = catalogGenre?.let { stringResource(it.title) } ?: filters.tag
                FilterCapsule(
                    label = tagTitle ?: stringResource(R.string.filter_tag),
                    icon = Icons.Filled.Sell,
                    selected = filters.tag != null,
                    onClick = { viewModel.loadOptions(); sheet = FilterKind.TAG },
                    onClear = { viewModel.setTag(null) },
                )
                FilterCapsule(
                    label = filters.codec ?: stringResource(R.string.filter_codec),
                    icon = Icons.Filled.GraphicEq,
                    selected = filters.codec != null,
                    onClick = { viewModel.loadOptions(); sheet = FilterKind.CODEC },
                    onClear = { viewModel.setCodec(null) },
                )
                FilterCapsule(
                    label = filters.bitrateMin?.let { stringResource(R.string.bitrate_from, it) }
                        ?: stringResource(R.string.filter_bitrate),
                    icon = Icons.Filled.Speed,
                    selected = filters.bitrateMin != null,
                    onClick = { sheet = FilterKind.BITRATE },
                    onClear = { viewModel.setBitrate(null) },
                )
                // Сортировка показывает текущий порядок; не по умолчанию — подсвечена и сбрасывается ✕.
                FilterCapsule(
                    label = stringResource(sortLabels.getValue(sort)),
                    icon = Icons.AutoMirrored.Filled.Sort,
                    selected = sort != SortMode.LISTENERS_DESC,
                    onClick = { sheet = FilterKind.SORT },
                    onClear = { viewModel.setSort(SortMode.LISTENERS_DESC) },
                )
            }
        }
        when {
            loading -> items(List(5) { it }, key = { "sh$it" }) { ShimmerCard(Modifier.padding(vertical = 4.dp)) }
            searching && results.isEmpty() -> item(key = "empty") {
                if (error) {
                    CatalogError(onRetry = viewModel::retry, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.empty_search), color = palette.textMuted)
                    }
                }
            }
            results.isNotEmpty() -> itemsIndexed(results, key = { _, s -> s.uuid }) { index, s ->
                if (index == results.lastIndex) LaunchedEffect(results.size) { viewModel.loadMore() }
                StationCard(
                    s, onClick = { playerController?.play(s) },
                    isCurrent = s.uuid == currentUuid,
                    showListeners = true,
                )
            }
            else -> {
                // Пока ничего не ищем — не пустота, а сетка жанров (как у radioMii): тап ставит фильтр жанра.
                if (query.isNotBlank()) item(key = "min") {
                    Text(
                        stringResource(R.string.search_min_letters), color = palette.textMuted, fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
                item(key = "genres_h") {
                    Text(
                        stringResource(R.string.search_browse_genres), color = palette.text, fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
                    )
                }
                GenreCatalog.all.chunked(2).forEach { row ->
                    item(key = "g_" + row.first().tag) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { g ->
                                GenreCard(g, height = 92.dp, onClick = { viewModel.setTag(g.tag) }, modifier = Modifier.weight(1f))
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    sheet?.let { kind ->
        val options: List<PickerOption> = when (kind) {
            FilterKind.COUNTRY -> countryOptions.map { c ->
                val name = countryDisplayName(c.code, c.name)
                PickerOption(name, c.stationCount) { viewModel.setCountry(c.code, name) }
            }
            FilterKind.TAG -> tagOptions.map { t ->
                PickerOption(t.name, t.stationCount) { viewModel.setTag(t.name) }
            }
            FilterKind.CODEC -> codecOptions.map { c ->
                PickerOption(c.name, c.stationCount) { viewModel.setCodec(c.name) }
            }
            FilterKind.BITRATE -> listOf(64, 96, 128, 192, 256, 320).map { b ->
                PickerOption(stringResource(R.string.bitrate_from, b), null) { viewModel.setBitrate(b) }
            }
            FilterKind.SORT -> sortLabels.map { (mode, res) -> PickerOption(stringResource(res), null) { viewModel.setSort(mode) } }
        }
        val onAll: () -> Unit = when (kind) {
            FilterKind.COUNTRY -> ({ viewModel.setCountry(null, null) })
            FilterKind.TAG -> ({ viewModel.setTag(null) })
            FilterKind.CODEC -> ({ viewModel.setCodec(null) })
            FilterKind.BITRATE -> ({ viewModel.setBitrate(null) })
            FilterKind.SORT -> ({ viewModel.setSort(SortMode.LISTENERS_DESC) })
        }
        val title = stringResource(
            when (kind) {
                FilterKind.COUNTRY -> R.string.filter_country
                FilterKind.TAG -> R.string.filter_tag
                FilterKind.CODEC -> R.string.filter_codec
                FilterKind.BITRATE -> R.string.filter_bitrate
                FilterKind.SORT -> R.string.filter_sort
            }
        )
        FilterPickerDialog(
            title = title,
            options = options,
            showCounts = kind == FilterKind.COUNTRY || kind == FilterKind.TAG || kind == FilterKind.CODEC,
            showAll = kind != FilterKind.SORT,
            onAll = onAll,
            onDismiss = { sheet = null },
        )
    }
}


/** Пункт полноэкранного пикера: имя, число станций (nullable — у битрейта/сортировки нет), выбор. */
private data class PickerOption(val name: String, val count: Int?, val select: () -> Unit)

/** Порядок сортировки внутри пикера (обе стороны — по количеству и по алфавиту). */
private enum class PickerSort(val label: Int) {
    COUNT_DESC(R.string.picker_count_desc),
    COUNT_ASC(R.string.picker_count_asc),
    NAME_ASC(R.string.picker_name_asc),
    NAME_DESC(R.string.picker_name_desc),
}

private fun formatCount(n: Int): String = "%,d".format(n).replace(',', ' ')

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterPickerDialog(
    title: String,
    options: List<PickerOption>,
    showCounts: Boolean,
    showAll: Boolean,
    onAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var filter by remember { mutableStateOf("") }
    var pickerSort by remember { mutableStateOf(PickerSort.COUNT_DESC) }   // по умолчанию — по количеству ↓

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = palette.pageBg) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(28.dp).clickable(onClick = onDismiss),
                    )
                    Text(
                        title, color = palette.text, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                if (showCounts) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PickerSort.entries) { ps ->
                            FilterChip(
                                selected = pickerSort == ps, onClick = { pickerSort = ps },
                                label = { Text(stringResource(ps.label), maxLines = 1) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                SearchField(
                    value = filter, onValueChange = { filter = it },
                    placeholder = stringResource(R.string.filter_search_hint),
                    modifier = Modifier.fillMaxWidth(),
                )
                val shown = options.filter { it.name.contains(filter, ignoreCase = true) }.let {
                    if (!showCounts) it else when (pickerSort) {
                        PickerSort.COUNT_DESC -> it.sortedByDescending { o -> o.count ?: 0 }
                        PickerSort.COUNT_ASC -> it.sortedBy { o -> o.count ?: 0 }
                        PickerSort.NAME_ASC -> it.sortedBy { o -> o.name.lowercase() }
                        PickerSort.NAME_DESC -> it.sortedByDescending { o -> o.name.lowercase() }
                    }
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
                    if (showAll) item {
                        Text(
                            stringResource(R.string.filter_all), color = palette.primary,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onAll(); onDismiss() }.padding(vertical = 14.dp),
                        )
                    }
                    // Список со счётчиками ещё не пришёл (или разово не догрузился) — не пустой экран.
                    if (showCounts && options.isEmpty()) item {
                        Text(
                            stringResource(R.string.picker_loading), color = palette.textMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                    items(shown) { o ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { o.select(); onDismiss() }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(o.name, color = palette.text, modifier = Modifier.weight(1f))
                            if (showCounts && o.count != null) {
                                Text(formatCount(o.count), color = palette.textMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
