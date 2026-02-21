package io.github.hayatoyagi.prvisualizer.ui.treemap

import io.github.hayatoyagi.prvisualizer.TreemapNode
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay

fun nodeKey(node: TreemapNode): String = if (node.isDirectory) "D:${node.path}" else "F:${node.path}"

fun computeConflictedDirectoryPaths(fileOverlayByPath: Map<String, FileOverlay>): Set<String> {
    val conflictedDirectories = mutableSetOf<String>()
    fileOverlayByPath.forEach { (filePath, overlay) ->
        if (overlay.prs.size <= 1) return@forEach
        val segments = filePath.split('/')
        var current = ""
        for (i in 0 until segments.lastIndex) {
            current = if (current.isEmpty()) segments[i] else "$current/${segments[i]}"
            conflictedDirectories += current
        }
    }
    return conflictedDirectories
}
