package io.github.hayatoyagi.prvisualizer.ui.treemap

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.TreemapNode
import io.github.hayatoyagi.prvisualizer.ui.shared.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private const val MIN_NODE_LABEL_WIDTH_PX = 90f
private const val MIN_NODE_LABEL_HEIGHT_PX = 24f
private const val NODE_LABEL_X_OFFSET_PX = 6f
private const val NODE_LABEL_Y_OFFSET_PX = 4f
private const val MIN_FILE_PR_WIDTH_PX = 120f
private const val MIN_FILE_PR_HEIGHT_PX = 40f
private const val MAX_VISIBLE_PR_COUNT = 3
private const val FILE_PR_Y_OFFSET_PX = 18f
private const val TOOLTIP_POINTER_OFFSET_PX = 12f

@Composable
fun TreemapOverlay(
    visibleNodes: List<TreemapNode>,
    visibleFiles: List<TreemapNode>,
    fileOverlayByPath: Map<String, FileOverlay>,
    hoveredNode: TreemapNode?,
    hoveredOverlay: FileOverlay?,
    hoveredDirOverlay: DirectoryOverlay?,
    zoom: Float,
    pan: Offset,
    pointerPos: Offset,
    modifier: Modifier = Modifier,
) {
    visibleNodes.forEach { node ->
        val widthPx = node.rect.width * zoom
        val heightPx = node.rect.height * zoom
        if (widthPx <= MIN_NODE_LABEL_WIDTH_PX || heightPx <= MIN_NODE_LABEL_HEIGHT_PX) return@forEach
        val label = if (node.isDirectory) "${node.name}/" else node.name
        Text(
            text = label,
            color = if (node.isDirectory) AppColors.explorerNodeDir else AppColors.textCanvasLabel,
            style = MaterialTheme.typography.labelSmall,
            modifier = modifier
                .offset {
                    IntOffset(
                        x = (node.rect.left * zoom + pan.x + NODE_LABEL_X_OFFSET_PX).toInt(),
                        y = (node.rect.top * zoom + pan.y + NODE_LABEL_Y_OFFSET_PX).toInt(),
                    )
                },
        )
    }

    visibleFiles.forEach { node ->
        val overlay = fileOverlayByPath[node.path] ?: return@forEach
        val widthPx = node.rect.width * zoom
        val heightPx = node.rect.height * zoom
        if (widthPx <= MIN_FILE_PR_WIDTH_PX || heightPx <= MIN_FILE_PR_HEIGHT_PX) return@forEach
        val prText = overlay.prs.take(MAX_VISIBLE_PR_COUNT).joinToString(" ") { "#${it.number}" }
        val suffix = if (overlay.prs.size > MAX_VISIBLE_PR_COUNT) " +" else ""
        Text(
            text = "$prText$suffix",
            color = AppColors.textPrItem,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
                .offset {
                    IntOffset(
                        x = (node.rect.left * zoom + pan.x + NODE_LABEL_X_OFFSET_PX).toInt(),
                        y = (node.rect.top * zoom + pan.y + FILE_PR_Y_OFFSET_PX).toInt(),
                    )
                },
        )
    }

    if (hoveredNode != null) {
        Surface(
            modifier = modifier
                .offset {
                    IntOffset(
                        x = (pointerPos.x + TOOLTIP_POINTER_OFFSET_PX).toInt(),
                        y = (pointerPos.y + TOOLTIP_POINTER_OFFSET_PX).toInt(),
                    )
                }.border(1.dp, AppColors.tooltipBorder),
            color = AppColors.tooltipBackground,
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(hoveredNode.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (hoveredNode.isDirectory) "Type: Directory" else "Type: File",
                    color = AppColors.textTooltip,
                )
                Text("Path: ${hoveredNode.path}", color = AppColors.textTooltip)
                Text("LOC: ${hoveredNode.totalLines}", color = AppColors.textTooltip)
                val prs = if (hoveredNode.isDirectory) hoveredDirOverlay?.prs.orEmpty() else hoveredOverlay?.prs.orEmpty()
                val prText = if (prs.isEmpty()) {
                    "PR: none"
                } else {
                    val prDetails = prs.joinToString { pr ->
                        val fileChange = pr.files.find { it.path == hoveredNode.path }
                        if (fileChange != null && !hoveredNode.isDirectory) {
                            "#${pr.number} ${pr.author} (+${fileChange.additions}/-${fileChange.deletions})"
                        } else {
                            "#${pr.number} ${pr.author}"
                        }
                    }
                    "PR: $prDetails"
                }
                Text(
                    text = prText,
                    color = if (prs.size > 1) AppColors.textTooltipMultiPr else AppColors.textTooltip,
                )
            }
        }
    }
}
