package com.tohn95.internetradio.ui.screens.genres

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tohn95.internetradio.R

/**
 * Жанр с картинкой. [tag] уходит в radio-browser как есть: поиск по тегу там частичный,
 * поэтому «game» ловит и «video game music», и «games».
 * Картинки — CC0 (Openverse), источники в docs/genre-images-credits.txt.
 */
data class Genre(val tag: String, @StringRes val title: Int, @DrawableRes val image: Int)

object GenreCatalog {
    val all: List<Genre> = listOf(
        Genre("jazz", R.string.genre_jazz, R.drawable.genre_jazz),
        Genre("game", R.string.genre_game, R.drawable.genre_game),
        Genre("anime", R.string.genre_anime, R.drawable.genre_anime),
        Genre("rock", R.string.genre_rock, R.drawable.genre_rock),
        Genre("pop", R.string.genre_pop, R.drawable.genre_pop),
        Genre("electronic", R.string.genre_electronic, R.drawable.genre_electronic),
        Genre("hip hop", R.string.genre_hiphop, R.drawable.genre_hiphop),
        Genre("classical", R.string.genre_classical, R.drawable.genre_classical),
        Genre("chillout", R.string.genre_chillout, R.drawable.genre_chillout),
        Genre("lounge", R.string.genre_lounge, R.drawable.genre_lounge),
        Genre("dance", R.string.genre_dance, R.drawable.genre_dance),
        Genre("80s", R.string.genre_eighties, R.drawable.genre_eighties),
        Genre("metal", R.string.genre_metal, R.drawable.genre_metal),
        Genre("news", R.string.genre_news, R.drawable.genre_news),
        Genre("blues", R.string.genre_blues, R.drawable.genre_blues),
        Genre("lofi", R.string.genre_lofi, R.drawable.genre_lofi),
        Genre("ambient", R.string.genre_ambient, R.drawable.genre_ambient),
        Genre("country", R.string.genre_country, R.drawable.genre_country),
        Genre("soundtrack", R.string.genre_soundtrack, R.drawable.genre_soundtrack),
        Genre("reggae", R.string.genre_reggae, R.drawable.genre_reggae),
    )

    /** Быстрые жанры по умолчанию: джаз, игровая, аниме и самые популярные. */
    val quick: List<Genre> = all.take(6)
}
