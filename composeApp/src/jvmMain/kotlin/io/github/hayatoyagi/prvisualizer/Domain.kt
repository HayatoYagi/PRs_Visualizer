package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.geometry.Rect

sealed class FileNode {
    abstract val path: String
    abstract val name: String
    abstract val weight: Double

    data class Directory(
        override val path: String,
        override val name: String,
        val children: List<FileNode>,
        override val weight: Double,
    ) : FileNode()

    data class File(
        override val path: String,
        override val name: String,
        val extension: String,
        val totalLines: Int,
        val hasActivePr: Boolean,
        override val weight: Double,
    ) : FileNode()
}

enum class ChangeType {
    Addition,
    Modification,
    Deletion,
}

data class PullRequest(
    val id: String,
    val number: Int,
    val title: String,
    val author: String,
    val isDraft: Boolean,
    val url: String,
    val files: List<PrFileChange>,
)

data class PrFileChange(
    val path: String,
    val additions: Int,
    val deletions: Int,
) {
    val changedLines: Int = additions + deletions

    val changeType: ChangeType = when {
        additions > 0 && deletions == 0 -> ChangeType.Addition
        deletions > 0 && additions == 0 -> ChangeType.Deletion
        else -> ChangeType.Modification
    }
}

data class TreemapNode(
    val path: String,
    val name: String,
    val rect: Rect,
    val depth: Int,
    val isDirectory: Boolean,
    val totalLines: Int,
    val hasActivePr: Boolean,
)

data class FileCommit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String,
    val url: String,
)
