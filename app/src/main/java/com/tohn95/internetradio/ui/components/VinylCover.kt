package com.tohn95.internetradio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tohn95.internetradio.ui.theme.LocalPalette

@Composable
fun VinylCover(faviconUrl: String?, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {   // непрерывное вращение: 360° за 6 секунд
                rotation.animateTo(rotation.value + 360f, tween(6000, easing = LinearEasing))
            }
        } else {
            rotation.animateTo(rotation.value + 15f, tween(500, easing = LinearOutSlowInEasing))
        }
    }

    Box(
        modifier.size(240.dp)
            // Угол читаем в слое (лямбда), а не при компоновке: вращение не пересобирает экран 60 раз в секунду.
            .graphicsLayer { rotationZ = rotation.value % 360f }
            .clip(CircleShape).background(Color(0xFF1a1a1a)),
        contentAlignment = Alignment.Center,
    ) {
        StationImage(model = faviconUrl, size = 96.dp, shape = CircleShape)
        Box(Modifier.size(10.dp).clip(CircleShape).background(palette.pageBg))
    }
}
