package io.github.hayatoyagi.prvisualizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hayatoyagi.prvisualizer.color.ColorManager
import io.github.hayatoyagi.prvisualizer.github.session.FileCommitsService
import io.github.hayatoyagi.prvisualizer.github.session.FileCommitsServiceImpl
import io.github.hayatoyagi.prvisualizer.github.session.GitHubSessionManager
import io.github.hayatoyagi.prvisualizer.navigation.NavigationManager
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.repository.store.SelectedRepositoryStore
import io.github.hayatoyagi.prvisualizer.state.AuthState
import io.github.hayatoyagi.prvisualizer.state.ColorState
import io.github.hayatoyagi.prvisualizer.state.DialogState
import io.github.hayatoyagi.prvisualizer.state.NavigationState
import io.github.hayatoyagi.prvisualizer.state.PrSelection
import io.github.hayatoyagi.prvisualizer.state.SnapshotFetchState
import io.github.hayatoyagi.prvisualizer.state.VisualizerState
import io.github.hayatoyagi.prvisualizer.state.resetForRepositoryChange
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class VisualizerViewModel(
    private val selectedRepositoryStore: SelectedRepositoryStore,
    private val fileCommitsService: FileCommitsService = FileCommitsServiceImpl(),
    initialState: VisualizerState = VisualizerState(),
) : ViewModel() {
    val repoState: StateFlow<RepoState>
        get() = selectedRepositoryStore.repoState

    private var lastAppliedRepoState: RepoState = selectedRepositoryStore.repoState.value

    // Main state container
    var state by mutableStateOf(initialState)
        private set

    private var fileDetailsJob: Job? = null

    // Delegate color management to ColorManager
    private val colorManager = ColorManager(
        onStateChanged = { newColorState ->
            updateReady { copy(colorState = newColorState) }
        },
    )

    // Delegate navigation management to NavigationManager
    private val navigationManager = NavigationManager(
        onStateChanged = { newNavigationState ->
            updateReady { copy(navigationState = newNavigationState) }
        },
    )

    // region: セッション管理
    private val sessionManager = GitHubSessionManager(
        scope = viewModelScope,
        getAuthState = { state.authState },
        setAuthState = { authState ->
            state = state.copy(
                authState = authState,
                dialogState = if (authState is AuthState.Failed) {
                    DialogState.AuthError(authState.error)
                } else {
                    state.dialogState
                },
            )
        },
        getSnapshotFetchState = { state.snapshotFetchState },
        setSnapshotFetchState = { snapshotFetchState ->
            state = state.copy(
                snapshotFetchState = snapshotFetchState,
                dialogState = if (snapshotFetchState is SnapshotFetchState.Failed) {
                    DialogState.SnapshotFetchError(snapshotFetchState.error)
                } else {
                    state.dialogState
                },
            )
        },
        getRepoState = { selectedRepositoryStore.repoState.value },
        getRepoSelectionState = { state.repoSelectionState },
        setRepoSelectionState = { state = state.copy(repoSelectionState = it) },
        onSnapshotLoaded = {
            navigationManager.resetNavigation()
            navigationManager.resetViewport()
        },
        selectRepo = ::selectRepo,
        unselectRepo = { selectedRepositoryStore.unselect() },
    )

    /**
     * Updates the [SnapshotFetchState.Ready] state. No-op when snapshot is not ready.
     */
    private inline fun updateReady(block: SnapshotFetchState.Ready.() -> SnapshotFetchState.Ready) {
        val ready = state.snapshotFetchState as? SnapshotFetchState.Ready ?: return
        state = state.copy(snapshotFetchState = ready.block())
    }

    /**
     * Applies UI state changes for a repository transition.
     * [lastAppliedRepoState] prevents re-applying state when the same repo is selected again.
     */
    private fun applyRepositoryState(repoState: RepoState) {
        if (repoState == lastAppliedRepoState) return
        lastAppliedRepoState = repoState

        when (repoState) {
            is RepoState.Selected -> {
                state = state.resetForRepositoryChange()
                // Sync managers with reset state (Idle has no filter/nav/color)
                colorManager.updateState(ColorState())
                navigationManager.updateState(NavigationState())
            }
            RepoState.Unselected -> {
                state = state.copy(
                    snapshotFetchState = SnapshotFetchState.Idle,
                )
            }
        }
        navigationManager.clearHistory()
    }

    fun initializeSession() = sessionManager.initializeSession()

    fun loginAndConnect() = sessionManager.loginAndConnect()

    fun refresh() = sessionManager.refresh()

    fun ensureRepositoryOptions() = sessionManager.ensureRepositoryOptions()

    fun loadRepositoryOptions() = sessionManager.loadRepositoryOptions()

    fun dismissErrorDialog() {
        if (state.dialogState is DialogState.SnapshotFetchError || state.dialogState is DialogState.AuthError) {
            state = state.copy(dialogState = DialogState.None)
        }
    }

    fun logout() = sessionManager.logout()

    // region: ダイアログ管理
    fun openRepoDialog() {
        state = state.copy(
            dialogState = DialogState.RepoPicker,
        )
    }

    fun closeRepoDialog() {
        state = state.copy(
            dialogState = DialogState.None,
        )
    }

    fun openFileDetailsDialog(filePath: String) {
        fileDetailsJob?.cancel()
        val ready = checkNotNull(state.snapshotFetchState as? SnapshotFetchState.Ready) {
            "File details requires a ready snapshot."
        }
        state = state.copy(
            dialogState = DialogState.FileDetails(
                filePath = filePath,
                defaultBranch = ready.snapshot.defaultBranch,
                commitsState = DialogState.FileDetails.CommitsState.Loading,
            ),
        )
        fileDetailsJob = loadFileDetailsCommits(filePath)
    }

    fun openPrDetailsDialog(pr: PullRequest) {
        state = state.copy(
            dialogState = DialogState.PrDetails(pr = pr),
        )
    }

    fun closeDialog() {
        fileDetailsJob?.cancel()
        state = state.copy(
            dialogState = DialogState.None,
        )
    }

    fun reloadFileDetailsCommits() {
        val fileDetails = state.dialogState as? DialogState.FileDetails ?: return
        fileDetailsJob?.cancel()
        fileDetailsJob = loadFileDetailsCommits(fileDetails.filePath)
    }

    private fun loadFileDetailsCommits(filePath: String): Job? {
        val authState = state.authState as? AuthState.Authenticated ?: run {
            updateFileDetailsCommitsState(
                filePath = filePath,
                commitsState = DialogState.FileDetails.CommitsState.Failed(AppError.AuthExpired()),
            )
            return null
        }
        val selectedRepo = selectedRepositoryStore.repoState.value as? RepoState.Selected
            ?: return null
        updateFileDetailsCommitsState(filePath = filePath, commitsState = DialogState.FileDetails.CommitsState.Loading)
        return viewModelScope.launch {
            val commitsResult = fileCommitsService.fetchFileCommits(
                token = authState.oauthToken,
                owner = selectedRepo.owner,
                repo = selectedRepo.repo,
                path = filePath,
                limit = 10,
            )
            val commitsState = commitsResult.fold(
                onSuccess = { commits -> DialogState.FileDetails.CommitsState.Ready(commits) },
                onFailure = { error -> DialogState.FileDetails.CommitsState.Failed(AppError.from(error)) },
            )
            updateFileDetailsCommitsState(filePath = filePath, commitsState = commitsState)
        }
    }

    private fun updateFileDetailsCommitsState(
        filePath: String,
        commitsState: DialogState.FileDetails.CommitsState,
    ) {
        val dialogState = state.dialogState as? DialogState.FileDetails ?: return
        if (dialogState.filePath != filePath) return
        state = state.copy(
            dialogState = dialogState.copy(commitsState = commitsState),
        )
    }

    // region: リポジトリ選択
    fun selectRepo(fullName: String) {
        val currentOwner = (selectedRepositoryStore.repoState.value as? RepoState.Selected)?.owner.orEmpty()
        val newOwner = fullName.substringBefore('/', currentOwner)
        val newRepo = fullName.substringAfter('/', fullName)
        selectedRepositoryStore.select(owner = newOwner, repo = newRepo)
        applyRepositoryState(selectedRepositoryStore.repoState.value)
    }

    // region: PR フィルタ
    fun updateShowDrafts(value: Boolean) {
        updateReady { copy(filterState = filterState.copy(showDrafts = value)) }
    }

    fun updateOnlyMine(value: Boolean) {
        updateReady { copy(filterState = filterState.copy(onlyMine = value)) }
    }

    fun togglePr(prId: String, checked: Boolean) {
        val ready = state.snapshotFetchState as? SnapshotFetchState.Ready ?: return
        val visibleIds = ready.filteredPrs.map { it.id }.toSet()
        updateReady {
            copy(
                filterState = filterState.copy(
                    prSelection = filterState.prSelection.toggle(
                        prId = prId,
                        checked = checked,
                        visibleIds = visibleIds,
                    ),
                ),
            )
        }
    }

    fun toggleSelectAll() {
        val ready = state.snapshotFetchState as? SnapshotFetchState.Ready ?: return
        updateReady {
            val newSelection = when (ready.selectAllState) {
                ToggleableState.On -> PrSelection.none()
                ToggleableState.Off, ToggleableState.Indeterminate -> PrSelection.allVisible()
            }
            copy(filterState = filterState.copy(prSelection = newSelection))
        }
    }

    fun selectAllPrs() {
        updateReady { copy(filterState = filterState.copy(prSelection = PrSelection.allVisible())) }
    }

    fun deselectAllPrs() {
        updateReady { copy(filterState = filterState.copy(prSelection = PrSelection.none())) }
    }

    // region: ナビゲーション
    fun selectDirectory(path: String) = navigationManager.selectDirectory(path)

    fun selectFile(path: String) = navigationManager.selectFile(path)

    fun changeFocusPath(path: String) = navigationManager.changeFocusPath(path)

    fun updateSelectedPath(path: String?) = navigationManager.updateSelectedPath(path)

    fun resetNavigation() = navigationManager.resetNavigation()

    fun resetViewport() = navigationManager.resetViewport()

    fun toggleDirectoryExpanded(path: String) = navigationManager.toggleDirectoryExpanded(path)

    /**
     * Navigates back in history. Returns true if navigation occurred.
     */
    fun navigateBack(): Boolean = navigationManager.navigateBack()

    /**
     * Navigates forward in history. Returns true if navigation occurred.
     */
    fun navigateForward(): Boolean = navigationManager.navigateForward()

    // region: 色管理
    fun ensurePrColors(prs: List<PullRequest>) = colorManager.ensurePrColors(prs)

    fun shufflePrColors() {
        val ready = state.snapshotFetchState as? SnapshotFetchState.Ready ?: return
        colorManager.shufflePrColors(ready.snapshot.pullRequests)
    }

    fun cyclePrColor(prId: String) = colorManager.cyclePrColor(prId)
}
