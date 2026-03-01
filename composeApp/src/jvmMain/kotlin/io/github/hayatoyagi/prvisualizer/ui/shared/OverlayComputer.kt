package io.github.hayatoyagi.prvisualizer.ui.shared

import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.PullRequest

/**
 * Finds a directory by path in the file tree.
 *
 * @param root The root directory to search from
 * @param path The path of the directory to find
 * @return The directory node if found, null otherwise
 */
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

/**
 * Finds a file node by path in the file tree.
 *
 * @param root The root directory to search from
 * @param path The path of the file to find
 * @return The file node if found, null otherwise
 */
fun findFileNode(
    root: FileNode.Directory,
    path: String,
): FileNode.File? {
    root.children.forEach { child ->
        when (child) {
            is FileNode.File -> {
                if (child.path == path) return child
            }
            is FileNode.Directory -> {
                val found = findFileNode(child, path)
                if (found != null) return found
            }
        }
    }
    return null
}

/**
 * Returns the parent path of a given path.
 *
 * @param path The path to get the parent of
 * @return The parent path, or empty string if no parent exists
 */
fun parentPathOf(path: String): String {
    if (path.isBlank()) return ""
    return path.substringBeforeLast('/', missingDelimiterValue = "")
}

/**
 * Calculates the total lines of code for a node and its children.
 *
 * @param node The file or directory node
 * @return Total number of lines
 */
fun totalLines(node: FileNode): Int = when (node) {
    is FileNode.File -> node.totalLines
    is FileNode.Directory -> node.children.sumOf(::totalLines)
}

/**
 * Computes which directories contain files with multiple PRs (conflicts).
 *
 * @param fileOverlayByPath Map of file paths to their overlay data
 * @return Set of directory paths that contain conflicted files
 */
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

/**
 * Computes overlay data for files affected by visible PRs.
 *
 * @param visiblePrs List of visible pull requests
 * @param visibleFiles List of visible file nodes
 * @return Map of file paths to their computed overlay data
 */
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
                ?.key
                ?: ChangeType.Modification
            val prs = items.map { it.first }.distinctBy { it.id }
            val lines = fileLines[path] ?: 1
            val density = (totalChanged.toFloat() / lines.toFloat()).coerceIn(0f, 1f)
            FileOverlay(prs = prs, dominantType = dominant, density = density)
        }
}

/**
 * Computes overlay data for directories affected by visible PRs.
 *
 * @param visiblePrs List of visible pull requests
 * @param visibleDirectories List of visible directory nodes
 * @return Map of directory paths to their computed overlay data
 */
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
