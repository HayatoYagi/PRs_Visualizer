package io.github.hayatoyagi.prvisualizer.ui.prlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.state.FilterState

data class PrListUiState(
    val filteredPrs: List<PullRequest>,
    val visibleIds: Set<String>,
    val selectedPrIds: Set<String>,
    val visiblePrs: List<PullRequest>,
    val selectedPath: String?,
    val prColorMap: Map<String, Color>,
    val showDrafts: Boolean,
    val onlyMine: Boolean,
    val visiblePrCount: Int,
    val selectAllState: ToggleableState,
    val isLoading: Boolean,
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

@Composable
fun rememberPrListUiState(
    allPrs: List<PullRequest>,
    filterState: FilterState,
    currentUser: String,
    selectedPath: String?,
    prColorMap: Map<String, Color>,
    isLoading: Boolean,
): PrListUiState {
    val filteredPrs = remember(
        filterState.showDrafts,
        filterState.onlyMine,
        allPrs,
        currentUser,
    ) {
        filterPrs(allPrs, filterState.showDrafts, filterState.onlyMine, currentUser)
    }
    val visibleIds = remember(filteredPrs) {
        filteredPrs.map { it.id }.toSet()
    }
    val selectedPrIds = remember(filterState.prSelection, visibleIds) {
        filterState.prSelection.resolve(visibleIds)
    }
    val visiblePrs = remember(filteredPrs, selectedPrIds) {
        filteredPrs.filter { selectedPrIds.contains(it.id) }
    }
    val selectAllState = remember(filterState.prSelection, visibleIds) {
        filterState.prSelection.triState(visibleIds)
    }
    val visiblePrCount = remember(selectedPrIds) {
        selectedPrIds.size
    }
    return PrListUiState(
        filteredPrs = filteredPrs,
        visibleIds = visibleIds,
        selectedPrIds = selectedPrIds,
        visiblePrs = visiblePrs,
        selectedPath = selectedPath,
        prColorMap = prColorMap,
        showDrafts = filterState.showDrafts,
        onlyMine = filterState.onlyMine,
        visiblePrCount = visiblePrCount,
        selectAllState = selectAllState,
        isLoading = isLoading,
    )
}
