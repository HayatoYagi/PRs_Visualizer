package io.github.hayatoyagi.prvisualizer.ui.prlist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import io.github.hayatoyagi.prvisualizer.PullRequest

data class PrListUiState(
    val filteredPrs: List<PullRequest>,
    val selectedPrIds: Set<String>,
    val selectedPrs: List<PullRequest>,
    val selectedPath: String?,
    val prColorMap: Map<String, Color>,
    val showDrafts: Boolean,
    val onlyMine: Boolean,
    val visiblePrCount: Int,
    val selectAllState: ToggleableState,
)

data class PrListActions(
    val onShowDraftsChange: (Boolean) -> Unit,
    val onOnlyMineChange: (Boolean) -> Unit,
    val onTogglePr: (prId: String, checked: Boolean) -> Unit,
    val onOpenPr: (PullRequest) -> Unit,
    val onCyclePrColor: (String) -> Unit,
    val onShuffleColors: () -> Unit,
    val onToggleSelectAll: () -> Unit,
)

fun buildPrListUiState(
    filteredPrs: List<PullRequest>,
    selectedPrIds: Set<String>,
    selectedPrs: List<PullRequest>,
    selectedPath: String?,
    prColorMap: Map<String, Color>,
    showDrafts: Boolean,
    onlyMine: Boolean,
    selectAllState: ToggleableState,
): PrListUiState = PrListUiState(
    filteredPrs = filteredPrs,
    selectedPrIds = selectedPrIds,
    selectedPrs = selectedPrs,
    selectedPath = selectedPath,
    prColorMap = prColorMap,
    showDrafts = showDrafts,
    onlyMine = onlyMine,
    visiblePrCount = selectedPrIds.size,
    selectAllState = selectAllState,
)
