package io.github.hayatoyagi.prvisualizer.ui.explorer

import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.ui.shared.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.computeConflictedDirectoryPaths

fun buildExplorerRows(
    root: FileNode.Directory,
    fileOverlayByPath: Map<String, FileOverlay>,
    directoryOverlayByPath: Map<String, DirectoryOverlay>,
    expandedPaths: Set<String> = setOf(""),
): List<ExplorerRow> {
    val rows = mutableListOf<ExplorerRow>()
    val conflictedDirectoryPaths = computeConflictedDirectoryPaths(fileOverlayByPath)

    fun visit(
        node: FileNode,
        depth: Int,
    ) {
        val (dominantType, hasConflict) = when (node) {
            is FileNode.Directory -> {
                val overlay = directoryOverlayByPath[node.path]
                Pair(overlay?.dominantType, conflictedDirectoryPaths.contains(node.path))
            }
            is FileNode.File -> {
                val overlay = fileOverlayByPath[node.path]
                val prCount = overlay?.prs?.size ?: 0
                Pair(overlay?.dominantType, prCount > 1)
            }
        }

        rows += ExplorerRow(
            path = node.path,
            name = if (node.path.isBlank()) "repo" else node.name,
            depth = depth,
            isDirectory = node is FileNode.Directory,
            dominantType = dominantType,
            hasConflict = hasConflict,
        )
        if (node is FileNode.Directory && expandedPaths.contains(node.path)) {
            node.children
                .sortedWith(
                    compareBy<FileNode> { it !is FileNode.Directory }
                        .thenBy { it.name.lowercase() },
                ).forEach { child ->
                    visit(child, depth + 1)
                }
        }
    }

    visit(root, depth = 0)
    return rows
}
