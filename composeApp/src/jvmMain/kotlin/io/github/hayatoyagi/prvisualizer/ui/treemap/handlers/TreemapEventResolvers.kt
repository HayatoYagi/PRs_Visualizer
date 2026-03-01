package io.github.hayatoyagi.prvisualizer.ui.treemap.handlers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.hayatoyagi.prvisualizer.TreemapNode
import io.github.hayatoyagi.prvisualizer.ui.treemap.models.MoveEventResult
import io.github.hayatoyagi.prvisualizer.ui.treemap.models.ReleaseEventResult
import io.github.hayatoyagi.prvisualizer.ui.treemap.models.ZoomEventResult
import io.github.hayatoyagi.prvisualizer.ui.treemap.nodeKey

internal fun resolveMoveEvent(
    position: Offset,
    dragging: Boolean,
    pan: Offset,
    dragPointerPos: Offset?,
    zoom: Float,
    visibleNodes: List<TreemapNode>,
): MoveEventResult {
    val nextPan = if (dragging && dragPointerPos != null) pan + (position - dragPointerPos) else pan
    val nextDragPointer = if (dragging) position else null
    val hoveredNode = visibleNodes.asReversed().firstOrNull { it.rect.contains((position - nextPan) / zoom) }
    return MoveEventResult(
        pan = nextPan,
        dragPointerPos = nextDragPointer,
        hoveredNode = hoveredNode,
    )
}

internal fun resolveZoomEvent(
    scrollY: Float,
    pointerPos: Offset,
    zoom: Float,
    pan: Offset,
): ZoomEventResult {
    val factor = if (scrollY > 0f) ZOOM_OUT_FACTOR else ZOOM_IN_FACTOR
    return resolveZoomByFactor(
        factor = factor,
        pointerPos = pointerPos,
        zoom = zoom,
        pan = pan,
    )
}

internal fun resolveZoomByFactor(
    factor: Float,
    pointerPos: Offset,
    zoom: Float,
    pan: Offset,
): ZoomEventResult {
    val newZoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
    val world = (pointerPos - pan) / zoom
    val newPan = pointerPos - world * newZoom
    return ZoomEventResult(zoom = newZoom, pan = newPan)
}

internal fun resolveReleaseEvent(
    position: Offset,
    uptimeMillis: Long,
    zoom: Float,
    pan: Offset,
    visibleNodes: List<TreemapNode>,
    lastClickKey: String?,
    lastClickAt: Long,
    onFocusPathChange: (String) -> Unit,
    onSelectedPathChange: (String?) -> Unit,
    onFileDoubleClick: (String) -> Unit,
): ReleaseEventResult {
    val node = visibleNodes.asReversed().firstOrNull { it.rect.contains((position - pan) / zoom) }
        ?: return ReleaseEventResult(lastClickKey = lastClickKey, lastClickAt = lastClickAt)

    if (!node.isDirectory) {
        onSelectedPathChange(node.path)
    }

    val key = nodeKey(node)
    val isDoubleClick = key == lastClickKey && uptimeMillis - lastClickAt < DOUBLE_CLICK_THRESHOLD_MILLIS
    if (isDoubleClick) {
        if (node.isDirectory) onFocusPathChange(node.path) else onFileDoubleClick(node.path)
    }
    return ReleaseEventResult(lastClickKey = key, lastClickAt = uptimeMillis)
}

internal fun centeredPan(
    canvasSize: IntSize,
    zoom: Float,
): Offset {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return Offset.Zero
    return Offset(
        x = canvasSize.width * (1f - zoom) / PAN_CENTER_DIVISOR,
        y = canvasSize.height * (1f - zoom) / PAN_CENTER_DIVISOR,
    )
}
