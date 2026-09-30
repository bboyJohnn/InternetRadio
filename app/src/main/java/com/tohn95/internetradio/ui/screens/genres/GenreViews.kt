package com.tohn95.internetradio.ui.screens.genres

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.R
import com.tohn95.internetradio.ui.theme.LocalPalette

/** Широкий баннер «Все жанры» с коллажем из трёх жанровых картинок (как «Your likes»). */
@Composable
fun AllGenresBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Коллаж меняется вместе со случайными жанрами Главной.
    images: List<Int> = listOf(R.drawable.genre_jazz, R.drawable.genre_anime, R.drawable.genre_game),
) {
    val palette = LocalPalette.current
    // Левый край темнее primary — белый текст читается в обеих темах.
    val deep = lerp(palette.primaryActive, Color.Black, 0.45f)
    Box(
        modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(deep, palette.primary)))
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.align(Alignment.CenterEnd).width(158.dp).height(96.dp)) {
            val slots = listOf(0.dp to -10f, 46.dp to 4f, 92.dp to 14f)
            val collage = images.take(3).zip(slots) { img, (x, angle) -> Triple(img, x, angle) }
            collage.forEachIndexed { i, (img, x, angle) ->
                Image(
                    painterResource(img), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.offset(x = x, y = if (i == 1) 14.dp else 22.dp)
                        .rotate(angle).size(60.dp)
                        .shadow(6.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)),
                )
            }
        }
        Column(Modifier.align(Alignment.CenterStart).padding(start = 18.dp, end = 160.dp)) {
            Text(stringResource(R.string.home_all_genres), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.home_all_genres_sub), color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp, maxLines = 2, lineHeight = 15.sp,
            )
        }
    }
}

/** Пилюля быстрого жанра для сетки на Главной: картинка слева + название. */
@Composable
fun GenrePill(genre: Genre, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(
        modifier.height(56.dp).clip(RoundedCornerShape(12.dp)).background(palette.cardBg).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(genre.image), null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp))
        Text(
            stringResource(genre.title), color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

/** Карточка жанра с фото для экрана «Жанры» (разновысокая сетка, как «Vibes»). */
@Composable
fun GenreCard(genre: Genre, height: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)) {
        Image(painterResource(genre.image), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))
            )
        )
        Text(
            stringResource(genre.title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        )
    }
}
