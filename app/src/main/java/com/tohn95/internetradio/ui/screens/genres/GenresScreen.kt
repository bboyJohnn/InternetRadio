package com.tohn95.internetradio.ui.screens.genres

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tohn95.internetradio.ui.components.LocalBottomBarInset
import com.tohn95.internetradio.R
import com.tohn95.internetradio.data.FilterCatalog
import com.tohn95.internetradio.domain.model.TagOption
import com.tohn95.internetradio.ui.components.SearchField
import com.tohn95.internetradio.ui.components.formatShort
import com.tohn95.internetradio.ui.theme.LocalPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenresViewModel @Inject constructor(private val catalog: FilterCatalog) : ViewModel() {
    private val _tags = MutableStateFlow<List<TagOption>>(emptyList())
    val tags: StateFlow<List<TagOption>> = _tags

    // distinctBy — ключи сетки должны быть уникальны, даже если API вернёт дубль.
    init { viewModelScope.launch { _tags.value = catalog.tags().distinctBy { it.name } } }
}

/** Разновысокие карточки дают «мозаику», как на референсе Vibes. */
private val cardHeights = listOf(196.dp, 148.dp, 148.dp, 196.dp)

@Composable
fun GenresScreen(
    onBack: () -> Unit,
    onOpenGenre: (tag: String, title: String) -> Unit,
    viewModel: GenresViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    val q = query.trim()
    val genres = GenreCatalog.all.map { it to stringResource(it.title) }
        .filter { (g, title) -> q.isEmpty() || title.contains(q, true) || g.tag.contains(q, true) }
    val shownTags = tags.filter { q.isEmpty() || it.name.contains(q, true) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = palette.text)
            }
            Text(stringResource(R.string.genres_title), color = palette.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        SearchField(
            value = query, onValueChange = { query = it },
            placeholder = stringResource(R.string.genres_search_hint),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp + LocalBottomBarInset.current),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
        ) {
            itemsIndexed(genres, key = { _, p -> "g" + p.first.tag }) { i, (g, title) ->
                GenreCard(g, height = cardHeights[i % cardHeights.size], onClick = { onOpenGenre(g.tag, title) })
            }
            if (shownTags.isNotEmpty()) {
                item(key = "all_h", span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        stringResource(R.string.genres_all_tags), color = palette.text, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    )
                }
            }
            items(shownTags, key = { "t" + it.name }, span = { StaggeredGridItemSpan.FullLine }) { t ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { onOpenGenre(t.name, t.name.replaceFirstChar { it.uppercase() }) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        t.name, color = palette.text, fontSize = 15.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.padding(4.dp))
                    Text(formatShort(t.stationCount), color = palette.textMuted, fontSize = 13.sp)
                }
            }
        }
    }
}
