package com.tohn95.internetradio.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation.toRoute
import com.tohn95.internetradio.ui.components.AmbientBackground
import com.tohn95.internetradio.ui.components.LocalAmbientAnimated
import com.tohn95.internetradio.ui.components.LocalOnline
import com.tohn95.internetradio.ui.components.OfflinePill
import com.tohn95.internetradio.ui.components.ambient
import com.tohn95.internetradio.ui.components.rememberDominantColor
import com.tohn95.internetradio.ui.screens.genres.GenreCatalog
import com.tohn95.internetradio.ui.screens.home.HomeCatalog
import com.tohn95.internetradio.ui.theme.Oklch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tohn95.internetradio.R
import com.tohn95.internetradio.playback.MetadataState
import com.tohn95.internetradio.playback.PlayerController
import com.tohn95.internetradio.ui.components.FloatingNavBar
import com.tohn95.internetradio.ui.components.LocalBottomBarInset
import com.tohn95.internetradio.ui.components.NavItem
import com.tohn95.internetradio.ui.components.MiniPlayerBar
import com.tohn95.internetradio.ui.screens.favorites.FavoritesScreen
import com.tohn95.internetradio.ui.screens.genres.GenresScreen
import com.tohn95.internetradio.ui.screens.stationlist.StationListScreen
import com.tohn95.internetradio.ui.screens.history.HistoryScreen
import com.tohn95.internetradio.ui.screens.home.HomeScreen
import com.tohn95.internetradio.ui.screens.player.PlayerScreen
import com.tohn95.internetradio.ui.screens.search.SearchScreen
import com.tohn95.internetradio.ui.screens.settings.SettingsScreen
import com.tohn95.internetradio.ui.theme.LocalPalette
import kotlinx.coroutines.launch

@Composable
fun AppScaffold(playerController: PlayerController, wavesAnimated: Boolean) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val station by playerController.currentStation.collectAsStateWithLifecycle()
    val isPlaying by playerController.isPlaying.collectAsStateWithLifecycle()
    val playRequested by playerController.playRequested.collectAsStateWithLifecycle()
    val streamAlive by playerController.streamAlive.collectAsStateWithLifecycle()
    val metadata by playerController.metadata.collectAsStateWithLifecycle()
    val appViewModel: AppViewModel = hiltViewModel()
    val favorites by appViewModel.favoriteUuids.collectAsStateWithLifecycle()
    val online by appViewModel.online.collectAsStateWithLifecycle()

    val homeListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val topLevel: List<NavItem<Route>> = listOf(
        NavItem(Route.Home, Icons.Filled.Home, R.string.nav_home),
        NavItem(Route.Favorites, Icons.Filled.Favorite, R.string.nav_favorites),
        NavItem(Route.Search, Icons.Filled.Search, R.string.nav_search),
        NavItem(Route.History, Icons.Filled.History, R.string.nav_history),
        NavItem(Route.Settings, Icons.Filled.Settings, R.string.nav_settings),
    )
    val currentDest = backStack?.destination
    val palette = LocalPalette.current
    // На полном плеере мини-плеер лишний — там и так всё управление (и информация о станции ниже).
    val onPlayerScreens = currentDest?.hasRoute(Route.Player::class) == true

    Scaffold(
        containerColor = palette.pageBg,
        // Не резервируем место под статус-бар — контент уходит под него (баннер-волна как фон).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Плавающая зона: карточка мини-плеера + «пилюля» навигации над жестовой полосой.
            Column(
                Modifier
                    // Мягкое затемнение снизу (как в Spotify): контент уходит под панели и тает, а не мельтешит.
                    .background(Brush.verticalGradient(listOf(Color.Transparent, palette.pageBg.copy(alpha = 0.9f), palette.pageBg)))
                    .navigationBarsPadding().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnimatedVisibility(visible = station != null && !onPlayerScreens) {
                    station?.let { s ->
                        MiniPlayerBar(
                            station = s, isPlaying = isPlaying, playRequested = playRequested, streamAlive = streamAlive,
                            // Многие станции шлют в ICY своё же название — не дублируем его второй строкой.
                            trackText = (metadata as? MetadataState.Available)?.text
                                ?.takeIf { !it.trim().equals(s.name.trim(), ignoreCase = true) },
                            isFavorite = s.uuid in favorites,
                            onToggleFavorite = { appViewModel.toggleFavorite(s) },
                            onToggle = { playerController.togglePlayPause() },
                            onOpen = { nav.navigate(Route.Player) { launchSingleTop = true } },
                        )
                    }
                }
                FloatingNavBar(
                    items = topLevel,
                    isSelected = { route ->
                        currentDest?.hasRoute(route::class) == true ||
                            // Жанры и списки открываются с Главной — подсвечиваем её вкладку.
                            (route == Route.Home && (currentDest?.hasRoute(Route.Genres::class) == true ||
                                currentDest?.hasRoute(Route.StationList::class) == true))
                    },
                    onClick = { route ->
                        if (route == Route.Home && currentDest?.hasRoute(Route.Home::class) == true) {
                            scope.launch { homeListState.animateScrollToItem(0) }
                        } else {
                            nav.navigate(route) {
                                // Всегда чистим стек до корня (Home): детальный экран
                                // (Player/StationInfo) над вкладкой сбрасывается, вкладка
                                // открывается на своём корне. saveState/restoreState НЕ
                                // используем — они воскрешали Player/StationInfo и путали
                                // вкладки (тап «Главная» уводил на StationInfo/Поиск).
                                popUpTo(nav.graph.startDestinationId) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        val inset = padding.calculateBottomPadding()
        // Контент идёт ПОД плавающую панель; списки сами добавляют отступ LocalBottomBarInset.
        // Базовый оттенок темы: от него считаются «свои» цвета переливов каждого экрана.
        val h = remember(palette.primary) {
            Oklch.fromSrgb(
                (palette.primary.red * 255).roundToInt(), (palette.primary.green * 255).roundToInt(),
                (palette.primary.blue * 255).roundToInt(),
            ).third
        }
        CompositionLocalProvider(
            LocalBottomBarInset provides inset, LocalAmbientAnimated provides wavesAnimated, LocalOnline provides online,
        ) {
          Box(Modifier.fillMaxSize()) {
            NavHost(nav, startDestination = Route.Home) {
                composable<Route.Home> {
                    HomeScreen(
                        playerController = playerController,
                        wavesAnimated = wavesAnimated,
                        listState = homeListState,
                        onOpenGenres = { nav.navigate(Route.Genres) { launchSingleTop = true } },
                        onOpenList = { nav.navigate(it) },
                        onOpenFavorites = {
                            nav.navigate(Route.Favorites) {
                                popUpTo(nav.graph.startDestinationId) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                    )
                }
                // Каждый экран — со своей бледной «подложкой-переливом» (у всех разные оттенки).
                composable<Route.Genres> {
                    ScreenFrame(listOf(palette.ambient(320.0), palette.ambient(190.0), palette.ambient(h))) {
                        GenresScreen(
                            onBack = { nav.popBackStack() },
                            onOpenGenre = { tag, title -> nav.navigate(Route.StationList(ListKind.TAG.name, title, tag)) },
                        )
                    }
                }
                composable<Route.StationList> { entry ->
                    ScreenFrame(stationListColors(entry.toRoute<Route.StationList>(), h)) {
                        StationListScreen(onBack = { nav.popBackStack() }, playerController = playerController)
                    }
                }
                composable<Route.Search> {
                    ScreenFrame(listOf(palette.ambient(h + 35), palette.ambient(h - 30))) {
                        SearchScreen(playerController = playerController)
                    }
                }
                composable<Route.Favorites> {
                    // Тёплый красный — цвет сердечка.
                    ScreenFrame(listOf(palette.ambient(15.0), palette.ambient(h), palette.ambient(345.0))) {
                        FavoritesScreen(playerController = playerController)
                    }
                }
                composable<Route.History> {
                    ScreenFrame(listOf(palette.ambient(h - 70), palette.ambient(h + 15))) {
                        HistoryScreen(playerController = playerController)
                    }
                }
                // Экраны без ленивого списка просто не заходят под панель. Настройки — едва заметная радуга.
                composable<Route.Settings> {
                    ScreenFrame(listOf(palette.ambient(h), palette.ambient(h + 120), palette.ambient(h + 240)), bottomInset = inset) {
                        SettingsScreen()
                    }
                }
                composable<Route.Player> {
                    // Без statusBarsPadding: фон плеера (цвет логотипа) уходит под статус-бар, отступ — внутри экрана.
                    Box(Modifier.fillMaxSize().padding(bottom = inset)) {
                        PlayerScreen(onClose = { nav.popBackStack() })
                    }
                }
            }
            // «Нет интернета» — пилюля поверх любого экрана, пока сети нет.
            AnimatedVisibility(
                visible = !online,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp),
            ) { OfflinePill() }
          }
        }
    }
}

/** Экран на своей «подложке-переливе»: фон уходит под статус-бар, контент — ниже него. */
@Composable
private fun ScreenFrame(colors: List<Color>, bottomInset: Dp = 0.dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AmbientBackground(colors)
        Box(Modifier.fillMaxSize().statusBarsPadding().padding(bottom = bottomInset)) { content() }
    }
}

/**
 * Цвета перелива списка — по его содержимому: жанр из каталога — из его фото, настроение/десятилетие —
 * их собственный оттенок, прочие теги и страны — стабильный оттенок из названия (у каждого свой).
 */
@Composable
private fun stationListColors(route: Route.StationList, h: Double): List<Color> {
    val palette = LocalPalette.current
    val arg = route.arg.orEmpty()
    val genre = GenreCatalog.all.firstOrNull { route.kind == ListKind.TAG.name && it.tag.equals(arg, ignoreCase = true) }
    val fromPhoto = rememberDominantColor(genre?.image)
    val hue: Double = when (route.kind) {
        ListKind.POPULAR.name -> h + 25
        ListKind.TREND.name -> 30.0
        ListKind.HQ.name -> h + 180
        else -> HomeCatalog.moods.firstOrNull { it.tag == arg }?.hue
            ?: HomeCatalog.decades.firstOrNull { it.tag == arg }?.hue
            ?: (abs(arg.lowercase().hashCode()) % 360).toDouble()
    }
    return listOf(fromPhoto?.copy(alpha = 1f) ?: palette.ambient(hue), palette.ambient(hue + 40))
}
