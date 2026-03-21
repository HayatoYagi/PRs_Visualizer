package io.github.hayatoyagi.prvisualizer.state

import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.FileCommit
import io.github.hayatoyagi.prvisualizer.PullRequest
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
        val filterState: FilterState = FilterState(),
        val navigationState: NavigationState = NavigationState(),
        val colorState: ColorState = ColorState(),
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

    data class AuthError(
        val error: AppError,
    ) : DialogState

    data class SnapshotFetchError(
        val error: AppError,
    ) : DialogState

    data class FileDetails(
        val filePath: String,
        val defaultBranch: String,
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

data class FilterState(
    val showDrafts: Boolean = true,
    val onlyMine: Boolean = false,
    val prSelection: PrSelection = PrSelection.allVisible(),
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
 *
 * FilterState, NavigationState, and ColorState are scoped to [SnapshotFetchState.Ready]
 * because they only have meaning when a snapshot is loaded.
 */
data class VisualizerState(
    val repoSelectionState: RepoSelectionState = RepoSelectionState.Idle,
    val dialogState: DialogState = DialogState.None,
    val authState: AuthState = AuthState.Unauthenticated,
    val snapshotFetchState: SnapshotFetchState = SnapshotFetchState.Idle,
) {
    val currentUser: String
        get() = when (val fetchState = snapshotFetchState) {
            is SnapshotFetchState.Ready -> fetchState.snapshot.viewerLogin.orEmpty()
            SnapshotFetchState.Fetching, SnapshotFetchState.Idle, is SnapshotFetchState.Failed -> ""
        }
}

fun NavigationState.resetNavigation(): NavigationState = copy(
    focusPath = "",
    selectedPath = null,
)

fun NavigationState.resetViewport(): NavigationState = copy(viewportResetToken = viewportResetToken + 1)

/**
 * Resets state when changing repositories.
 * Clears snapshot (which includes filter, navigation, and color state).
 * Auth errors are cleared back to unauthenticated.
 */
fun VisualizerState.resetForRepositoryChange(): VisualizerState = copy(
    dialogState = DialogState.None,
    snapshotFetchState = SnapshotFetchState.Idle,
    authState = if (authState is AuthState.Failed) AuthState.Unauthenticated else authState,
)
