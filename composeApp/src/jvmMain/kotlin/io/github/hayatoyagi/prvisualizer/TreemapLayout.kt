package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.geometry.Rect

fun computeTreemap(root: FileNode.Directory, bounds: Rect): List<TreemapNode> {
    val engine = TreemapLayoutEngine()
    return engine.compute(root, bounds)
}

private class TreemapLayoutEngine {
    val nodes = mutableListOf<TreemapNode>()

    // TODO: Precompute/memoize subtree aggregates to avoid repeated recursive scans on large trees.
    private fun totalLines(node: FileNode): Int = when (node) {
        is FileNode.File -> node.totalLines
        is FileNode.Directory -> node.children.sumOf { totalLines(it) }
    }

    // TODO: Precompute/memoize subtree aggregates to avoid repeated recursive scans on large trees.
    private fun hasActivePr(node: FileNode): Boolean = when (node) {
        is FileNode.File -> node.hasActivePr
        is FileNode.Directory -> node.children.any { hasActivePr(it) }
    }

    fun compute(root: FileNode.Directory, bounds: Rect): List<TreemapNode> {
        layout(root, bounds, depth = 0)
        return nodes
    }

    private fun layout(node: FileNode, rect: Rect, depth: Int) {
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

        squarify(directory.children, rect, depth + 1)
    }

    // Squarified treemap algorithm to minimize aspect ratios
    private fun squarify(children: List<FileNode>, bounds: Rect, depth: Int) {
        if (children.isEmpty() || bounds.width <= 0f || bounds.height <= 0f) return

        val sortedChildren = children.sortedByDescending { it.weight }
        val totalWeight = sortedChildren.sumOf { it.weight }

        squarifyRecursive(sortedChildren, mutableListOf(), bounds, totalWeight, depth)
    }

    private fun squarifyRecursive(
        remaining: List<FileNode>,
        current: MutableList<FileNode>,
        bounds: Rect,
        totalWeight: Double,
        depth: Int
    ) {
        if (remaining.isEmpty()) {
            if (current.isNotEmpty()) {
                layoutRow(current, bounds, totalWeight, depth)
            }
            return
        }

        val next = remaining.first()
        val newCurrent = current + next

        if (current.isEmpty() || improvesRatio(current, newCurrent, bounds, totalWeight)) {
            // Adding next item improves or maintains aspect ratio
            squarifyRecursive(remaining.drop(1), newCurrent.toMutableList(), bounds, totalWeight, depth)
        } else {
            // Layout current row and continue with remaining items
            val rowWeight = current.sumOf { it.weight }
            val remainingBounds = layoutRow(current, bounds, totalWeight, depth)
            val remainingWeight = totalWeight - rowWeight
            squarifyRecursive(remaining, mutableListOf(), remainingBounds, remainingWeight, depth)
        }
    }

    // Check if adding a new item to the current row improves the worst aspect ratio
    private fun improvesRatio(
        current: List<FileNode>,
        newCurrent: List<FileNode>,
        bounds: Rect,
        totalWeight: Double
    ): Boolean {
        val currentWorst = worstAspectRatio(current, bounds, totalWeight)
        val newWorst = worstAspectRatio(newCurrent, bounds, totalWeight)
        return newWorst <= currentWorst
    }

    // Calculate the worst (highest) aspect ratio in a row
    private fun worstAspectRatio(row: List<FileNode>, bounds: Rect, totalWeight: Double): Double {
        if (row.isEmpty()) return Double.MAX_VALUE

        val rowWeight = row.sumOf { it.weight }
        val rowRatio = rowWeight / totalWeight

        // Determine if we're laying out horizontally or vertically
        val isHorizontal = bounds.width >= bounds.height

        // For horizontal layout: row takes full height, partial width
        // For vertical layout: row takes full width, partial height
        val rowShortSide = if (isHorizontal) {
            bounds.width * rowRatio.toFloat()
        } else {
            bounds.height * rowRatio.toFloat()
        }
        val rowLongSide = if (isHorizontal) bounds.height else bounds.width

        return row.maxOf { child ->
            val childRatio = child.weight / rowWeight
            val childSize = (rowLongSide * childRatio.toFloat())

            val w = if (isHorizontal) rowShortSide else childSize
            val h = if (isHorizontal) childSize else rowShortSide

            if (w <= 0f || h <= 0f) Double.MAX_VALUE
            else maxOf(w / h, h / w).toDouble()
        }
    }

    // Layout a row of items and return the remaining bounds
    private fun layoutRow(row: List<FileNode>, bounds: Rect, totalWeight: Double, depth: Int): Rect {
        if (row.isEmpty()) return bounds

        val rowWeight = row.sumOf { it.weight }
        val rowRatio = (rowWeight / totalWeight).toFloat()

        val isHorizontal = bounds.width >= bounds.height

        val (rowBounds, remainingBounds) = if (isHorizontal) {
            // Layout horizontally: row takes left portion of width
            val rowWidth = bounds.width * rowRatio
            val rowRect = Rect(bounds.left, bounds.top, bounds.left + rowWidth, bounds.bottom)
            val remaining = Rect(bounds.left + rowWidth, bounds.top, bounds.right, bounds.bottom)
            Pair(rowRect, remaining)
        } else {
            // Layout vertically: row takes top portion of height
            val rowHeight = bounds.height * rowRatio
            val rowRect = Rect(bounds.left, bounds.top, bounds.right, bounds.top + rowHeight)
            val remaining = Rect(bounds.left, bounds.top + rowHeight, bounds.right, bounds.bottom)
            Pair(rowRect, remaining)
        }

        // Subdivide the row among its items
        var cursor = if (isHorizontal) rowBounds.top else rowBounds.left

        row.forEachIndexed { index, child ->
            val childRatio = (child.weight / rowWeight).toFloat()
            val childRect = if (isHorizontal) {
                // Items arranged vertically within horizontal row
                val bottom = if (index == row.lastIndex) rowBounds.bottom else cursor + rowBounds.height * childRatio
                Rect(rowBounds.left, cursor, rowBounds.right, bottom)
            } else {
                // Items arranged horizontally within vertical row
                val right = if (index == row.lastIndex) rowBounds.right else cursor + rowBounds.width * childRatio
                Rect(cursor, rowBounds.top, right, rowBounds.bottom)
            }
            cursor = if (isHorizontal) childRect.bottom else childRect.right
            layout(child, childRect, depth)
        }

        return remainingBounds
    }
}
