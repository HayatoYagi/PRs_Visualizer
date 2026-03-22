package io.github.hayatoyagi.prvisualizer.ui.prlist

import io.github.hayatoyagi.prvisualizer.PullRequest

data class PrListActions(
    val onShowDraftsChange: (Boolean) -> Unit,
    val onOnlyMineChange: (Boolean) -> Unit,
    val onTogglePr: (prId: String, checked: Boolean) -> Unit,
    val onOpenPr: (PullRequest) -> Unit,
    val onCyclePrColor: (String) -> Unit,
    val onShuffleColors: () -> Unit,
    val onToggleSelectAll: () -> Unit,
)
