package io.github.hayatoyagi.prvisualizer.ui.shared

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.TreemapNode
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

fun authorColor(author: String): Color {
    val palette = listOf(
        Color(0xFF4FC3F7),
        Color(0xFF81C784),
        Color(0xFFFF8A65),
        Color(0xFFBA68C8),
        Color(0xFFFFD54F),
        Color(0xFF90A4AE),
        Color(0xFF64B5F6),
        Color(0xFFA5D6A7),
        Color(0xFFFFB74D),
        Color(0xFFE57373),
        Color(0xFF4DB6AC),
        Color(0xFFF06292),
        Color(0xFF7986CB),
        Color(0xFFFFA726),
        Color(0xFF26A69A),
        Color(0xFFDCE775),
        Color(0xFF9575CD),
        Color(0xFFFF7043),
        Color(0xFF29B6F6),
        Color(0xFF8D6E63),
        Color(0xFF9CCC65),
        Color(0xFFFFCA28),
        Color(0xFF66BB6A),
        Color(0xFF42A5F5),
    )
    return palette[(author.hashCode().ushr(1)) % palette.size]
}

fun prColor(pr: PullRequest): Color {
    return authorColor("${pr.author}:${pr.number}")
}

fun findDirectory(root: FileNode.Directory, path: String): FileNode.Directory? {
    if (path.isBlank()) return root
    if (root.path == path) return root
    root.children.forEach { child ->
        if (child is FileNode.Directory) {
            val found = findDirectory(child, path)
            if (found != null) return found
        }
    }
    return null
}

fun parentPathOf(path: String): String {
    if (path.isBlank()) return ""
    return path.substringBeforeLast('/', missingDelimiterValue = "")
}

fun nodeKey(node: TreemapNode): String {
    return if (node.isDirectory) "D:${node.path}" else "F:${node.path}"
}

fun totalLines(node: FileNode): Int {
    return when (node) {
        is FileNode.File -> node.totalLines
        is FileNode.Directory -> node.children.sumOf(::totalLines)
    }
}

fun DrawScope.drawPrBorder(
    topLeft: Offset,
    size: Size,
    prs: List<PullRequest>,
    fallback: Color,
    borderWidth: Float,
) {
    val uniquePrs = prs.distinctBy { it.id }
    when {
        uniquePrs.isEmpty() -> {
            drawRect(
                color = fallback,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = borderWidth),
            )
        }

        uniquePrs.size == 1 -> {
            drawRect(
                color = prColor(uniquePrs.first()),
                topLeft = topLeft,
                size = size,
                style = Stroke(width = borderWidth),
            )
        }

        else -> {
            drawDashedMulticolorRectBorder(
                topLeft = topLeft,
                size = size,
                colors = uniquePrs.map(::prColor),
                strokeWidth = borderWidth,
                dashLength = 14f,
                gapLength = 8f,
            )
        }
    }
}

private fun DrawScope.drawDashedMulticolorRectBorder(
    topLeft: Offset,
    size: Size,
    colors: List<Color>,
    strokeWidth: Float,
    dashLength: Float,
    gapLength: Float,
) {
    if (colors.isEmpty()) return
    val perimeter = (size.width + size.height) * 2f
    if (perimeter <= 0f) return

    var cursor = 0f
    var dashIndex = 0
    while (cursor < perimeter) {
        val dashStart = cursor
        val dashEnd = (cursor + dashLength).coerceAtMost(perimeter)
        val color = colors[dashIndex % colors.size]
        drawPerimeterSegment(
            topLeft = topLeft,
            size = size,
            startDistance = dashStart,
            endDistance = dashEnd,
            color = color,
            strokeWidth = strokeWidth,
        )
        dashIndex += 1
        cursor += dashLength + gapLength
    }
}

private fun DrawScope.drawPerimeterSegment(
    topLeft: Offset,
    size: Size,
    startDistance: Float,
    endDistance: Float,
    color: Color,
    strokeWidth: Float,
) {
    val breaks = listOf(
        0f,
        size.width,
        size.width + size.height,
        size.width * 2f + size.height,
        size.width * 2f + size.height * 2f,
    )

    var segmentStart = startDistance
    while (segmentStart < endDistance) {
        val edgeEnd = breaks.first { it > segmentStart }
        val segmentEnd = minOf(endDistance, edgeEnd)
        val from = pointOnRectPerimeter(topLeft, size, segmentStart)
        val to = pointOnRectPerimeter(topLeft, size, segmentEnd)
        drawLine(
            color = color,
            start = from,
            end = to,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt,
        )
        segmentStart = segmentEnd
    }
}

private fun pointOnRectPerimeter(topLeft: Offset, size: Size, distance: Float): Offset {
    val w = size.width
    val h = size.height
    val p = (2f * (w + h)).coerceAtLeast(1f)
    val d = ((distance % p) + p) % p

    return when {
        d <= w -> Offset(topLeft.x + d, topLeft.y)
        d <= w + h -> Offset(topLeft.x + w, topLeft.y + (d - w))
        d <= 2f * w + h -> Offset(topLeft.x + (2f * w + h - d), topLeft.y + h)
        else -> Offset(topLeft.x, topLeft.y + (p - d))
    }
}

fun buildExplorerRows(root: FileNode.Directory): List<ExplorerRow> {
    val rows = mutableListOf<ExplorerRow>()

    fun visit(node: FileNode, depth: Int) {
        rows += ExplorerRow(
            path = node.path,
            name = if (node.path.isBlank()) "repo" else node.name,
            depth = depth,
            isDirectory = node is FileNode.Directory,
        )
        if (node is FileNode.Directory) {
            node.children
                .sortedWith(
                    compareBy<FileNode> { it !is FileNode.Directory }
                        .thenBy { it.name.lowercase() },
                )
                .forEach { child ->
                    visit(child, depth + 1)
                }
        }
    }

    visit(root, depth = 0)
    return rows
}

fun openUrl(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}

fun copyToClipboard(text: String) {
    runCatching {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }
}
