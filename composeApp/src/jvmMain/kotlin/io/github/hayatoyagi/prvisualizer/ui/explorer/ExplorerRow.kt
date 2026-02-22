package io.github.hayatoyagi.prvisualizer.ui.explorer

import io.github.hayatoyagi.prvisualizer.ChangeType

data class ExplorerRow(
    val path: String,
    val name: String,
    val depth: Int,
    val isDirectory: Boolean,
    val dominantType: ChangeType?,
    val hasConflict: Boolean,
)
