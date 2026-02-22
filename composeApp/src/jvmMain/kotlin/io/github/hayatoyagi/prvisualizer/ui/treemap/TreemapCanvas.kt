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
import io.github.hayatoyagi.prvisualizer.ui.shared.computeConflictedDirectoryPaths
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

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
        computeConflictedDirectoryPaths(fileOverlayByPath)
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        visibleDirectories.forEach { node ->
            val widthPx = node.rect.width * zoom
            val heightPx = node.rect.height * zoom
            if (widthPx < 1f || heightPx < 1f) return@forEach
            val overlay = directoryOverlayByPath[node.path]
            val fill = when (overlay?.dominantType) {
                ChangeType.Addition -> AppColors.treemapAddition
                ChangeType.Modification -> AppColors.treemapModification
                ChangeType.Deletion -> AppColors.treemapDeletion
                null -> AppColors.treemapNeutralDir
            }
            val alpha = if (overlay?.dominantType == null) {
                0.20f
            } else {
                (0.18f + overlay.density * 0.78f).coerceIn(0.18f, 0.96f)
            }
            drawNodeCell(
                node = node,
                zoom = zoom,
                pan = pan,
                fill = fill,
                alpha = alpha,
                prs = overlay?.prs.orEmpty(),
                prColorMap = prColorMap,
                fallback = AppColors.treemapFallbackBorderDir,
                isHighlighted = hoveredNode?.path == node.path,
                hasConflict = conflictedDirectoryPaths.contains(node.path),
            )
        }

        visibleFiles.forEach { node ->
            val widthPx = node.rect.width * zoom
            val heightPx = node.rect.height * zoom
            val overlay = fileOverlayByPath[node.path]
            if (widthPx < 2f || heightPx < 2f) {
                if (node.hasActivePr) {
                    drawCircle(
                        color = AppColors.treemapActivePrDot,
                        radius = 1.5f,
                        center = node.rect.center * zoom + pan,
                    )
                }
                return@forEach
            }
            val prs = overlay?.prs.orEmpty()
            val fill = when (overlay?.dominantType) {
                ChangeType.Addition -> AppColors.treemapAddition
                ChangeType.Modification -> AppColors.treemapModification
                ChangeType.Deletion -> AppColors.treemapDeletion
                null -> AppColors.treemapNeutralFile
            }
            val alpha = (0.18f + (overlay?.density ?: 0f) * 0.78f).coerceIn(0.18f, 0.96f)
            drawNodeCell(
                node = node,
                zoom = zoom,
                pan = pan,
                fill = fill,
                alpha = alpha,
                prs = prs,
                prColorMap = prColorMap,
                fallback = AppColors.treemapFallbackBorderFile,
                isHighlighted = node.path == hoveredNode?.path || node.path == selectedPath,
                hasConflict = prs.size > 1,
            )
        }
    }
}

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
    val borderWidth = 8f
    val inset = 1.5f
    val topLeft = Offset(rawTopLeft.x + inset, rawTopLeft.y + inset)
    val size = Size(
        width = (rawSize.width - inset * 2f).coerceAtLeast(0f),
        height = (rawSize.height - inset * 2f).coerceAtLeast(0f),
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
            style = Stroke(width = 2.5f),
        )
    }
    if (hasConflict) {
        clipRect(
            left = topLeft.x,
            top = topLeft.y,
            right = topLeft.x + size.width,
            bottom = topLeft.y + size.height,
        ) {
            val spacing = 8f
            var x = topLeft.x - size.height
            while (x < topLeft.x + size.width) {
                drawLine(
                    color = AppColors.treemapConflictStripe,
                    start = Offset(x, topLeft.y + size.height),
                    end = Offset(x + size.height, topLeft.y),
                    strokeWidth = 1f,
                    cap = StrokeCap.Square,
                )
                x += spacing
            }
        }
    }
}
