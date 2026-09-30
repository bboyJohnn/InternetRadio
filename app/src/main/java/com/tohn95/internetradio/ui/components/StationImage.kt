package com.tohn95.internetradio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.tohn95.internetradio.ui.theme.LocalPalette

/**
 * Логотип станции с плейсхолдером-радио: значок виден, пока картинки нет
 * (грузится, ошибка, у станции нет логотипа). Обычный AsyncImage, без подкомпоновки
 * (SubcomposeAsyncImage) — в длинных списках это заметно дешевле при прокрутке.
 */
@Composable
fun StationImage(
    model: String?,
    size: Dp,
    shape: Shape = RoundedCornerShape(8.dp),
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    var loaded by remember(model) { mutableStateOf(false) }
    Box(modifier.size(size).clip(shape).background(palette.btnBg)) {
        if (!loaded) {
            Icon(
                Icons.Filled.Radio, contentDescription = null,
                tint = palette.textMuted,
                modifier = Modifier.fillMaxSize().padding(size / 5),
            )
        }
        if (!model.isNullOrBlank()) {
            AsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                onState = { loaded = it is AsyncImagePainter.State.Success },
            )
        }
    }
}
