package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.graphics.Color

/**
 * Represents the repository identity.
 */
data class RepoState(
    val owner: String = "",
    val repo: String = "",
) {
    val fullName: String get() = "$owner/$repo"
}

/**
 * Represents dialog-related state.
 */
data class DialogState(
    val isRepoDialogOpen: Boolean = false,
    val repoPickerQuery: String = "",
)

/**
 * Represents PR filter state.
 */
data class FilterState(
    val showDrafts: Boolean = true,
    val onlyMine: Boolean = false,
    val query: String = "",
    val selectedPrIds: Set<String> = emptySet(),
)

/**
 * Represents explorer expand/collapse state.
 */
data class ExplorerState(
    val expandedPaths: Set<String> = setOf(""), // Root is expanded by default
)

/**
 * Represents navigation state within the file tree.
 */
data class NavigationState(
    val focusPath: String = "",
    val selectedPath: String? = null,
    val viewportResetToken: Int = 0,
    val explorerState: ExplorerState = ExplorerState(),
) {
    fun resetNavigation(): NavigationState {
        return copy(
            focusPath = "",
            selectedPath = null,
        )
    }

    fun resetViewport(): NavigationState {
        return copy(viewportResetToken = viewportResetToken + 1)
    }
}

/**
 * Represents PR color management state.
 */
data class ColorState(
    val prColorMap: Map<String, Color> = emptyMap(),
)

/**
 * Main state container for the VisualizerViewModel.
 * Groups all related states together for better organization and easier state management.
 */
data class VisualizerState(
    val repoState: RepoState = RepoState(),
    val dialogState: DialogState = DialogState(),
    val filterState: FilterState = FilterState(),
    val navigationState: NavigationState = NavigationState(),
    val colorState: ColorState = ColorState(),
) {
    /**
     * Resets state when changing repositories.
     * Keeps toggle filters while clearing query, selection, colors, and navigation.
     */
    fun resetForNewRepo(owner: String, repo: String): VisualizerState {
        return copy(
            repoState = RepoState(owner, repo),
            dialogState = DialogState(isRepoDialogOpen = false, repoPickerQuery = ""),
            filterState = filterState.copy(query = "", selectedPrIds = emptySet()),
            navigationState = NavigationState(),
            colorState = ColorState(),
        )
    }
}
