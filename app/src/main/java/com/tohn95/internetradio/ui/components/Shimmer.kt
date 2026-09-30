package com.tohn95.internetradio.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.tohn95.internetradio.ui.theme.LocalPalette

@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val x = rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "shimmer",
    )
    val base = palette.itemBg
    val colors = listOf(base.copy(0.4f), base.copy(0.6f), base.copy(1f), base.copy(0.6f), base.copy(0.4f))
    // Положение блика читаем при рисовании, а не при компоновке: карточки не пересобираются каждый кадр.
    Box(
        modifier.fillMaxWidth().height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(Brush.linearGradient(colors, start = Offset(x.value, 0f), end = Offset(x.value + 400f, 80f)))
            }
    )
}
