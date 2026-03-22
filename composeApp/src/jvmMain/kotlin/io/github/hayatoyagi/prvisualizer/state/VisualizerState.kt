package io.github.hayatoyagi.prvisualizer.state

import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.FileCommit
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrListUiState
import io.github.hayatoyagi.prvisualizer.ui.prlist.filterPrs
import io.github.hayatoyagi.prvisualizer.ui.shared.computeDirectoryOverlayByPath
import io.github.hayatoyagi.prvisualizer.ui.shared.computeFileOverlayByPath
import io.github.hayatoyagi.prvisualizer.ui.shared.findDirectory

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
    ) : SnapshotFetchState {
        val prListUiState = buildPrListUiState(
            allPrs = snapshot.pullRequests,
            filterState = filterState,
            currentUser = snapshot.viewerLogin.orEmpty(),
            selectedPath = navigationState.selectedPath,
            prColorMap = colorState.prColorMap,
        )
        val focusRoot = findDirectory(snapshot.rootNode, navigationState.focusPath) ?: snapshot.rootNode
        val fileOverlayByPath = computeFileOverlayByPath(prListUiState.visiblePrs, collectAllFiles(snapshot.rootNode))
        val directoryOverlayByPath = computeDirectoryOverlayByPath(
            prListUiState.visiblePrs,
            collectAllDirectories(snapshot.rootNode),
        )
    }

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

private fun buildPrListUiState(
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

private fun collectAllFiles(root: FileNode.Directory): List<FileNode.File> = buildList {
    fun collectFiles(node: FileNode) {
        when (node) {
            is FileNode.File -> add(node)
            is FileNode.Directory -> node.children.forEach(::collectFiles)
        }
    }
    collectFiles(root)
}

private fun collectAllDirectories(root: FileNode.Directory): List<FileNode.Directory> = buildList {
    fun collectDirectories(dir: FileNode.Directory) {
        add(dir)
        dir.children.forEach { child ->
            if (child is FileNode.Directory) collectDirectories(child)
        }
    }
    collectDirectories(root)
}

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
