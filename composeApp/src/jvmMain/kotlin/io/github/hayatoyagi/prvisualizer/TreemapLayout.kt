package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.geometry.Rect

fun computeTreemap(
    root: FileNode.Directory,
    bounds: Rect,
): List<TreemapNode> {
    val nodes = mutableListOf<TreemapNode>()

    // TODO: Precompute/memoize subtree aggregates to avoid repeated recursive scans on large trees.
    fun totalLines(node: FileNode): Int = when (node) {
        is FileNode.File -> node.totalLines
        is FileNode.Directory -> node.children.sumOf { totalLines(it) }
    }

    // TODO: Precompute/memoize subtree aggregates to avoid repeated recursive scans on large trees.
    fun hasActivePr(node: FileNode): Boolean = when (node) {
        is FileNode.File -> node.hasActivePr
        is FileNode.Directory -> node.children.any { hasActivePr(it) }
    }

    fun layout(
        node: FileNode,
        rect: Rect,
        depth: Int,
        horizontal: Boolean,
    ) {
        if (rect.width <= 0f || rect.height <= 0f) return

        nodes += TreemapNode(
            path = node.path,
            name = node.name,
            rect = rect,
            depth = depth,
            isDirectory = node is FileNode.Directory,
            totalLines = totalLines(node),
            hasActivePr = hasActivePr(node),
        )

        val directory = node as? FileNode.Directory ?: return
        if (directory.children.isEmpty()) return

        val totalWeight = directory.children.sumOf { it.weight }.coerceAtLeast(1.0)
        var cursor = if (horizontal) rect.left else rect.top

        directory.children.forEachIndexed { index, child ->
            val ratio = (child.weight / totalWeight).toFloat()
            val childRect = if (horizontal) {
                val right = if (index == directory.children.lastIndex) rect.right else cursor + rect.width * ratio
                Rect(cursor, rect.top, right, rect.bottom)
            } else {
                val bottom = if (index == directory.children.lastIndex) rect.bottom else cursor + rect.height * ratio
                Rect(rect.left, cursor, rect.right, bottom)
            }
            cursor = if (horizontal) childRect.right else childRect.bottom
            layout(child, childRect, depth + 1, !horizontal)
        }
    }

    layout(root, bounds, depth = 0, horizontal = bounds.width >= bounds.height)
    return nodes
}
