package io.github.hayatoyagi.prvisualizer.ui.treemap.handlers

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.treemapMoveHandler(
    isLoading: Boolean,
    onMoveEvent: (position: Offset, dragging: Boolean) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Move) { event ->
    if (isLoading) return@onPointerEvent
    val position = event.changes.firstOrNull()?.position ?: return@onPointerEvent
    onMoveEvent(position, event.buttons.isPrimaryPressed)
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.treemapScrollHandler(
    isLoading: Boolean,
    onScrollEvent: (scrollY: Float) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Scroll) { event ->
    if (isLoading) return@onPointerEvent
    val scrollDelta = event.changes.firstOrNull()?.scrollDelta ?: return@onPointerEvent
    // Support both vertical wheel and horizontal pinch deltas.
    val scrollY = if (abs(scrollDelta.x) > abs(scrollDelta.y)) scrollDelta.x else scrollDelta.y
    if (scrollY == 0f) return@onPointerEvent
    onScrollEvent(scrollY)
}

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.treemapReleaseHandler(
    isLoading: Boolean,
    onReleaseEvent: (position: Offset, uptimeMillis: Long) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Release) { event ->
    if (isLoading || event.button != PointerButton.Primary) return@onPointerEvent
    val change = event.changes.firstOrNull() ?: return@onPointerEvent
    onReleaseEvent(change.position, change.uptimeMillis)
}
