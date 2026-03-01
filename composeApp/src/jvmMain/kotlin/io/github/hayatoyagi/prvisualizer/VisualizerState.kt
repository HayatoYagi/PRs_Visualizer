package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot

sealed interface RepoSelectionState {
    data object Idle : RepoSelectionState

    data object Loading : RepoSelectionState

    data class Ready(
        val options: List<String>,
    ) : RepoSelectionState

    data class Error(
        val options: List<String>,
        val error: AppError,
    ) : RepoSelectionState
}

sealed interface AuthState {
    data object Unauthenticated : AuthState

    data class Authorizing(
        val deviceUserCode: String? = null,
        val deviceVerificationUrl: String? = null,
    ) : AuthState

    data class Authenticated(
        val oauthToken: String,
    ) : AuthState

    data class Failed(
        val error: AppError,
    ) : AuthState
}

sealed interface SnapshotFetchState {
    data object Idle : SnapshotFetchState

    data object Fetching : SnapshotFetchState

    data class Ready(
        val snapshot: GitHubSnapshot,
    ) : SnapshotFetchState

    data class Failed(
        val error: AppError,
    ) : SnapshotFetchState
}

/**
 * Represents dialog-related state.
 */
sealed interface DialogState {
    data object None : DialogState

    data object RepoPicker : DialogState

    data class FileDetails(
        val filePath: String,
        val commitsState: CommitsState = CommitsState.Loading,
    ) : DialogState {
        sealed interface CommitsState {
            data object Loading : CommitsState

            data class Ready(
                val commits: List<FileCommit>,
            ) : CommitsState

            data class Failed(
                val error: AppError,
            ) : CommitsState
        }
    }

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
)

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
    val repoSelectionState: RepoSelectionState = RepoSelectionState.Idle,
    val dialogState: DialogState = DialogState.None,
    val filterState: FilterState = FilterState(),
    val navigationState: NavigationState = NavigationState(),
    val colorState: ColorState = ColorState(),
    val authState: AuthState = AuthState.Unauthenticated,
    val snapshotFetchState: SnapshotFetchState = SnapshotFetchState.Idle,
) {
    val currentUser: String
        get() = when (val fetchState = snapshotFetchState) {
            is SnapshotFetchState.Ready -> fetchState.snapshot.viewerLogin.orEmpty()
            SnapshotFetchState.Fetching, SnapshotFetchState.Idle, is SnapshotFetchState.Failed -> ""
        }
}

/**
 * Resets navigation to the root directory.
 *
 * @return Updated navigation state
 */
fun NavigationState.resetNavigation(): NavigationState = copy(
    focusPath = "",
    selectedPath = null,
)

/**
 * Resets the viewport by incrementing the reset token.
 *
 * @return Updated navigation state
 */
fun NavigationState.resetViewport(): NavigationState = copy(viewportResetToken = viewportResetToken + 1)

/**
 * Resets state when changing repositories.
 * Keeps toggle filters while clearing selection, colors, and navigation.
 * Clears fetched snapshot/error; auth errors are cleared back to unauthenticated.
 */
fun VisualizerState.resetForRepositoryChange(): VisualizerState = copy(
    dialogState = DialogState.None,
    filterState = filterState.copy(selectedPrIds = emptySet()),
    navigationState = NavigationState(),
    colorState = ColorState(),
    snapshotFetchState = SnapshotFetchState.Idle,
    authState = if (authState is AuthState.Failed) AuthState.Unauthenticated else authState,
)
