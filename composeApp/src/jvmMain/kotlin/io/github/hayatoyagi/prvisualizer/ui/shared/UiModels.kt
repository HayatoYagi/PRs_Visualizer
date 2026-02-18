package io.github.hayatoyagi.prvisualizer.ui.shared

import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.PullRequest

data class FileOverlay(
    val prs: List<PullRequest>,
    val dominantType: ChangeType,
    val density: Float,
)

data class DirectoryOverlay(
    val prs: List<PullRequest>,
    val dominantType: ChangeType?,
    val density: Float,
)

data class ExplorerRow(
    val path: String,
    val name: String,
    val depth: Int,
    val isDirectory: Boolean,
)
