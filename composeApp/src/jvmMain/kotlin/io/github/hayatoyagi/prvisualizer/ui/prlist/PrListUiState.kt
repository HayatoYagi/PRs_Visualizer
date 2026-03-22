package io.github.hayatoyagi.prvisualizer.ui.prlist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.state.FilterState

data class PrListUiState(
    val filteredPrs: List<PullRequest>,
    val selectedPrIds: Set<String>,
    val visiblePrs: List<PullRequest>,
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
    allPrs: List<PullRequest>,
    filterState: FilterState,
    currentUser: String,
    selectedPath: String?,
    prColorMap: Map<String, Color>,
): PrListUiState {
    val filteredPrs = filterPrs(allPrs, filterState.showDrafts, filterState.onlyMine, currentUser)
    val visibleIds = filteredPrs.map { it.id }.toSet()
    val selectedPrIds = filterState.prSelection.resolve(visibleIds)
    val visiblePrs = filteredPrs.filter { selectedPrIds.contains(it.id) }
    return PrListUiState(
        filteredPrs = filteredPrs,
        selectedPrIds = selectedPrIds,
        visiblePrs = visiblePrs,
        selectedPath = selectedPath,
        prColorMap = prColorMap,
        showDrafts = filterState.showDrafts,
        onlyMine = filterState.onlyMine,
        visiblePrCount = selectedPrIds.size,
        selectAllState = filterState.prSelection.triState(visibleIds),
    )
}
