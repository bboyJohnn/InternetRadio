package com.tohn95.internetradio.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.R
import com.tohn95.internetradio.ui.theme.LocalPalette

/** Идея RadioWave: мигающая красная точка = эфир жив; серая статичная = поток упал. */
@Composable
fun LiveDot(alive: Boolean, playing: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val active = alive && playing
    // Мигаем только в эфире; яркость читаем в слое — точка мигает, не пересобирая экран каждый кадр.
    val blink = if (active) {
        rememberInfiniteTransition(label = "live").animateFloat(
            initialValue = 1f, targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "live",
        )
    } else null
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(8.dp)
                .graphicsLayer { alpha = blink?.value ?: 1f }
                .background(if (alive) palette.red else palette.textMuted, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(if (alive) R.string.live else R.string.offline),
            fontSize = 11.sp,
            color = if (alive) palette.red else palette.textMuted,
        )
    }
}
