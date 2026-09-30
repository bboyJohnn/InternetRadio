package com.tohn95.internetradio.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Home : Route
    @Serializable data object Search : Route
    @Serializable data object Favorites : Route
    @Serializable data object History : Route
    @Serializable data object Settings : Route
    @Serializable data object Player : Route
    @Serializable data object Genres : Route
    /** Полный список станций с сортировками: «Популярное», жанр, тренды, страна, HQ. */
    @Serializable data class StationList(val kind: String, val title: String, val arg: String? = null) : Route
}

/** Что показывает [Route.StationList]; имя enum уходит в маршрут строкой. */
enum class ListKind { POPULAR, TAG, TREND, COUNTRY, HQ }
