package com.tohn95.internetradio.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import com.tohn95.internetradio.ui.screens.favorites.DragTarget
import com.tohn95.internetradio.ui.screens.favorites.ItemBounds
import com.tohn95.internetradio.ui.screens.favorites.resolveDragTarget

@Stable
class DragDropState(
    val listState: LazyListState,
    private val isFolder: (Any) -> Boolean,
    private val reorderAllowed: () -> Boolean,
) {
    var draggedKey by mutableStateOf<Any?>(null); private set
    var offsetY by mutableStateOf(0f); private set
    var target by mutableStateOf<DragTarget>(DragTarget.None); private set
    private var startCenter = 0f
    private var centerY = 0f
    private var scrollDirection = 0

    internal fun start(position: Offset): Boolean {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            position.y >= it.offset && position.y < it.offset + it.size
        } ?: return false
        draggedKey = item.key
        startCenter = item.offset + item.size / 2f
        centerY = startCenter
        offsetY = 0f
        target = DragTarget.None
        return true
    }

    internal fun drag(amount: Offset) {
        val key = draggedKey ?: return
        offsetY += amount.y
        centerY = startCenter + offsetY
        val info = listState.layoutInfo
        val bounds = info.visibleItemsInfo.map {
            ItemBounds(it.key, it.offset, it.offset + it.size, isFolder(it.key))
        }
        target = resolveDragTarget(key, isFolder(key), centerY.toInt(), bounds, reorderAllowed())
        scrollDirection = when {
            centerY < 64f -> -1
            centerY > info.viewportEndOffset - 64f -> 1
            else -> 0
        }
    }

    internal inline fun finish(
        onReorder: (Any, Any) -> Unit,
        onDropInto: (Any, Any) -> Unit,
    ) {
        val from = draggedKey
        when (val current = target) {
            is DragTarget.Reorder -> if (from != null) onReorder(from, current.toKey)
            is DragTarget.Into -> if (from != null) onDropInto(from, current.folderKey)
            DragTarget.None -> Unit
        }
        reset()
    }

    internal fun reset() {
        draggedKey = null
        offsetY = 0f
        target = DragTarget.None
        scrollDirection = 0
    }

    internal fun scrollDirection() = scrollDirection
}

@Composable
fun rememberDragDropState(
    listState: LazyListState,
    isFolder: (Any) -> Boolean,
    reorderAllowed: () -> Boolean,
): DragDropState {
    val currentIsFolder by rememberUpdatedState(isFolder)
    val currentReorderAllowed by rememberUpdatedState(reorderAllowed)
    val state = remember(listState) {
        DragDropState(listState, { key -> currentIsFolder(key) }, { currentReorderAllowed() })
    }
    LaunchedEffect(state) {
        while (true) {
            withFrameMillis { }
            val direction = state.scrollDirection()
            val canScroll = (direction < 0 && state.listState.canScrollBackward) ||
                (direction > 0 && state.listState.canScrollForward)
            if (state.draggedKey != null && canScroll) state.listState.scrollBy(direction * 12f)
        }
    }
    return state
}

@Composable
fun Modifier.dragDropContainer(
    state: DragDropState,
    onReorder: (Any, Any) -> Unit,
    onDropInto: (Any, Any) -> Unit,
): Modifier = composedWithHaptics(state, onReorder, onDropInto)

@Composable
private fun Modifier.composedWithHaptics(
    state: DragDropState,
    onReorder: (Any, Any) -> Unit,
    onDropInto: (Any, Any) -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    return pointerInput(state) {
        detectDragGesturesAfterLongPress(
            onDragStart = { if (state.start(it)) haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
            onDrag = { change, amount -> change.consume(); state.drag(amount) },
            onDragEnd = { state.finish(onReorder, onDropInto) },
            onDragCancel = state::reset,
        )
    }
}

fun Modifier.dragDropItem(state: DragDropState, key: Any): Modifier = graphicsLayer {
    if (state.draggedKey == key) {
        translationY = state.offsetY
        scaleX = 1.03f
        scaleY = 1.03f
        shadowElevation = 16f
        clip = false
    }
}.zIndex(if (state.draggedKey == key) 1f else 0f)
