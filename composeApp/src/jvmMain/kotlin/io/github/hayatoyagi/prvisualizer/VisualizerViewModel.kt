package io.github.hayatoyagi.prvisualizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hayatoyagi.prvisualizer.color.ColorManager
import io.github.hayatoyagi.prvisualizer.github.session.FileCommitsService
import io.github.hayatoyagi.prvisualizer.github.session.FileCommitsServiceImpl
import io.github.hayatoyagi.prvisualizer.github.session.GitHubSessionManager
import io.github.hayatoyagi.prvisualizer.navigation.NavigationManager
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.repository.store.SelectedRepositoryStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VisualizerViewModel(
    private val selectedRepositoryStore: SelectedRepositoryStore,
    private val fileCommitsService: FileCommitsService = FileCommitsServiceImpl(),
) : ViewModel() {
    val repoState: StateFlow<RepoState>
        get() = selectedRepositoryStore.repoState

    private var lastAppliedRepoState: RepoState = selectedRepositoryStore.repoState.value

    // Main state container
    var state by mutableStateOf(VisualizerState())
        private set

    private var fileDetailsJob: Job? = null

    // Delegate color management to ColorManager
    private val colorManager = ColorManager(
        colorState = state.colorState,
        onStateChanged = { newColorState ->
            state = state.copy(colorState = newColorState)
        },
    )

    // Delegate navigation management to NavigationManager
    private val navigationManager = NavigationManager(
        navigationState = state.navigationState,
        onStateChanged = { newNavigationState ->
            state = state.copy(navigationState = newNavigationState)
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
            resetNavigation()
            resetViewport()
        },
        selectRepo = ::selectRepo,
        unselectRepo = { selectedRepositoryStore.unselect() },
    )

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
                // Sync managers with the reset state
                colorManager.updateState(state.colorState)
                navigationManager.updateState(state.navigationState)
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
        val snapshotFetchState = requireNotNull(state.snapshotFetchState as? SnapshotFetchState.Ready) {
            "File details requires a ready snapshot."
        }
        state = state.copy(
            dialogState = DialogState.FileDetails(
                filePath = filePath,
                defaultBranch = snapshotFetchState.snapshot.defaultBranch,
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
        state = state.copy(
            filterState = state.filterState.copy(showDrafts = value),
        )
    }

    fun updateOnlyMine(value: Boolean) {
        state = state.copy(
            filterState = state.filterState.copy(onlyMine = value),
        )
    }

    fun togglePr(
        prId: String,
        checked: Boolean,
    ) {
        val newSelectedPrIds = if (checked) {
            state.filterState.selectedPrIds + prId
        } else {
            state.filterState.selectedPrIds - prId
        }
        state = state.copy(
            filterState = state.filterState.copy(selectedPrIds = newSelectedPrIds),
        )
    }

    fun selectAllPrs(available: Set<String>) {
        state = state.copy(
            filterState = state.filterState.copy(selectedPrIds = available),
        )
    }

    fun addRelatedPrs(related: Set<String>) {
        if (related.isNotEmpty()) {
            state = state.copy(
                filterState = state.filterState.copy(
                    selectedPrIds = state.filterState.selectedPrIds + related,
                ),
            )
        }
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

    fun shufflePrColors(prs: List<PullRequest>) = colorManager.shufflePrColors(prs)

    fun cyclePrColor(prId: String) = colorManager.cyclePrColor(prId)
}
