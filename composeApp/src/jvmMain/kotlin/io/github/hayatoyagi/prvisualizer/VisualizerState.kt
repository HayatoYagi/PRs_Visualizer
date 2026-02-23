package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot

data class RepoSelectionState(
    val options: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
)

data class AuthState(
    val oauthToken: String = "",
    val isAuthorizing: Boolean = false,
    val deviceUserCode: String? = null,
    val deviceVerificationUrl: String? = null,
    val error: AppError? = null,
) {
    val isLoggedIn: Boolean
        get() = oauthToken.isNotBlank()
}

data class SnapshotFetchState(
    val snapshot: GitHubSnapshot? = null,
    val isFetching: Boolean = false,
    val error: AppError? = null,
)

/**
 * Represents GitHub authentication and snapshot-fetch session.
 */
data class SessionState(
    val authState: AuthState = AuthState(),
    val snapshotFetchState: SnapshotFetchState = SnapshotFetchState(),
) {
    val currentUser: String
        get() = snapshotFetchState.snapshot?.viewerLogin.orEmpty()
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

    data class PrDetails(
        val pr: PullRequest,
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
 * Main state container for the VisualizerViewModel.
 */
data class VisualizerState(
    val repoSelectionState: RepoSelectionState = RepoSelectionState(),
    val dialogState: DialogState = DialogState.None,
    val filterState: FilterState = FilterState(),
    val navigationState: NavigationState = NavigationState(),
    val colorState: ColorState = ColorState(),
    val sessionState: SessionState = SessionState(),
) {
    /**
     * Resets state when changing repositories.
     * Keeps toggle filters while clearing query, selection, colors, and navigation.
     * Clears fetched snapshot and related errors so a fresh fetch is triggered.
     */
    fun resetForRepositoryChange(): VisualizerState = copy(
        dialogState = DialogState.None,
        filterState = filterState.copy(query = "", selectedPrIds = emptySet()),
        navigationState = NavigationState(),
        colorState = ColorState(),
        sessionState = sessionState.copy(
            snapshotFetchState = sessionState.snapshotFetchState.copy(snapshot = null, error = null),
            authState = sessionState.authState.copy(error = null),
        ),
    )
}
