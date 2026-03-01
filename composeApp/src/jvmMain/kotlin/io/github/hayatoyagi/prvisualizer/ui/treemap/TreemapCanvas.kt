package io.github.hayatoyagi.prvisualizer.ui.treemap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.TreemapNode
import io.github.hayatoyagi.prvisualizer.ui.shared.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.computeConflictedDirs
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private const val MIN_DIRECTORY_RENDER_SIZE_PX = 1f
private const val MIN_FILE_RENDER_SIZE_PX = 2f
private const val ACTIVE_PR_DOT_RADIUS_PX = 1.5f
private const val BASE_ALPHA = 0.18f
private const val DIRECTORY_NEUTRAL_ALPHA = 0.20f
private const val ALPHA_DENSITY_MULTIPLIER = 0.78f
private const val MIN_ALPHA = 0.18f
private const val MAX_ALPHA = 0.96f
private const val BORDER_WIDTH_PX = 8f
private const val NODE_INSET_PX = 1.5f
private const val HIGHLIGHT_STROKE_WIDTH_PX = 2.5f
private const val CONFLICT_STRIPE_SPACING_PX = 12f
private const val CONFLICT_STRIPE_WIDTH_PX = 3f
private const val DOUBLE_INSET_MULTIPLIER = 2f

/**
 * Renders the treemap visualization canvas with directories and files.
 *
 * @param visibleDirectories List of directory nodes to render
 * @param visibleFiles List of file nodes to render
 * @param directoryOverlayByPath Map of directory paths to their overlay data
 * @param fileOverlayByPath Map of file paths to their overlay data
 * @param prColorMap Map of PR IDs to their assigned colors
 * @param hoveredNode The currently hovered node
 * @param selectedPath The path of the currently selected node
 * @param zoom Current zoom level
 * @param pan Current pan offset
 * @param modifier Modifier for the canvas
 */
@Composable
fun TreemapCanvas(
    visibleDirectories: List<TreemapNode>,
    visibleFiles: List<TreemapNode>,
    directoryOverlayByPath: Map<String, DirectoryOverlay>,
    fileOverlayByPath: Map<String, FileOverlay>,
    prColorMap: Map<String, Color>,
    hoveredNode: TreemapNode?,
    selectedPath: String?,
    zoom: Float,
    pan: Offset,
    modifier: Modifier = Modifier,
) {
    val conflictedDirectoryPaths = remember(fileOverlayByPath) {
        computeConflictedDirs(fileOverlayByPath)
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        drawDirectoryLayer(
            visibleDirectories = visibleDirectories,
            directoryOverlayByPath = directoryOverlayByPath,
            conflictedDirectoryPaths = conflictedDirectoryPaths,
            prColorMap = prColorMap,
            hoveredNode = hoveredNode,
            zoom = zoom,
            pan = pan,
        )
        drawFileLayer(
            visibleFiles = visibleFiles,
            fileOverlayByPath = fileOverlayByPath,
            prColorMap = prColorMap,
            hoveredNode = hoveredNode,
            selectedPath = selectedPath,
            zoom = zoom,
            pan = pan,
        )
    }
}

private fun DrawScope.drawDirectoryLayer(
    visibleDirectories: List<TreemapNode>,
    directoryOverlayByPath: Map<String, DirectoryOverlay>,
    conflictedDirectoryPaths: Set<String>,
    prColorMap: Map<String, Color>,
    hoveredNode: TreemapNode?,
    zoom: Float,
    pan: Offset,
) {
    visibleDirectories.forEach { node ->
        val widthPx = node.rect.width * zoom
        val heightPx = node.rect.height * zoom
        if (widthPx < MIN_DIRECTORY_RENDER_SIZE_PX || heightPx < MIN_DIRECTORY_RENDER_SIZE_PX) return@forEach
        val overlay = directoryOverlayByPath[node.path]
        drawNodeCell(
            node = node,
            zoom = zoom,
            pan = pan,
            fill = overlayFillColor(overlay?.dominantType, isDirectory = true),
            alpha = directoryAlpha(overlay),
            prs = overlay?.prs.orEmpty(),
            prColorMap = prColorMap,
            fallback = AppColors.treemapFallbackBorderDir,
            isHighlighted = hoveredNode?.path == node.path,
            hasConflict = conflictedDirectoryPaths.contains(node.path),
        )
    }
}

private fun DrawScope.drawFileLayer(
    visibleFiles: List<TreemapNode>,
    fileOverlayByPath: Map<String, FileOverlay>,
    prColorMap: Map<String, Color>,
    hoveredNode: TreemapNode?,
    selectedPath: String?,
    zoom: Float,
    pan: Offset,
) {
    visibleFiles.forEach { node ->
        val widthPx = node.rect.width * zoom
        val heightPx = node.rect.height * zoom
        val overlay = fileOverlayByPath[node.path]
        if (widthPx < MIN_FILE_RENDER_SIZE_PX || heightPx < MIN_FILE_RENDER_SIZE_PX) {
            drawSmallActivePrDot(node = node, zoom = zoom, pan = pan)
            return@forEach
        }
        val prs = overlay?.prs.orEmpty()
        drawNodeCell(
            node = node,
            zoom = zoom,
            pan = pan,
            fill = overlayFillColor(overlay?.dominantType, isDirectory = false),
            alpha = overlayDensityAlpha(overlay?.density ?: 0f),
            prs = prs,
            prColorMap = prColorMap,
            fallback = AppColors.treemapFallbackBorderFile,
            isHighlighted = node.path == hoveredNode?.path || node.path == selectedPath,
            hasConflict = prs.size > 1,
        )
    }
}

private fun DrawScope.drawSmallActivePrDot(
    node: TreemapNode,
    zoom: Float,
    pan: Offset,
) {
    if (!node.hasActivePr) return
    drawCircle(
        color = AppColors.treemapActivePrDot,
        radius = ACTIVE_PR_DOT_RADIUS_PX,
        center = node.rect.center * zoom + pan,
    )
}

private fun overlayFillColor(
    dominantType: ChangeType?,
    isDirectory: Boolean,
): Color = when (dominantType) {
    ChangeType.Addition -> AppColors.treemapAddition
    ChangeType.Modification -> AppColors.treemapModification
    ChangeType.Deletion -> AppColors.treemapDeletion
    null -> if (isDirectory) AppColors.treemapNeutralDir else AppColors.treemapNeutralFile
}

private fun directoryAlpha(overlay: DirectoryOverlay?): Float {
    if (overlay?.dominantType == null) return DIRECTORY_NEUTRAL_ALPHA
    return overlayDensityAlpha(overlay.density)
}

private fun overlayDensityAlpha(density: Float): Float = (BASE_ALPHA + density * ALPHA_DENSITY_MULTIPLIER).coerceIn(MIN_ALPHA, MAX_ALPHA)

private fun DrawScope.drawNodeCell(
    node: TreemapNode,
    zoom: Float,
    pan: Offset,
    fill: Color,
    alpha: Float,
    prs: List<PullRequest>,
    prColorMap: Map<String, Color>,
    fallback: Color,
    isHighlighted: Boolean,
    hasConflict: Boolean,
) {
    val rawTopLeft = node.rect.topLeft * zoom + pan
    val rawSize = node.rect.size * zoom
    val borderWidth = BORDER_WIDTH_PX
    val inset = NODE_INSET_PX
    val topLeft = Offset(rawTopLeft.x + inset, rawTopLeft.y + inset)
    val size = Size(
        width = (rawSize.width - inset * DOUBLE_INSET_MULTIPLIER).coerceAtLeast(0f),
        height = (rawSize.height - inset * DOUBLE_INSET_MULTIPLIER).coerceAtLeast(0f),
    )
    if (size.width <= 0f || size.height <= 0f) return

    drawRect(color = fill.copy(alpha = alpha), topLeft = topLeft, size = size)
    drawPrBorder(
        topLeft = topLeft,
        size = size,
        prs = prs,
        colorMap = prColorMap,
        fallback = fallback,
        borderWidth = borderWidth,
    )
    if (isHighlighted) {
        drawRect(
            color = Color.White,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = HIGHLIGHT_STROKE_WIDTH_PX),
        )
    }
    if (hasConflict) {
        clipRect(
            left = topLeft.x,
            top = topLeft.y,
            right = topLeft.x + size.width,
            bottom = topLeft.y + size.height,
        ) {
            val spacing = CONFLICT_STRIPE_SPACING_PX
            var x = topLeft.x - size.height
            while (x < topLeft.x + size.width) {
                drawLine(
                    color = AppColors.treemapConflictStripe,
                    start = Offset(x, topLeft.y + size.height),
                    end = Offset(x + size.height, topLeft.y),
                    strokeWidth = CONFLICT_STRIPE_WIDTH_PX,
                    cap = StrokeCap.Square,
                )
                x += spacing
            }
        }
    }
}
