package io.github.hayatoyagi.prvisualizer.ui.treemap.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.TreemapNode
import io.github.hayatoyagi.prvisualizer.state.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.state.FileOverlay

internal data class TreemapViewportModel(
    val visibleNodes: List<TreemapNode>,
    val visibleDirectories: List<TreemapNode>,
    val visibleFiles: List<TreemapNode>,
    val fileOverlayByPath: Map<String, FileOverlay>,
    val directoryOverlayByPath: Map<String, DirectoryOverlay>,
    val prColorMap: Map<String, Color>,
    val selectedPath: String?,
    val hoveredNode: TreemapNode?,
    val hoveredOverlay: FileOverlay?,
    val hoveredDirOverlay: DirectoryOverlay?,
    val zoom: Float,
    val pan: Offset,
    val pointerPos: Offset,
)
