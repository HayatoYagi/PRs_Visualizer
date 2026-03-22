package io.github.hayatoyagi.prvisualizer.ui.shared

import io.github.hayatoyagi.prvisualizer.state.FileOverlay

fun computeConflictedDirs(fileOverlayByPath: Map<String, FileOverlay>): Set<String> {
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
