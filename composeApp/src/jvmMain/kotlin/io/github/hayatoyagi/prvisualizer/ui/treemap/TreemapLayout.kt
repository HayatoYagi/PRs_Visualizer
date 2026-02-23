package io.github.hayatoyagi.prvisualizer.ui.treemap

import androidx.compose.ui.geometry.Rect
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.TreemapNode

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
        val positiveWeightChildren = sortedChildren.filter { it.weight > 0.0 }
        if (positiveWeightChildren.isEmpty()) return

        val totalWeight = positiveWeightChildren.sumOf { it.weight }

        var currentRow: List<FileNode> = emptyList()
        var remainingItems = positiveWeightChildren
        var remainingBounds = bounds
        var remainingWeight = totalWeight

        while (remainingItems.isNotEmpty() && remainingWeight > 0.0) {
            val next = remainingItems.first()
            val newRow = currentRow + next

            if (currentRow.isEmpty() || improvesRatio(currentRow, newRow, remainingBounds, remainingWeight)) {
                currentRow = newRow
                remainingItems = remainingItems.drop(1)
            } else {
                val rowWeight = currentRow.sumOf { it.weight }
                remainingBounds = layoutRow(currentRow, remainingBounds, remainingWeight, depth)
                remainingWeight -= rowWeight
                currentRow = emptyList()
            }
        }

        if (currentRow.isNotEmpty() && remainingWeight > 0.0) {
            layoutRow(currentRow, remainingBounds, remainingWeight, depth)
        }
    }

    // Check if adding a new item to the current row improves the worst aspect ratio
    private fun improvesRatio(
        current: List<FileNode>,
        newCurrent: List<FileNode>,
        bounds: Rect,
        totalWeight: Double,
    ): Boolean {
        val currentWorst = worstAspectRatio(current, bounds, totalWeight)
        val newWorst = worstAspectRatio(newCurrent, bounds, totalWeight)
        return newWorst <= currentWorst
    }

    // Calculate the worst (highest) aspect ratio in a row
    private fun worstAspectRatio(row: List<FileNode>, bounds: Rect, totalWeight: Double): Double {
        if (row.isEmpty()) return Double.MAX_VALUE

        val rowWeight = row.sumOf { it.weight }
        if (totalWeight <= 0.0 || rowWeight <= 0.0) return Double.MAX_VALUE
        val rowRatio = rowWeight / totalWeight
        val isHorizontal = bounds.width >= bounds.height
        val stripSize = stripSize(bounds = bounds, rowRatio = rowRatio, isHorizontal = isHorizontal)

        return row.maxOf { child ->
            val childSize = childSizeInStrip(
                childWeight = child.weight,
                rowWeight = rowWeight,
                stripWidth = stripSize.first,
                stripHeight = stripSize.second,
                isHorizontal = isHorizontal,
            )
            aspectRatioOf(childSize.first, childSize.second)
        }
    }

    // Layout a row of items and return the remaining bounds
    private fun layoutRow(row: List<FileNode>, bounds: Rect, totalWeight: Double, depth: Int): Rect {
        if (row.isEmpty()) return bounds

        val rowWeight = row.sumOf { it.weight }
        if (totalWeight <= 0.0 || rowWeight <= 0.0) return bounds
        val rowRatio = (rowWeight / totalWeight).toFloat()

        val isHorizontal = bounds.width >= bounds.height

        val (rowBounds, remainingBounds) = splitRowBounds(bounds = bounds, rowRatio = rowRatio, isHorizontal = isHorizontal)

        // Subdivide the row among its items
        var cursor = if (isHorizontal) rowBounds.top else rowBounds.left

        row.forEachIndexed { index, child ->
            val childRatio = (child.weight / rowWeight).toFloat()
            val childRect = childRectInRow(
                rowBounds = rowBounds,
                isHorizontal = isHorizontal,
                cursor = cursor,
                childRatio = childRatio,
                isLast = index == row.lastIndex,
            )
            cursor = if (isHorizontal) childRect.bottom else childRect.right
            layout(child, childRect, depth)
        }

        return remainingBounds
    }

    private fun splitRowBounds(
        bounds: Rect,
        rowRatio: Float,
        isHorizontal: Boolean,
    ): Pair<Rect, Rect> = if (isHorizontal) {
        val rowWidth = bounds.width * rowRatio
        val rowRect = Rect(bounds.left, bounds.top, bounds.left + rowWidth, bounds.bottom)
        val remaining = Rect(bounds.left + rowWidth, bounds.top, bounds.right, bounds.bottom)
        Pair(rowRect, remaining)
    } else {
        val rowHeight = bounds.height * rowRatio
        val rowRect = Rect(bounds.left, bounds.top, bounds.right, bounds.top + rowHeight)
        val remaining = Rect(bounds.left, bounds.top + rowHeight, bounds.right, bounds.bottom)
        Pair(rowRect, remaining)
    }

    private fun childRectInRow(
        rowBounds: Rect,
        isHorizontal: Boolean,
        cursor: Float,
        childRatio: Float,
        isLast: Boolean,
    ): Rect = if (isHorizontal) {
        val bottom = if (isLast) rowBounds.bottom else cursor + rowBounds.height * childRatio
        Rect(rowBounds.left, cursor, rowBounds.right, bottom)
    } else {
        val right = if (isLast) rowBounds.right else cursor + rowBounds.width * childRatio
        Rect(cursor, rowBounds.top, right, rowBounds.bottom)
    }

    private fun stripSize(
        bounds: Rect,
        rowRatio: Double,
        isHorizontal: Boolean,
    ): Pair<Float, Float> {
        val stripWidth = if (isHorizontal) bounds.width * rowRatio.toFloat() else bounds.width
        val stripHeight = if (isHorizontal) bounds.height else bounds.height * rowRatio.toFloat()
        return Pair(stripWidth, stripHeight)
    }

    private fun childSizeInStrip(
        childWeight: Double,
        rowWeight: Double,
        stripWidth: Float,
        stripHeight: Float,
        isHorizontal: Boolean,
    ): Pair<Float, Float> {
        val childRatio = childWeight / rowWeight
        val width = if (isHorizontal) stripWidth else (stripWidth * childRatio).toFloat()
        val height = if (isHorizontal) (stripHeight * childRatio).toFloat() else stripHeight
        return Pair(width, height)
    }

    private fun aspectRatioOf(width: Float, height: Float): Double {
        if (width <= 0f || height <= 0f) return Double.MAX_VALUE
        return maxOf(width / height, height / width).toDouble()
    }
}
