package com.example.myapplication.ui.util

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit,
) = remember(lazyListState, onMove) {
    ReorderableLazyListState(lazyListState, onMove)
}

class ReorderableLazyListState(
    val lazyListState: LazyListState,
    private val onMove: (from: Int, to: Int) -> Unit
) {
    var draggedDistance by mutableStateOf(0f)
    var currentlyDraggingItemIndex by mutableStateOf<Int?>(null)

    private val initialOffsets: Pair<Int, Int>? by lazy {
        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.index == currentlyDraggingItemIndex
        }?.let { Pair(it.offset, it.offset + it.size) }
    }

    private val currentDraggingItemOffset
        get() = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == currentlyDraggingItemIndex }?.offset?.toFloat() ?: 0f

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }?.index
            ?.also { currentlyDraggingItemIndex = it }
    }

    fun onDragInterrupted() {
        draggedDistance = 0f
        currentlyDraggingItemIndex = null
    }

    fun onDrag(dragAmount: Offset) {
        draggedDistance += dragAmount.y

        val topOffset = initialOffsets?.first?.plus(draggedDistance)?.toInt() ?: 0
        val bottomOffset = initialOffsets?.second?.plus(draggedDistance)?.toInt() ?: 0

        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull {
            val isMe = it.index == currentlyDraggingItemIndex
            !isMe && topOffset in it.offset..(it.offset + it.size) || !isMe && bottomOffset in it.offset..(it.offset + it.size)
        }?.also { hovered ->
            currentlyDraggingItemIndex?.let { current ->
                onMove(current, hovered.index)
                draggedDistance = 0f
            }
        }
    }
}

fun Modifier.reorderable(state: ReorderableLazyListState, coroutineScope: CoroutineScope): Modifier {
    return pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
            onDragStart = state::onDragStart,
            onDragEnd = state::onDragInterrupted,
            onDragCancel = state::onDragInterrupted,
            onDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount)

                coroutineScope.launch {
                    state.lazyListState.scrollBy(dragAmount.y)
                }
            }
        )
    }
}

@Composable
fun LazyItemScope.DraggableItem(state: ReorderableLazyListState, index: Int, modifier: Modifier, content: @Composable (isDragging: Boolean) -> Unit) {
    val isDragging = index == state.currentlyDraggingItemIndex
    val draggingModifier = if (isDragging) {
        Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = state.draggedDistance }
    } else {
        Modifier.zIndex(0f)
    }
    Column(modifier = modifier.then(draggingModifier)) {
        content(isDragging)
    }
}
