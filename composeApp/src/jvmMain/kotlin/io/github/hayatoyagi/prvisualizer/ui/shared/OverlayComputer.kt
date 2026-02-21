package io.github.hayatoyagi.prvisualizer.ui.shared

import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.PullRequest

fun findDirectory(
    root: FileNode.Directory,
    path: String,
): FileNode.Directory? {
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

// Total lines for overlay density calculation (distinct from TreemapLayoutEngine's private version used for layout weights)
fun totalLines(node: FileNode): Int = when (node) {
    is FileNode.File -> node.totalLines
    is FileNode.Directory -> node.children.sumOf(::totalLines)
}

fun computeFileOverlayByPath(
    visiblePrs: List<PullRequest>,
    visibleFiles: List<FileNode.File>,
): Map<String, FileOverlay> {
    val fileLines = visibleFiles.associateBy({ it.path }, { it.totalLines.coerceAtLeast(1) })
    return visiblePrs
        .flatMap { pr -> pr.files.map { change -> pr to change } }
        .filter { (_, change) -> fileLines.containsKey(change.path) }
        .groupBy({ it.second.path }, { it })
        .mapValues { (path, items) ->
            val totalChanged = items.sumOf { it.second.changedLines }
            val dominant = items
                .groupBy { it.second.changeType }
                .maxByOrNull { it.value.sumOf { pair -> pair.second.changedLines } }
                ?.key ?: ChangeType.Modification
            val prs = items.map { it.first }.distinctBy { it.id }
            val lines = fileLines[path] ?: 1
            val density = (totalChanged.toFloat() / lines.toFloat()).coerceIn(0f, 1f)
            FileOverlay(prs = prs, dominantType = dominant, density = density)
        }
}

fun computeDirectoryOverlayByPath(
    visiblePrs: List<PullRequest>,
    visibleDirectories: List<FileNode.Directory>,
): Map<String, DirectoryOverlay> = visibleDirectories.associate { dir ->
    val relatedChanges = visiblePrs
        .flatMap { pr -> pr.files.map { change -> pr to change } }
        .filter { (_, change) -> change.path.startsWith("${dir.path}/") }

    val relatedPrs = relatedChanges.map { it.first }.distinctBy { it.id }
    val dominantType = relatedChanges
        .groupBy { it.second.changeType }
        .maxByOrNull { (_, items) -> items.sumOf { it.second.changedLines } }
        ?.key
    val totalChanged = relatedChanges.sumOf { it.second.changedLines }
    val density = (totalChanged.toFloat() / totalLines(dir).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    dir.path to DirectoryOverlay(
        prs = relatedPrs,
        dominantType = dominantType,
        density = density,
    )
}
