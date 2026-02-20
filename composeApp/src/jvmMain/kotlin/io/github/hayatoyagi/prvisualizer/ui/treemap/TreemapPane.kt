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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import io.github.hayatoyagi.prvisualizer.computeTreemap
import io.github.hayatoyagi.prvisualizer.ui.shared.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.copyToClipboard
import io.github.hayatoyagi.prvisualizer.ui.shared.nodeKey
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.shared.parentPathOf
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

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
    repoFullName: String,
    modifier: Modifier = Modifier,
) {
    var zoom by remember { mutableStateOf(0.8f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var pendingViewportCentering by remember { mutableStateOf(true) }
    var pointerPos by remember { mutableStateOf(Offset.Zero) }
    var dragPointerPos by remember { mutableStateOf<Offset?>(null) }
    var hoveredNode by remember { mutableStateOf<TreemapNode?>(null) }
    var lastClickKey by remember { mutableStateOf<String?>(null) }
    var lastClickAt by remember { mutableStateOf(0L) }

    LaunchedEffect(focusPath, viewportResetToken) {
        zoom = 0.8f
        pendingViewportCentering = true
        if (canvasSize.width > 1 && canvasSize.height > 1) {
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
                text = "Visible PRs: ${visiblePrs.size}",
                color = AppColors.textSecondary,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .background(AppColors.backgroundCanvas)
                .onSizeChanged {
                    canvasSize = it
                    if (pendingViewportCentering && it.width > 1 && it.height > 1) {
                        pan = centeredPan(canvasSize = it, zoom = zoom)
                        pendingViewportCentering = false
                    }
                }
                .onPointerEvent(PointerEventType.Move) { event ->
                    val position = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                    pointerPos = position
                    val dragging = event.buttons.isSecondaryPressed
                    if (dragging) {
                        val prev = dragPointerPos
                        if (prev != null) {
                            pan += position - prev
                        }
                        dragPointerPos = position
                    } else {
                        dragPointerPos = null
                    }

                    val world = (position - pan) / zoom
                    hoveredNode = visibleNodes.asReversed().firstOrNull { it.rect.contains(world) }
                }
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: return@onPointerEvent
                    val factor = if (scrollY > 0f) 0.9f else 1.1f
                    val newZoom = (zoom * factor).coerceIn(0.4f, 8f)
                    val cursor = pointerPos
                    val world = (cursor - pan) / zoom
                    pan = cursor - world * newZoom
                    zoom = newZoom
                }
                .onPointerEvent(PointerEventType.Release) { event ->
                    dragPointerPos = null
                    val change = event.changes.firstOrNull() ?: return@onPointerEvent
                    if (event.button != PointerButton.Primary) return@onPointerEvent
                    val world = (change.position - pan) / zoom
                    val node = visibleNodes.asReversed().firstOrNull { it.rect.contains(world) } ?: return@onPointerEvent

                    if (!node.isDirectory) {
                        onSelectedPathChange(node.path)
                        val related = visiblePrs
                            .filter { pr -> pr.files.any { it.path == node.path } }
                            .map { it.id }
                            .toSet()
                        onRelatedPrsDetected(related)
                    }

                    val key = nodeKey(node)
                    val isDoubleClick = key == lastClickKey && (change.uptimeMillis - lastClickAt) < 350
                    if (isDoubleClick) {
                        if (node.isDirectory) {
                            onFocusPathChange(node.path)
                        } else {
                            openUrl("https://github.com/${repoFullName}/blob/main/${node.path}")
                        }
                    }
                    lastClickKey = key
                    lastClickAt = change.uptimeMillis
                },
        ) {
            TreemapCanvas(
                visibleDirectories = visibleDirectories,
                visibleFiles = visibleFiles,
                directoryOverlayByPath = directoryOverlayByPath,
                fileOverlayByPath = fileOverlayByPath,
                prColorMap = prColorMap,
                hoveredNode = hoveredNode,
                selectedPath = selectedPath,
                zoom = zoom,
                pan = pan,
            )
            TreemapOverlay(
                visibleNodes = visibleNodes,
                visibleFiles = visibleFiles,
                fileOverlayByPath = fileOverlayByPath,
                hoveredNode = hoveredNode,
                hoveredOverlay = hoveredOverlay,
                hoveredDirOverlay = hoveredDirOverlay,
                zoom = zoom,
                pan = pan,
                pointerPos = pointerPos,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                TreemapLegend()
            }
        }
    }
}

private fun centeredPan(canvasSize: IntSize, zoom: Float): Offset {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return Offset.Zero
    return Offset(
        x = canvasSize.width * (1f - zoom) / 2f,
        y = canvasSize.height * (1f - zoom) / 2f,
    )
}
