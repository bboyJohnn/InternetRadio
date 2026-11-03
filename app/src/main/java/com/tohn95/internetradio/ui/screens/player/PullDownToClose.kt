package com.tohn95.internetradio.ui.screens.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Свайп вниз закрывает экран станции — только когда он прокручен в самый верх; иначе это обычная
 * прокрутка к «О станции». Протянули меньше 120dp — экран пружинит на место. Ставится ПЕРЕД verticalScroll.
 */
@Composable
fun Modifier.pullDownToClose(scroll: ScrollState, onClose: () -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { 120.dp.toPx() }
    val pull = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val close by rememberUpdatedState(onClose)
    val connection = remember(scroll, threshold) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Потянули вниз и повели обратно вверх — сначала убираем оттяжку, потом уже прокрутка.
                if (pull.value > 0f && available.y < 0f && source == NestedScrollSource.UserInput) {
                    val used = max(available.y, -pull.value)
                    scope.launch { pull.snapTo(pull.value + used) }
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && source == NestedScrollSource.UserInput && scroll.value == 0) {
                    scope.launch { pull.snapTo(pull.value + available.y * 0.5f) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pull.value <= 0f) return Velocity.Zero
                if (pull.value >= threshold) close() else pull.animateTo(0f, spring())
                return available
            }
        }
    }
    return this.nestedScroll(connection).graphicsLayer { translationY = pull.value }
}
