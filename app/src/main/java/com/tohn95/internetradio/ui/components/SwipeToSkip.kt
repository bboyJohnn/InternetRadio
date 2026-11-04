package com.tohn95.internetradio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/** Сдвиг не дальше этого — касание, а не свайп (даже если палец «дёрнулся» быстро). */
private const val MIN_SWIPE_PX = 24f
private const val FLICK_VELOCITY = 1200f

/**
 * Решение по отпущенному свайпу: +1 — следующая станция, -1 — предыдущая, 0 — вернуть на место.
 * Свайп засчитывается, если протянули на треть ширины или быстро смахнули в ту же сторону.
 * Справа налево (арабский) «следующая» — там, куда читают дальше, то есть вправо.
 */
fun swipeDecision(offsetPx: Float, velocityPx: Float, widthPx: Float, rtl: Boolean): Int {
    if (abs(offsetPx) < MIN_SWIPE_PX) return 0
    val far = abs(offsetPx) >= widthPx / 3f
    val flick = abs(velocityPx) >= FLICK_VELOCITY && sign(velocityPx) == sign(offsetPx)
    if (!far && !flick) return 0
    val leftward = offsetPx < 0
    return if (leftward != rtl) +1 else -1
}

/**
 * Смахнуть влево/вправо — соседняя станция: карточка уезжает за край, приезжает с другой стороны.
 * Листать некуда ([enabled] = false) — тянется туго и пружинит обратно. Смещение читается
 * только в graphicsLayer, поэтому свайп не перекомпоновывает содержимое.
 */
@Composable
fun Modifier.swipeToSkip(enabled: Boolean, onSkip: (Int) -> Unit): Modifier {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val canSkip by rememberUpdatedState(enabled)
    val skip by rememberUpdatedState(onSkip)
    var width by remember { mutableIntStateOf(0) }

    return this
        .onSizeChanged { width = it.width }
        .pointerInput(rtl) {
            val tracker = VelocityTracker()
            detectHorizontalDragGestures(
                onDragStart = { tracker.resetTracking() },
                onDragEnd = {
                    val w = width.toFloat().coerceAtLeast(1f)
                    val dir = if (canSkip) swipeDecision(offset.value, tracker.calculateVelocity().x, w, rtl) else 0
                    scope.launch {
                        if (dir == 0) {
                            offset.animateTo(0f, spring())
                        } else {
                            val out = if (offset.value < 0) -w else w
                            offset.animateTo(out, tween(150))
                            skip(dir)
                            offset.snapTo(-out)
                            offset.animateTo(0f, tween(240))
                        }
                    }
                },
                onDragCancel = { scope.launch { offset.animateTo(0f, spring()) } },
                onHorizontalDrag = { change, delta ->
                    change.consume()
                    tracker.addPosition(change.uptimeMillis, change.position)
                    val step = if (canSkip) delta else delta * 0.3f
                    scope.launch { offset.snapTo(offset.value + step) }
                },
            )
        }
        .graphicsLayer {
            translationX = offset.value
            alpha = 1f - (abs(offset.value) / width.coerceAtLeast(1)).coerceIn(0f, 1f) * 0.6f
        }
}
