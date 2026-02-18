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
import androidx.compose.ui.Alignment
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
        if (widthPx <= 90f || heightPx <= 24f) return@forEach
        val label = if (node.isDirectory) "${node.name}/" else node.name
        Text(
            text = label,
            color = if (node.isDirectory) Color(0xFFFFD37A) else Color(0xFFDAE8F3),
            style = MaterialTheme.typography.labelSmall,
            modifier = modifier
                .offset {
                    IntOffset(
                        x = (node.rect.left * zoom + pan.x + 6f).toInt(),
                        y = (node.rect.top * zoom + pan.y + 4f).toInt(),
                    )
                },
        )
    }

    visibleFiles.forEach { node ->
        val overlay = fileOverlayByPath[node.path] ?: return@forEach
        val widthPx = node.rect.width * zoom
        val heightPx = node.rect.height * zoom
        if (widthPx <= 120f || heightPx <= 40f) return@forEach
        val prText = overlay.prs.take(3).joinToString(" ") { "#${it.number}" }
        val suffix = if (overlay.prs.size > 3) " +" else ""
        Text(
            text = "$prText$suffix",
            color = Color(0xFFEAF2F8),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
                .offset {
                    IntOffset(
                        x = (node.rect.left * zoom + pan.x + 6f).toInt(),
                        y = (node.rect.top * zoom + pan.y + 18f).toInt(),
                    )
                },
        )
    }

    if (hoveredNode != null) {
        Surface(
            modifier = modifier
                .offset {
                    IntOffset(
                        x = (pointerPos.x + 12f).toInt(),
                        y = (pointerPos.y + 12f).toInt(),
                    )
                }
                .border(1.dp, Color(0xFF3E5A72)),
            color = Color(0xFF12212F),
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(hoveredNode.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (hoveredNode.isDirectory) "Type: Directory" else "Type: File",
                    color = Color(0xFFC5D8E7),
                )
                Text("Path: ${hoveredNode.path}", color = Color(0xFFC5D8E7))
                Text("LOC: ${hoveredNode.totalLines}", color = Color(0xFFC5D8E7))
                val prs = if (hoveredNode.isDirectory) hoveredDirOverlay?.prs.orEmpty() else hoveredOverlay?.prs.orEmpty()
                Text(
                    text = if (prs.isEmpty()) "PR: none" else "PR: ${prs.joinToString { "#${it.number} ${it.author}" }}",
                    color = if (prs.size > 1) Color(0xFFFFD37A) else Color(0xFFC5D8E7),
                )
            }
        }
    }
}
