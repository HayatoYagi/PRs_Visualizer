package io.github.hayatoyagi.prvisualizer.ui.treemap.models

import androidx.compose.ui.geometry.Offset
import io.github.hayatoyagi.prvisualizer.TreemapNode

internal data class MoveEventResult(
    val pan: Offset,
    val dragPointerPos: Offset?,
    val hoveredNode: TreemapNode?,
)

internal data class ZoomEventResult(
    val zoom: Float,
    val pan: Offset,
)

internal data class ReleaseEventResult(
    val lastClickKey: String?,
    val lastClickAt: Long,
)
