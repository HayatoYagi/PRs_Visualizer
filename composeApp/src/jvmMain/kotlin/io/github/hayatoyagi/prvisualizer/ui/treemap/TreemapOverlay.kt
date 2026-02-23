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
    TreemapNodeLabelLayer(visibleNodes = visibleNodes, zoom = zoom, pan = pan, modifier = modifier)
    TreemapPrLabelLayer(
        visibleFiles = visibleFiles,
        fileOverlayByPath = fileOverlayByPath,
        zoom = zoom,
        pan = pan,
        modifier = modifier,
    )
    TreemapHoverTooltip(
        hoveredNode = hoveredNode,
        hoveredOverlay = hoveredOverlay,
        hoveredDirOverlay = hoveredDirOverlay,
        pointerPos = pointerPos,
        modifier = modifier,
    )
}

@Composable
private fun TreemapNodeLabelLayer(
    visibleNodes: List<TreemapNode>,
    zoom: Float,
    pan: Offset,
    modifier: Modifier,
) {
    visibleNodes.forEach { node ->
        val widthPx = node.rect.width * zoom
        val heightPx = node.rect.height * zoom
        if (widthPx <= MIN_NODE_LABEL_WIDTH_PX || heightPx <= MIN_NODE_LABEL_HEIGHT_PX) return@forEach
        Text(
            text = nodeLabel(node),
            color = if (node.isDirectory) AppColors.explorerNodeDir else AppColors.textCanvasLabel,
            style = MaterialTheme.typography.labelSmall,
            modifier = modifier.offset { nodeLabelOffset(node = node, zoom = zoom, pan = pan) },
        )
    }
}

@Composable
private fun TreemapPrLabelLayer(
    visibleFiles: List<TreemapNode>,
    fileOverlayByPath: Map<String, FileOverlay>,
    zoom: Float,
    pan: Offset,
    modifier: Modifier,
) {
    visibleFiles.forEach { node ->
        val overlay = fileOverlayByPath[node.path] ?: return@forEach
        val widthPx = node.rect.width * zoom
        val heightPx = node.rect.height * zoom
        if (widthPx <= MIN_FILE_PR_WIDTH_PX || heightPx <= MIN_FILE_PR_HEIGHT_PX) return@forEach
        Text(
            text = prSummaryText(overlay),
            color = AppColors.textPrItem,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.offset { filePrOffset(node = node, zoom = zoom, pan = pan) },
        )
    }
}

@Composable
private fun TreemapHoverTooltip(
    hoveredNode: TreemapNode?,
    hoveredOverlay: FileOverlay?,
    hoveredDirOverlay: DirectoryOverlay?,
    pointerPos: Offset,
    modifier: Modifier,
) {
    val node = hoveredNode ?: return
    val prs = if (node.isDirectory) hoveredDirOverlay?.prs.orEmpty() else hoveredOverlay?.prs.orEmpty()
    Surface(
        modifier = modifier
            .offset { tooltipOffset(pointerPos) }
            .border(1.dp, AppColors.tooltipBorder),
        color = AppColors.tooltipBackground,
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(node.name, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (node.isDirectory) "Type: Directory" else "Type: File",
                color = AppColors.textTooltip,
            )
            Text("Path: ${node.path}", color = AppColors.textTooltip)
            Text("LOC: ${node.totalLines}", color = AppColors.textTooltip)
            Text(
                text = tooltipPrText(node = node, prs = prs),
                color = if (prs.size > 1) AppColors.textTooltipMultiPr else AppColors.textTooltip,
            )
        }
    }
}

private fun nodeLabel(node: TreemapNode): String = if (node.isDirectory) "${node.name}/" else node.name

private fun prSummaryText(overlay: FileOverlay): String {
    val prText = overlay.prs.take(MAX_VISIBLE_PR_COUNT).joinToString(" ") { "#${it.number}" }
    val suffix = if (overlay.prs.size > MAX_VISIBLE_PR_COUNT) " +" else ""
    return "$prText$suffix"
}

private fun nodeLabelOffset(
    node: TreemapNode,
    zoom: Float,
    pan: Offset,
): IntOffset = IntOffset(
    x = (node.rect.left * zoom + pan.x + NODE_LABEL_X_OFFSET_PX).toInt(),
    y = (node.rect.top * zoom + pan.y + NODE_LABEL_Y_OFFSET_PX).toInt(),
)

private fun filePrOffset(
    node: TreemapNode,
    zoom: Float,
    pan: Offset,
): IntOffset = IntOffset(
    x = (node.rect.left * zoom + pan.x + NODE_LABEL_X_OFFSET_PX).toInt(),
    y = (node.rect.top * zoom + pan.y + FILE_PR_Y_OFFSET_PX).toInt(),
)

private fun tooltipOffset(pointerPos: Offset): IntOffset = IntOffset(
    x = (pointerPos.x + TOOLTIP_POINTER_OFFSET_PX).toInt(),
    y = (pointerPos.y + TOOLTIP_POINTER_OFFSET_PX).toInt(),
)

private fun tooltipPrText(
    node: TreemapNode,
    prs: List<io.github.hayatoyagi.prvisualizer.PullRequest>,
): String {
    if (prs.isEmpty()) return "PR: none"
    val prDetails = prs.joinToString { pr ->
        val fileChange = pr.files.find { it.path == node.path }
        if (fileChange != null && !node.isDirectory) {
            "#${pr.number} ${pr.author} (+${fileChange.additions}/-${fileChange.deletions})"
        } else {
            "#${pr.number} ${pr.author}"
        }
    }
    return "PR: $prDetails"
}
