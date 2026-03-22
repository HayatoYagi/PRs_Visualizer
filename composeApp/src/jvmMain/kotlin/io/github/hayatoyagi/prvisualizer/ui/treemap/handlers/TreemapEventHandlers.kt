package io.github.hayatoyagi.prvisualizer.ui.treemap.handlers

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.treemapMoveHandler(
    onMoveEvent: (position: Offset, dragging: Boolean) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Move) { event ->
    val position = event.changes.firstOrNull()?.position ?: return@onPointerEvent
    onMoveEvent(position, event.buttons.isPrimaryPressed)
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.treemapScrollHandler(
    onScrollEvent: (scrollY: Float) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Scroll) { event ->
    val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: return@onPointerEvent
    onScrollEvent(scrollY)
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.treemapReleaseHandler(
    onReleaseEvent: (position: Offset, uptimeMillis: Long) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Release) { event ->
    if (event.button != PointerButton.Primary) return@onPointerEvent
    val change = event.changes.firstOrNull() ?: return@onPointerEvent
    onReleaseEvent(change.position, change.uptimeMillis)
}
