package io.github.hayatoyagi.prvisualizer.ui.treemap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.TreemapNode
import io.github.hayatoyagi.prvisualizer.ui.shared.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.copyToClipboard
import io.github.hayatoyagi.prvisualizer.ui.shared.parentPathOf
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private const val INITIAL_ZOOM = 0.8f
private const val MIN_CANVAS_SIZE_PX = 1
private const val ZOOM_OUT_FACTOR = 0.9f
private const val ZOOM_IN_FACTOR = 1.1f
private const val MIN_ZOOM = 0.4f
private const val MAX_ZOOM = 8f
private const val DOUBLE_CLICK_THRESHOLD_MILLIS = 350L
private const val LOADING_OVERLAY_ALPHA = 0.8f
private const val PAN_CENTER_DIVISOR = 2f

private data class MoveEventResult(
    val pan: Offset,
    val dragPointerPos: Offset?,
    val hoveredNode: TreemapNode?,
)

private data class ZoomEventResult(
    val zoom: Float,
    val pan: Offset,
)

private data class ReleaseEventResult(
    val lastClickKey: String?,
    val lastClickAt: Long,
)

private data class TreemapViewportModel(
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

private data class TreemapViewportCallbacks(
    val onSizeChanged: (IntSize) -> Unit,
    val onMoveEvent: (position: Offset, dragging: Boolean) -> Unit,
    val onScrollEvent: (scrollY: Float) -> Unit,
    val onReleaseEvent: (position: Offset, uptimeMillis: Long) -> Unit,
)

/**
 * Displays the interactive treemap visualization pane.
 *
 * @param focusPath The current focused directory path
 * @param visiblePrs List of visible pull requests
 * @param focusRoot The root directory node for the focused path
 * @param selectedPath The currently selected file path
 * @param fileOverlayByPath Map of file paths to their overlay data
 * @param directoryOverlayByPath Map of directory paths to their overlay data
 * @param prColorMap Map of PR IDs to their assigned colors
 * @param viewportResetToken Token to trigger viewport resets
 * @param onFocusPathChange Callback when focus path changes
 * @param onSelectedPathChange Callback when selected path changes
 * @param onRelatedPrsDetected Callback when related PRs are detected
 * @param onFileDoubleClick Callback when a file is double-clicked
 * @param modifier Modifier for the pane
 * @param isLoading Whether the pane is in loading state
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
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

    val allLayoutNodes = remember(focusRoot, canvasSize) {
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

    Column(modifier = modifier.fillMaxHeight()) {
        TreemapPaneHeader(
            focusPath = focusPath,
            visiblePrCount = visiblePrs.size,
            onFocusPathChange = onFocusPathChange,
        )
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

@Composable
private fun TreemapPaneHeader(
    focusPath: String,
    visiblePrCount: Int,
    onFocusPathChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.backgroundHeader)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = { onFocusPathChange("") }) {
            Text("Root")
        }
        Button(
            onClick = { onFocusPathChange(parentPathOf(focusPath)) },
            enabled = focusPath.isNotBlank(),
        ) {
            Text("Up")
        }
        Text(
            text = "Focus: /${focusPath.ifBlank { "" }}",
            color = AppColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { copyToClipboard("/${focusPath.ifBlank { "" }}") }) {
            Text("Copy")
        }
        Text(
            text = "Visible PRs: $visiblePrCount",
            color = AppColors.textSecondary,
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun TreemapViewport(
    model: TreemapViewportModel,
    isLoading: Boolean,
    callbacks: TreemapViewportCallbacks,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(AppColors.backgroundCanvas)
            .onSizeChanged(callbacks.onSizeChanged)
            .treemapMoveHandler(isLoading = isLoading, onMoveEvent = callbacks.onMoveEvent)
            .treemapScrollHandler(isLoading = isLoading, onScrollEvent = callbacks.onScrollEvent)
            .treemapReleaseHandler(isLoading = isLoading, onReleaseEvent = callbacks.onReleaseEvent),
    ) {
        TreemapCanvas(
            visibleDirectories = model.visibleDirectories,
            visibleFiles = model.visibleFiles,
            directoryOverlayByPath = model.directoryOverlayByPath,
            fileOverlayByPath = model.fileOverlayByPath,
            prColorMap = model.prColorMap,
            hoveredNode = model.hoveredNode,
            selectedPath = model.selectedPath,
            zoom = model.zoom,
            pan = model.pan,
        )
        TreemapOverlay(
            visibleNodes = model.visibleNodes,
            visibleFiles = model.visibleFiles,
            fileOverlayByPath = model.fileOverlayByPath,
            hoveredNode = model.hoveredNode,
            hoveredOverlay = model.hoveredOverlay,
            hoveredDirOverlay = model.hoveredDirOverlay,
            zoom = model.zoom,
            pan = model.pan,
            pointerPos = model.pointerPos,
        )
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.backgroundCanvas.copy(alpha = LOADING_OVERLAY_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AppColors.textPrimary)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.treemapMoveHandler(
    isLoading: Boolean,
    onMoveEvent: (position: Offset, dragging: Boolean) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Move) { event ->
    if (isLoading) return@onPointerEvent
    val position = event.changes.firstOrNull()?.position ?: return@onPointerEvent
    onMoveEvent(position, event.buttons.isSecondaryPressed)
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.treemapScrollHandler(
    isLoading: Boolean,
    onScrollEvent: (scrollY: Float) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Scroll) { event ->
    if (isLoading) return@onPointerEvent
    val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: return@onPointerEvent
    onScrollEvent(scrollY)
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.treemapReleaseHandler(
    isLoading: Boolean,
    onReleaseEvent: (position: Offset, uptimeMillis: Long) -> Unit,
): Modifier = onPointerEvent(PointerEventType.Release) { event ->
    if (isLoading || event.button != PointerButton.Primary) return@onPointerEvent
    val change = event.changes.firstOrNull() ?: return@onPointerEvent
    onReleaseEvent(change.position, change.uptimeMillis)
}

private fun resolveMoveEvent(
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

private fun resolveZoomEvent(
    scrollY: Float,
    pointerPos: Offset,
    zoom: Float,
    pan: Offset,
): ZoomEventResult {
    val factor = if (scrollY > 0f) ZOOM_OUT_FACTOR else ZOOM_IN_FACTOR
    val newZoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
    val world = (pointerPos - pan) / zoom
    val newPan = pointerPos - world * newZoom
    return ZoomEventResult(zoom = newZoom, pan = newPan)
}

private fun resolveReleaseEvent(
    position: Offset,
    uptimeMillis: Long,
    zoom: Float,
    pan: Offset,
    visibleNodes: List<TreemapNode>,
    visiblePrs: List<PullRequest>,
    lastClickKey: String?,
    lastClickAt: Long,
    onFocusPathChange: (String) -> Unit,
    onSelectedPathChange: (String?) -> Unit,
    onRelatedPrsDetected: (Set<String>) -> Unit,
    onFileDoubleClick: (String) -> Unit,
): ReleaseEventResult {
    val node = visibleNodes.asReversed().firstOrNull { it.rect.contains((position - pan) / zoom) }
        ?: return ReleaseEventResult(lastClickKey = lastClickKey, lastClickAt = lastClickAt)

    if (!node.isDirectory) {
        onSelectedPathChange(node.path)
        onRelatedPrsDetected(relatedPrIdsForNode(nodePath = node.path, visiblePrs = visiblePrs))
    }

    val key = nodeKey(node)
    val isDoubleClick = key == lastClickKey && uptimeMillis - lastClickAt < DOUBLE_CLICK_THRESHOLD_MILLIS
    if (isDoubleClick) {
        if (node.isDirectory) onFocusPathChange(node.path) else onFileDoubleClick(node.path)
    }
    return ReleaseEventResult(lastClickKey = key, lastClickAt = uptimeMillis)
}

private fun relatedPrIdsForNode(
    nodePath: String,
    visiblePrs: List<PullRequest>,
): Set<String> = visiblePrs
    .filter { pr -> pr.files.any { it.path == nodePath } }
    .map { it.id }
    .toSet()

private fun centeredPan(
    canvasSize: IntSize,
    zoom: Float,
): Offset {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return Offset.Zero
    return Offset(
        x = canvasSize.width * (1f - zoom) / PAN_CENTER_DIVISOR,
        y = canvasSize.height * (1f - zoom) / PAN_CENTER_DIVISOR,
    )
}
