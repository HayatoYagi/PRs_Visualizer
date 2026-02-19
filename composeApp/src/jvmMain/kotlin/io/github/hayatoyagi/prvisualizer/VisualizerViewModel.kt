package io.github.hayatoyagi.prvisualizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import io.github.hayatoyagi.prvisualizer.github.EnvConfig
import io.github.hayatoyagi.prvisualizer.ui.shared.parentPathOf
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import kotlin.random.Random

class VisualizerViewModel(
    initialOwner: String = EnvConfig.get("GITHUB_OWNER") ?: "HayatoYagi",
    initialRepo: String = EnvConfig.get("GITHUB_REPO") ?: "GitHub_PRs_Visualizer",
) : ViewModel() {

    // Main state container
    var state by mutableStateOf(
        VisualizerState(
            repoState = RepoState(owner = initialOwner, repo = initialRepo),
        )
    )
        private set

    // Convenience accessors for backward compatibility
    val owner: String get() = state.repoState.owner
    val repo: String get() = state.repoState.repo
    val isRepoDialogOpen: Boolean get() = state.dialogState.isRepoDialogOpen
    val repoPickerQuery: String get() = state.dialogState.repoPickerQuery
    val showDrafts: Boolean get() = state.filterState.showDrafts
    val onlyMine: Boolean get() = state.filterState.onlyMine
    val query: String get() = state.filterState.query
    val selectedPrIds: Set<String> get() = state.filterState.selectedPrIds
    val prColorMap: Map<String, Color> get() = state.colorState.prColorMap
    val focusPath: String get() = state.navigationState.focusPath
    val selectedPath: String? get() = state.navigationState.selectedPath
    val viewportResetToken: Int get() = state.navigationState.viewportResetToken

    // Dialog intents
    fun openRepoDialog() {
        state = state.copy(
            dialogState = state.dialogState.copy(
                repoPickerQuery = "${state.repoState.owner}/${state.repoState.repo}".trim().trim('/'),
                isRepoDialogOpen = true,
            )
        )
    }

    fun closeRepoDialog() {
        state = state.copy(
            dialogState = state.dialogState.copy(isRepoDialogOpen = false)
        )
    }

    fun updateRepoPickerQuery(q: String) {
        state = state.copy(
            dialogState = state.dialogState.copy(repoPickerQuery = q)
        )
    }

    fun selectRepo(fullName: String) {
        val newOwner = fullName.substringBefore('/', state.repoState.owner)
        val newRepo = fullName.substringAfter('/', fullName)
        state = state.resetForNewRepo(owner = newOwner, repo = newRepo)
    }

    // PR filter intents
    fun updateShowDrafts(value: Boolean) {
        state = state.copy(
            filterState = state.filterState.copy(showDrafts = value)
        )
    }

    fun updateOnlyMine(value: Boolean) {
        state = state.copy(
            filterState = state.filterState.copy(onlyMine = value)
        )
    }

    fun updateQuery(value: String) {
        state = state.copy(
            filterState = state.filterState.copy(query = value)
        )
    }

    fun clearQuery() {
        state = state.copy(
            filterState = state.filterState.copy(query = "")
        )
    }

    fun togglePr(prId: String, checked: Boolean) {
        val newSelectedPrIds = if (checked) {
            state.filterState.selectedPrIds + prId
        } else {
            state.filterState.selectedPrIds - prId
        }
        state = state.copy(
            filterState = state.filterState.copy(selectedPrIds = newSelectedPrIds)
        )
    }

    fun selectAllPrs(available: Set<String>) {
        state = state.copy(
            filterState = state.filterState.copy(selectedPrIds = available)
        )
    }

    fun addRelatedPrs(related: Set<String>) {
        if (related.isNotEmpty()) {
            state = state.copy(
                filterState = state.filterState.copy(
                    selectedPrIds = state.filterState.selectedPrIds + related
                )
            )
        }
    }

    // Navigation intents
    fun selectDirectory(path: String) {
        state = state.copy(
            navigationState = state.navigationState.copy(
                focusPath = path,
                viewportResetToken = state.navigationState.viewportResetToken + 1,
            )
        )
    }

    fun selectFile(path: String) {
        state = state.copy(
            navigationState = state.navigationState.copy(
                selectedPath = path,
                focusPath = parentPathOf(path),
                viewportResetToken = state.navigationState.viewportResetToken + 1,
            )
        )
    }

    fun changeFocusPath(path: String) {
        state = state.copy(
            navigationState = state.navigationState.copy(
                focusPath = path,
                viewportResetToken = state.navigationState.viewportResetToken + 1,
            )
        )
    }

    fun updateSelectedPath(path: String?) {
        state = state.copy(
            navigationState = state.navigationState.copy(selectedPath = path)
        )
    }

    fun resetNavigation() {
        state = state.copy(
            navigationState = state.navigationState.resetNavigation()
        )
    }

    fun resetViewport() {
        state = state.copy(
            navigationState = state.navigationState.resetViewport()
        )
    }

    // PR color management intents
    fun ensurePrColors(prs: List<PullRequest>) {
        val prsNeedingColors = prs.filter { !state.colorState.prColorMap.containsKey(it.id) }
        if (prsNeedingColors.isNotEmpty()) {
            val newMap = state.colorState.prColorMap.toMutableMap()
            prsNeedingColors.forEach { pr ->
                newMap[pr.id] = randomColorAvoidingMap(newMap)
            }
            state = state.copy(
                colorState = state.colorState.copy(prColorMap = newMap)
            )
        }
    }

    fun shufflePrColors(prs: List<PullRequest>) {
        val newMap = mutableMapOf<String, Color>()
        prs.forEach { pr ->
            newMap[pr.id] = randomColorAvoidingMap(newMap)
        }
        state = state.copy(
            colorState = state.colorState.copy(prColorMap = newMap)
        )
    }

    fun cyclePrColor(prId: String) {
        val currentColor = state.colorState.prColorMap[prId]
        val currentIndex = if (currentColor != null) {
            AppColors.authorPalette.indexOf(currentColor)
        } else {
            -1
        }
        val nextIndex = (currentIndex + 1) % AppColors.authorPalette.size
        state = state.copy(
            colorState = state.colorState.copy(
                prColorMap = state.colorState.prColorMap + (prId to AppColors.authorPalette[nextIndex])
            )
        )
    }

    private fun randomColorAvoidingMap(assignedMap: Map<String, Color>): Color {
        // Avoid the 5 most recently assigned colors (map preserves insertion order)
        val recentColors = assignedMap.values.toList().takeLast(5).toSet()
        val availableColors = AppColors.authorPalette.filter { it !in recentColors }
        return if (availableColors.isNotEmpty()) {
            availableColors[Random.nextInt(availableColors.size)]
        } else {
            AppColors.authorPalette[Random.nextInt(AppColors.authorPalette.size)]
        }
    }
}
