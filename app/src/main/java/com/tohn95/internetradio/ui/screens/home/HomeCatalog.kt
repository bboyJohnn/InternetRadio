package com.tohn95.internetradio.ui.screens.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tohn95.internetradio.R

/** Настроение для блока «Под настроение»: тег radio-browser, подпись, картинка, свой оттенок блока (OKLCH hue). */
data class Mood(val tag: String, @StringRes val title: Int, @DrawableRes val image: Int, val hue: Double)

/** Десятилетие для «Хитов десятилетий»: тег, крупная цифра на карточке, подпись (#80-е), оттенок. */
data class Decade(val tag: String, val big: String, val label: String, val hue: Double)

/** Страна для «Радио мира»: ISO-код, имя по-русски, флаг-эмодзи. */
data class WorldCountry(val code: String, val name: String, val flag: String)

object HomeCatalog {
    // Теги проверены по каталогу: в каждом от десятков до сотен живых станций.
    val moods: List<Mood> = listOf(
        Mood("relax", R.string.mood_relax, R.drawable.genre_chillout, 205.0),
        Mood("party", R.string.mood_party, R.drawable.genre_dance, 330.0),
        Mood("romantic", R.string.mood_romantic, R.drawable.genre_lounge, 15.0),
        Mood("meditation", R.string.mood_meditation, R.drawable.genre_ambient, 150.0),
        Mood("workout", R.string.mood_workout, R.drawable.genre_electronic, 45.0),
        Mood("instrumental", R.string.mood_instrumental, R.drawable.genre_classical, 285.0),
        Mood("sleep", R.string.mood_sleep, R.drawable.genre_lofi, 255.0),
        Mood("chill", R.string.mood_chill, R.drawable.genre_reggae, 115.0),
    )

    val decades: List<Decade> = listOf(
        Decade("60s", "60", "60", 55.0),
        Decade("70s", "70", "70", 25.0),
        Decade("80s", "80", "80", 330.0),
        Decade("90s", "90", "90", 280.0),
        Decade("2000s", "00", "2000", 220.0),
    )
}

/** Флаг-эмодзи из ISO-кода страны («DE» → 🇩🇪); непонятный код → 🌐. */
fun flagEmoji(code: String): String {
    val c = code.uppercase()
    if (c.length != 2 || !c.all { it in 'A'..'Z' }) return "🌐"
    return c.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}
