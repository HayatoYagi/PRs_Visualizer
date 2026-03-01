package io.github.hayatoyagi.prvisualizer.ui.treemap

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.TreemapNode
import io.github.hayatoyagi.prvisualizer.ui.shared.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.treemap.components.TreemapViewport
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.INITIAL_ZOOM
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.MAX_ZOOM
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.MIN_CANVAS_SIZE_PX
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.MIN_ZOOM
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.ZOOM_IN_FACTOR
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.ZOOM_OUT_FACTOR
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.centeredPan
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.resolveMoveEvent
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.resolveReleaseEvent
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.resolveZoomByFactor
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.resolveZoomEvent
import io.github.hayatoyagi.prvisualizer.ui.treemap.models.TreemapViewportCallbacks
import io.github.hayatoyagi.prvisualizer.ui.treemap.models.TreemapViewportModel

@Composable
fun TreemapPane(
    focusPath: String,
    visiblePrs: List<PullRequest>,
    focusRoot: FileNode.Directory,
    selectedPath: String?,
    fileOverlayByPath: Map<String, FileOverlay>,
    directoryOverlayByPath: Map<String, DirectoryOverlay>,
    prColorMap: Map<String, Color>,
    viewportResetToken: Int,
    onFocusPathChange: (String) -> Unit,
    onSelectedPathChange: (String?) -> Unit,
    onRelatedPrsDetected: (Set<String>) -> Unit,
    onFileDoubleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    var zoom by remember { mutableStateOf(INITIAL_ZOOM) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize(MIN_CANVAS_SIZE_PX, MIN_CANVAS_SIZE_PX)) }
    var pendingViewportCentering by remember { mutableStateOf(true) }
    var pointerPos by remember { mutableStateOf(Offset.Zero) }
    var dragPointerPos by remember { mutableStateOf<Offset?>(null) }
    var hoveredNode by remember { mutableStateOf<TreemapNode?>(null) }
    var lastClickKey by remember { mutableStateOf<String?>(null) }
    var lastClickAt by remember { mutableStateOf(0L) }

    LaunchedEffect(focusPath, viewportResetToken) {
        zoom = INITIAL_ZOOM
        pendingViewportCentering = true
        if (canvasSize.width > MIN_CANVAS_SIZE_PX && canvasSize.height > MIN_CANVAS_SIZE_PX) {
            pan = centeredPan(canvasSize = canvasSize, zoom = zoom)
            pendingViewportCentering = false
        } else {
            pan = Offset.Zero
        }
    }

    // Memoize layout computation to avoid recomputation on UI state changes (hover, scroll, etc.)
    // Keys: focusPath (navigation), focusRoot (data), canvasSize (viewport dimensions)
    val allLayoutNodes = remember(focusPath, focusRoot, canvasSize) {
        computeTreemap(
            root = focusRoot,
            bounds = Rect(0f, 0f, canvasSize.width.toFloat(), canvasSize.height.toFloat()),
        )
    }
    val visibleNodes = remember(allLayoutNodes) { allLayoutNodes.filter { it.depth == 1 } }
    val visibleFiles = remember(visibleNodes) { visibleNodes.filter { !it.isDirectory } }
    val visibleDirectories = remember(visibleNodes) { visibleNodes.filter { it.isDirectory } }

    val hoveredOverlay = hoveredNode?.takeIf { !it.isDirectory }?.let { fileOverlayByPath[it.path] }
    val hoveredDirOverlay = hoveredNode?.takeIf { it.isDirectory }?.let { directoryOverlayByPath[it.path] }
    val viewportCenter = Offset(canvasSize.width / 2f, canvasSize.height / 2f)

    Column(modifier = modifier.fillMaxHeight()) {
        TreemapViewport(
            model = TreemapViewportModel(
                visibleNodes = visibleNodes,
                visibleDirectories = visibleDirectories,
                visibleFiles = visibleFiles,
                fileOverlayByPath = fileOverlayByPath,
                directoryOverlayByPath = directoryOverlayByPath,
                prColorMap = prColorMap,
                selectedPath = selectedPath,
                hoveredNode = hoveredNode,
                hoveredOverlay = hoveredOverlay,
                hoveredDirOverlay = hoveredDirOverlay,
                zoom = zoom,
                pan = pan,
                pointerPos = pointerPos,
            ),
            isLoading = isLoading,
            canZoomOut = zoom > MIN_ZOOM,
            canZoomIn = zoom < MAX_ZOOM,
            onZoomOut = {
                val result = resolveZoomByFactor(
                    factor = ZOOM_OUT_FACTOR,
                    pointerPos = viewportCenter,
                    zoom = zoom,
                    pan = pan,
                )
                zoom = result.zoom
                pan = result.pan
            },
            onZoomIn = {
                val result = resolveZoomByFactor(
                    factor = ZOOM_IN_FACTOR,
                    pointerPos = viewportCenter,
                    zoom = zoom,
                    pan = pan,
                )
                zoom = result.zoom
                pan = result.pan
            },
            callbacks = TreemapViewportCallbacks(
                onSizeChanged = { newSize ->
                    canvasSize = newSize
                    if (pendingViewportCentering && newSize.width > MIN_CANVAS_SIZE_PX && newSize.height > MIN_CANVAS_SIZE_PX) {
                        pan = centeredPan(canvasSize = newSize, zoom = zoom)
                        pendingViewportCentering = false
                    }
                },
                onMoveEvent = { position, dragging ->
                    val result = resolveMoveEvent(
                        position = position,
                        dragging = dragging,
                        pan = pan,
                        dragPointerPos = dragPointerPos,
                        zoom = zoom,
                        visibleNodes = visibleNodes,
                    )
                    pointerPos = position
                    pan = result.pan
                    dragPointerPos = result.dragPointerPos
                    hoveredNode = result.hoveredNode
                },
                onScrollEvent = { scrollY ->
                    val result = resolveZoomEvent(
                        scrollY = scrollY,
                        pointerPos = pointerPos,
                        zoom = zoom,
                        pan = pan,
                    )
                    zoom = result.zoom
                    pan = result.pan
                },
                onReleaseEvent = { position, uptimeMillis ->
                    val result = resolveReleaseEvent(
                        position = position,
                        uptimeMillis = uptimeMillis,
                        zoom = zoom,
                        pan = pan,
                        visibleNodes = visibleNodes,
                        visiblePrs = visiblePrs,
                        lastClickKey = lastClickKey,
                        lastClickAt = lastClickAt,
                        onFocusPathChange = onFocusPathChange,
                        onSelectedPathChange = onSelectedPathChange,
                        onRelatedPrsDetected = onRelatedPrsDetected,
                        onFileDoubleClick = onFileDoubleClick,
                    )
                    dragPointerPos = null
                    lastClickKey = result.lastClickKey
                    lastClickAt = result.lastClickAt
                },
            ),
        )
    }
}
