package io.github.hayatoyagi.prvisualizer.filetree

import io.github.hayatoyagi.prvisualizer.FileNode

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

fun parentPathOf(path: String): String {
    if (path.isBlank()) return ""
    return path.substringBeforeLast('/', missingDelimiterValue = "")
}

fun totalLines(node: FileNode): Int = when (node) {
    is FileNode.File -> node.totalLines
    is FileNode.Directory -> node.children.sumOf(::totalLines)
}

fun collectAllFiles(root: FileNode.Directory): List<FileNode.File> = buildList {
    fun collectFiles(node: FileNode) {
        when (node) {
            is FileNode.File -> add(node)
            is FileNode.Directory -> node.children.forEach(::collectFiles)
        }
    }
    collectFiles(root)
}

fun collectAllDirectories(root: FileNode.Directory): List<FileNode.Directory> = buildList {
    fun collectDirectories(dir: FileNode.Directory) {
        add(dir)
        dir.children.forEach { child ->
            if (child is FileNode.Directory) collectDirectories(child)
        }
    }
    collectDirectories(root)
}
