package io.github.hayatoyagi.prvisualizer.ui.treemap.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

internal data class TreemapViewportCallbacks(
    val onSizeChanged: (IntSize) -> Unit,
    val onMoveEvent: (position: Offset, dragging: Boolean) -> Unit,
    val onScrollEvent: (scrollY: Float) -> Unit,
    val onReleaseEvent: (position: Offset, uptimeMillis: Long) -> Unit,
)
