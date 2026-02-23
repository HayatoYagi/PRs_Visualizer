package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot

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
sealed interface DialogState {
    data object None : DialogState

    data object RepoPicker : DialogState

    data class FileDetails(
        val filePath: String,
    ) : DialogState
}

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
    fun resetNavigation(): NavigationState = copy(
        focusPath = "",
        selectedPath = null,
    )

    fun resetViewport(): NavigationState = copy(viewportResetToken = viewportResetToken + 1)
}

/**
 * Represents PR color management state.
 */
data class ColorState(
    val prColorMap: Map<String, Color> = emptyMap(),
)

/**
 * Represents GitHub session and connectivity state.
 */
data class SessionState(
    val oauthToken: String = "",
    val currentUserOverride: String = "",
    val githubSnapshot: GitHubSnapshot? = null,
    val connectionError: AppError? = null,
    val isConnecting: Boolean = false,
    val isAuthorizing: Boolean = false,
    val deviceUserCode: String? = null,
    val deviceVerificationUrl: String? = null,
    val repositoryOptions: List<String> = emptyList(),
    val isLoadingRepositories: Boolean = false,
) {
    val currentUser: String
        get() = githubSnapshot?.viewerLogin ?: currentUserOverride
}

/**
 * Main state container for the VisualizerViewModel.
 * Groups all related states together for better organization and easier state management.
 */
data class VisualizerState(
    val repoState: RepoState = RepoState(),
    val dialogState: DialogState = DialogState.None,
    val filterState: FilterState = FilterState(),
    val navigationState: NavigationState = NavigationState(),
    val colorState: ColorState = ColorState(),
    val sessionState: SessionState = SessionState(),
) {
    /**
     * Resets state when changing repositories.
     * Keeps toggle filters while clearing query, selection, colors, and navigation.
     * Clears the snapshot and connection error so a fresh fetch is triggered.
     */
    fun resetForNewRepo(
        owner: String,
        repo: String,
    ): VisualizerState = copy(
        repoState = RepoState(owner, repo),
        dialogState = DialogState.None,
        filterState = filterState.copy(query = "", selectedPrIds = emptySet()),
        navigationState = NavigationState(),
        colorState = ColorState(),
        sessionState = sessionState.copy(githubSnapshot = null, connectionError = null),
    )
}
