package io.github.hayatoyagi.prvisualizer.github

import io.github.hayatoyagi.prvisualizer.FileNode

internal data class FileSeed(
    val path: String,
    val estimatedLines: Int,
)

internal fun buildTree(
    allFiles: List<FileSeed>,
    activePaths: Set<String>,
): FileNode.Directory {
    data class MutableDir(
        val path: String,
        val name: String,
        val children: MutableList<Any> = mutableListOf(),
    )

    val root = MutableDir(path = "", name = "repo")
    val dirsByPath = mutableMapOf("" to root)

    fun ensureDir(path: String): MutableDir =
        dirsByPath.getOrPut(path) {
            val parentPath = path.substringBeforeLast('/', missingDelimiterValue = "")
            val dirName = path.substringAfterLast('/')
            val parent = ensureDir(parentPath)
            val newDir = MutableDir(path = path, name = dirName)
            parent.children += newDir
            newDir
        }

    allFiles.forEach { file ->
        val parentPath = file.path.substringBeforeLast('/', missingDelimiterValue = "")
        val dir = ensureDir(parentPath)
        dir.children += file
    }

    fun freeze(dir: MutableDir): FileNode.Directory {
        val frozenChildren = dir.children.map { child ->
            when (child) {
                is MutableDir -> freeze(child)
                is FileSeed -> {
                    val extension = child.path.substringAfterLast('.', missingDelimiterValue = "")
                    FileNode.File(
                        path = child.path,
                        name = child.path.substringAfterLast('/'),
                        extension = extension,
                        totalLines = child.estimatedLines,
                        hasActivePr = activePaths.contains(child.path),
                        weight = maxOf(
                            child.estimatedLines.toDouble(),
                            if (activePaths.contains(child.path)) 8.0 else 1.0,
                        ),
                    )
                }
                else -> error("Unexpected node type")
            }
        }

        return FileNode.Directory(
            path = dir.path,
            name = if (dir.path.isEmpty()) "repo" else dir.name,
            children = frozenChildren.sortedByDescending { it.weight },
            weight = frozenChildren.sumOf { it.weight }.coerceAtLeast(1.0),
        )
    }

    return freeze(root)
}
