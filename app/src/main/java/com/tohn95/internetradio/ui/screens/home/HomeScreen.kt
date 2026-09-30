package com.tohn95.internetradio.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tohn95.internetradio.ui.components.LocalBottomBarInset
import com.tohn95.internetradio.R
import com.tohn95.internetradio.util.countryDisplayName
import com.tohn95.internetradio.domain.model.Station
import com.tohn95.internetradio.playback.PlayerController
import com.tohn95.internetradio.ui.components.AmbientAnchor
import com.tohn95.internetradio.ui.components.CatalogError
import com.tohn95.internetradio.ui.components.AmbientBackground
import com.tohn95.internetradio.ui.components.NewStationCard
import com.tohn95.internetradio.ui.components.ambient
import com.tohn95.internetradio.ui.components.baseHue
import com.tohn95.internetradio.ui.components.PopularCard
import com.tohn95.internetradio.ui.components.RecentTile
import com.tohn95.internetradio.ui.components.SectionHeader
import com.tohn95.internetradio.ui.components.ShimmerCard
import com.tohn95.internetradio.ui.components.StationTile
import com.tohn95.internetradio.ui.components.WaveBanner
import com.tohn95.internetradio.ui.navigation.ListKind
import com.tohn95.internetradio.ui.navigation.Route
import com.tohn95.internetradio.ui.screens.genres.AllGenresBanner
import com.tohn95.internetradio.ui.screens.genres.GenreCatalog
import com.tohn95.internetradio.ui.screens.genres.GenrePill
import com.tohn95.internetradio.ui.theme.LocalPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    playerController: PlayerController? = null,
    wavesAnimated: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
    onOpenGenres: () -> Unit = {},
    onOpenList: (Route.StationList) -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val favs by viewModel.favoriteUuids.collectAsStateWithLifecycle()
    var boost by remember { mutableIntStateOf(0) }
    val play: (Station) -> Unit = { boost++; playerController?.play(it) }
    val pullState = rememberPullToRefreshState()

    // Заголовки для маршрутов считаем здесь: внутри LazyListScope нет @Composable-контекста.
    val tPopular = stringResource(R.string.home_popular)
    val tTrend = stringResource(R.string.home_trending)
    val tLocal = stringResource(R.string.home_local, viewModel.countryName)
    val tHq = stringResource(R.string.home_hq)
    val tForYou = stringResource(R.string.home_for_you)

    // Кнопки «обновить» больше нет: список обновляется свайпом вниз от самого верха.
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = { boost++; viewModel.refresh() },
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = state.refreshing,
                containerColor = palette.cardBg,
                color = palette.primary,
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
            )
        },
    ) {
        // Перелив Главной — ниже середины: сверху и так цветной баннер с волнами.
        val h = remember(palette.primary) { palette.baseHue() }
        AmbientBackground(
            listOf(palette.ambient(h + 50), palette.ambient(h - 50), palette.ambient(h)),
            anchor = AmbientAnchor.LOWER,
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp + LocalBottomBarInset.current),
        ) {
            item(key = "banner") { HomeBanner(wavesAnimated, boost) }
            when {
                state.loading -> items(List(5) { it }, key = { "sh$it" }) {
                    ShimmerCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
                }
                // Нет интернета или каталог не отвечает — понятная карточка; Избранное играет и без каталога.
                state.error && state.popular.isEmpty() -> item(key = "error") {
                    CatalogError(
                        onRetry = { viewModel.refresh() }, onOpenFavorites = onOpenFavorites,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                else -> {
                    // 1. Жанры: баннер «Все жанры» + быстрые жанры 2×3.
                    item(key = "genres_h") {
                        SectionHeader(stringResource(R.string.home_genres), topPadding = 0.dp, onMore = onOpenGenres)
                    }
                    item(key = "genres") {
                        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 6 случайных жанров на каждое обновление; коллаж баннера — из них же.
                            AllGenresBanner(onClick = onOpenGenres, images = state.quickGenres.take(3).map { it.image })
                            state.quickGenres.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { g ->
                                        val title = stringResource(g.title)
                                        GenrePill(
                                            g, modifier = Modifier.weight(1f),
                                            onClick = { onOpenList(Route.StationList(ListKind.TAG.name, title, g.tag)) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Популярное 2×2 → полный список с сортировками.
                    if (state.popular.isNotEmpty()) {
                        item(key = "pop_h") {
                            SectionHeader(
                                tPopular,
                                badge = if (state.fromCache) stringResource(R.string.offline_short) else null,
                                onMore = { onOpenList(Route.StationList(ListKind.POPULAR.name, tPopular)) },
                            )
                        }
                        item(key = "pop") {
                            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.popular.chunked(2).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        row.forEach { s -> PopularCard(s, onClick = { play(s) }, modifier = Modifier.weight(1f)) }
                                        if (row.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // 3. «Мне повезёт» — случайная станция одним нажатием.
                    item(key = "lucky") {
                        LuckyBanner(
                            onClick = { boost++; viewModel.lucky { s -> playerController?.play(s) } },
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp),
                        )
                    }

                    // 4. Новая станция.
                    state.newStation?.let { s ->
                        item(key = "new_h") { SectionHeader(stringResource(R.string.home_new_station)) }
                        item(key = "new") {
                            NewStationCard(
                                s, isFavorite = s.uuid in favs,
                                onPlay = { play(s) }, onToggleFavorite = { viewModel.toggleFavorite(s) },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }

                    // 5. Для тебя (крупные плитки).
                    stationCarousel("foryou", tForYou, state.forYou, tile = 156.dp, onPlay = play)

                    // 6. Миксы жанров (фото + значок «МИКС» + полоса) → список жанра.
                    if (state.mixes.isNotEmpty()) {
                        item(key = "mixes_h") { SectionHeader(stringResource(R.string.home_mixes)) }
                        item(key = "mixes") {
                            val mixTitles = state.mixes.map { stringResource(R.string.home_mix_title, stringResource(it.genre.title)) }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.mixes.size, key = { "mix" + state.mixes[it].genre.tag }) { i ->
                                    val m = state.mixes[i]
                                    MixCard(m, onClick = { onOpenList(Route.StationList(ListKind.TAG.name, mixTitles[i], m.genre.tag)) })
                                }
                            }
                        }
                    }

                    // 7. Недавно слушал (круглые).
                    if (recent.isNotEmpty()) {
                        item(key = "recent_h") { SectionHeader(stringResource(R.string.home_recent)) }
                        item(key = "recent") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(recent, key = { "r" + it.uuid }) { s -> RecentTile(s, onClick = { play(s) }) }
                            }
                        }
                    }

                    // 8. В тренде — чарт с номерами 1–5.
                    if (state.trending.isNotEmpty()) {
                        item(key = "trend_h") {
                            SectionHeader(tTrend, onMore = { onOpenList(Route.StationList(ListKind.TREND.name, tTrend)) })
                        }
                        item(key = "trend") { ChartPanel(state.trending, onPlay = play, modifier = Modifier.padding(horizontal = 16.dp)) }
                    }

                    // 9. Под настроение — цветные блоки 2×2 (каждый раз 4 случайных).
                    if (state.moods.isNotEmpty()) {
                        item(key = "moods_h") { SectionHeader(stringResource(R.string.home_moods)) }
                        item(key = "moods") {
                            val moodTitles = state.moods.associate { it.tag to stringResource(it.title) }
                            MoodGrid(
                                state.moods, modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = { m -> onOpenList(Route.StationList(ListKind.TAG.name, moodTitles.getValue(m.tag), m.tag)) },
                            )
                        }
                    }

                    // 10. Эфир своей страны — обычные плитки (случайные 15 из топ-60).
                    stationCarousel("local", tLocal, state.local, onPlay = play,
                        onMore = { onOpenList(Route.StationList(ListKind.COUNTRY.name, tLocal, viewModel.countryCode)) })

                    // 11. Радио мира — круглые флаги (8 случайных стран).
                    if (state.world.isNotEmpty()) {
                        item(key = "world_h") { SectionHeader(stringResource(R.string.home_world)) }
                        item(key = "world") {
                            val worldTitles = state.world.associate { it.code to stringResource(R.string.home_local, countryDisplayName(it.code, it.name)) }
                            WorldRow(state.world, onClick = { c ->
                                onOpenList(Route.StationList(ListKind.COUNTRY.name, worldTitles.getValue(c.code), c.code))
                            })
                        }
                    }

                    // 12. Хиты десятилетий — высокие карточки с цифрой.
                    item(key = "decades_h") { SectionHeader(stringResource(R.string.home_decades)) }
                    item(key = "decades") {
                        val decadeTitles = HomeCatalog.decades.associate { it.tag to stringResource(R.string.decade_list_title, it.label) }
                        DecadeRow(HomeCatalog.decades, onClick = { d ->
                            onOpenList(Route.StationList(ListKind.TAG.name, decadeTitles.getValue(d.tag), d.tag))
                        })
                    }

                    // 13. Высокое качество — лента «пилюль» в два ряда (случайные из топ-150).
                    if (state.hq.isNotEmpty()) {
                        item(key = "hq_h") {
                            SectionHeader(tHq, onMore = { onOpenList(Route.StationList(ListKind.HQ.name, tHq)) })
                        }
                        item(key = "hq") { HqPillGrid(state.hq, onPlay = play) }
                    }
                }
            }
        }
        // Баннер уехал — под часами/значками статус-бара нужна подложка, иначе контент лезет под них.
        val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
        AnimatedVisibility(
            scrolled, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(palette.pageBg.copy(alpha = 0.94f)),
            )
        }
    }
}

/** Баннер-волна под статус-баром + скруглённый верх «листа», на котором лежит весь контент. */
@Composable
private fun HomeBanner(animated: Boolean, boost: Int) {
    val palette = LocalPalette.current
    Box {
        WaveBanner(
            title = stringResource(R.string.app_name), animated = animated, boostTrigger = boost,
            height = 124.dp, waveAlpha = 0.55f, wavesLift = 22.dp,
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(26.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(palette.pageBg),
        )
    }
}

private fun LazyListScope.stationCarousel(
    key: String,
    title: String,
    stations: List<Station>,
    onPlay: (Station) -> Unit,
    tile: Dp = 136.dp,
    onMore: (() -> Unit)? = null,
) {
    if (stations.isEmpty()) return
    item(key = "${key}_h") { SectionHeader(title, onMore = onMore) }
    item(key = key) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(stations, key = { key + it.uuid }) { s -> StationTile(s, onClick = { onPlay(s) }, size = tile) }
        }
    }
}
