package io.github.hayatoyagi.prvisualizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hayatoyagi.prvisualizer.color.PrColorAssigner
import io.github.hayatoyagi.prvisualizer.github.session.FileCommitsService
import io.github.hayatoyagi.prvisualizer.github.session.FileCommitsServiceImpl
import io.github.hayatoyagi.prvisualizer.github.session.GitHubSessionManager
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.repository.store.SelectedRepositoryStore
import io.github.hayatoyagi.prvisualizer.ui.shared.parentPathOf
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
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

    // Navigation history for back/forward buttons
    private val navigationHistory = NavigationHistory()

    private var fileDetailsJob: Job? = null

    // region: セッション管理
    private val sessionManager = GitHubSessionManager(
        scope = viewModelScope,
        getAuthState = { state.authState },
        setAuthState = { state = state.copy(authState = it) },
        getSnapshotFetchState = { state.snapshotFetchState },
        setSnapshotFetchState = { state = state.copy(snapshotFetchState = it) },
        getRepoState = { selectedRepositoryStore.repoState.value },
        getRepoSelectionState = { state.repoSelectionState },
        setRepoSelectionState = { state = state.copy(repoSelectionState = it) },
        onSnapshotLoaded = {
            resetNavigation()
            resetViewport()
        },
        selectRepo = ::selectRepo,
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
            }
            RepoState.Unselected -> {
                state = state.copy(
                    snapshotFetchState = SnapshotFetchState.Idle,
                )
            }
        }
        navigationHistory.clear()
        navigationHistory.recordFocusPath(state.navigationState.focusPath)
    }

    /**
     * Initializes the session by restoring saved token.
     */
    fun initializeSession() = sessionManager.initializeSession()

    /**
     * Initiates GitHub login flow.
     *
     * @param clientId The GitHub OAuth client ID
     */
    fun loginAndConnect(clientId: String) = sessionManager.loginAndConnect(clientId)

    /**
     * Refreshes the connection and fetches latest data.
     */
    fun refresh() = sessionManager.refresh()

    /**
     * Ensures repository options are loaded.
     */
    fun ensureRepositoryOptions() = sessionManager.ensureRepositoryOptions()

    /**
     * Loads or reloads available repositories.
     */
    fun loadRepositoryOptions() = sessionManager.loadRepositoryOptions()

    /**
     * Opens the repository picker dialog.
     */
    fun openRepoDialog() {
        state = state.copy(
            dialogState = DialogState.RepoPicker,
        )
    }

    /**
     * Closes the repository picker dialog.
     */
    fun closeRepoDialog() {
        state = state.copy(
            dialogState = DialogState.None,
        )
    }

    /**
     * Opens the file details dialog for a specific file.
     *
     * @param filePath The path of the file to show details for
     */
    fun openFileDetailsDialog(filePath: String) {
        fileDetailsJob?.cancel()
        state = state.copy(
            dialogState = DialogState.FileDetails(
                filePath = filePath,
                commitsState = DialogState.FileDetails.CommitsState.Loading,
            ),
        )
        fileDetailsJob = loadFileDetailsCommits(filePath)
    }

    /**
     * Opens the PR details dialog.
     *
     * @param pr The pull request to show details for
     */
    fun openPrDetailsDialog(pr: PullRequest) {
        state = state.copy(
            dialogState = DialogState.PrDetails(pr = pr),
        )
    }

    /**
     * Closes the currently open dialog.
     */
    fun closeDialog() {
        fileDetailsJob?.cancel()
        state = state.copy(
            dialogState = DialogState.None,
        )
    }

    /**
     * Reloads commits for the currently open file details dialog.
     */
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

    /**
     * Selects a repository.
     *
     * @param fullName The full repository name in "owner/repo" format
     */
    fun selectRepo(fullName: String) {
        val currentOwner = (selectedRepositoryStore.repoState.value as? RepoState.Selected)?.owner.orEmpty()
        val newOwner = fullName.substringBefore('/', currentOwner)
        val newRepo = fullName.substringAfter('/', fullName)
        selectedRepositoryStore.select(owner = newOwner, repo = newRepo)
        applyRepositoryState(selectedRepositoryStore.repoState.value)
    }

    /**
     * Updates whether draft PRs are shown.
     *
     * @param value Whether to show draft PRs
     */
    fun updateShowDrafts(value: Boolean) {
        state = state.copy(
            filterState = state.filterState.copy(showDrafts = value),
        )
    }

    /**
     * Updates whether only the user's PRs are shown.
     *
     * @param value Whether to show only the user's PRs
     */
    fun updateOnlyMine(value: Boolean) {
        state = state.copy(
            filterState = state.filterState.copy(onlyMine = value),
        )
    }

    /**
     * Toggles a PR's selection state.
     *
     * @param prId The PR ID to toggle
     * @param checked Whether the PR should be checked
     */
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

    /**
     * Selects all available PRs.
     *
     * @param available Set of available PR IDs
     */
    fun selectAllPrs(available: Set<String>) {
        state = state.copy(
            filterState = state.filterState.copy(selectedPrIds = available),
        )
    }

    /**
     * Adds related PRs to the selection.
     *
     * @param related Set of PR IDs to add
     */
    fun addRelatedPrs(related: Set<String>) {
        if (related.isNotEmpty()) {
            state = state.copy(
                filterState = state.filterState.copy(
                    selectedPrIds = state.filterState.selectedPrIds + related,
                ),
            )
        }
    }

    /**
     * Selects a directory in the treemap view.
     *
     * @param path The directory path to select
     */
    fun selectDirectory(path: String) {
        navigationHistory.recordFocusPath(path)
        // Auto-expand the selected directory and its ancestor chain.
        val explorerState = state.navigationState.explorerState
        val expandedPaths = expandPathAndAncestors(explorerState.expandedPaths, path)
        state = state.copy(
            navigationState = state.navigationState.copy(
                focusPath = path,
                viewportResetToken = state.navigationState.viewportResetToken + 1,
                explorerState = explorerState.copy(expandedPaths = expandedPaths),
            ),
        )
    }

    /**
     * Selects a file in the treemap view.
     *
     * @param path The file path to select
     */
    fun selectFile(path: String) {
        val parentPath = parentPathOf(path)
        navigationHistory.recordFocusPath(parentPath)
        // Auto-expand parent directories so the file is visible
        val explorerState = state.navigationState.explorerState
        val expandedPaths = expandPathAndAncestors(explorerState.expandedPaths, parentPath)
        state = state.copy(
            navigationState = state.navigationState.copy(
                selectedPath = path,
                focusPath = parentPath,
                viewportResetToken = state.navigationState.viewportResetToken + 1,
                explorerState = explorerState.copy(expandedPaths = expandedPaths),
            ),
        )
    }

    /**
     * Changes the focus path in the treemap view.
     *
     * @param path The new focus path
     */
    fun changeFocusPath(path: String) {
        navigationHistory.recordFocusPath(path)
        // Auto-expand the focused directory and its ancestor chain.
        val explorerState = state.navigationState.explorerState
        val expandedPaths = expandPathAndAncestors(explorerState.expandedPaths, path)
        state = state.copy(
            navigationState = state.navigationState.copy(
                focusPath = path,
                viewportResetToken = state.navigationState.viewportResetToken + 1,
                explorerState = explorerState.copy(expandedPaths = expandedPaths),
            ),
        )
    }

    /**
     * Updates the selected path.
     *
     * @param path The new selected path, or null to clear
     */
    fun updateSelectedPath(path: String?) {
        state = state.copy(
            navigationState = state.navigationState.copy(selectedPath = path),
        )
    }

    /**
     * Resets navigation to the root.
     */
    fun resetNavigation() {
        state = state.copy(
            navigationState = state.navigationState.resetNavigation(),
        )
        navigationHistory.clear()
        navigationHistory.recordFocusPath(state.navigationState.focusPath)
    }

    /**
     * Resets the treemap viewport to default zoom and pan.
     */
    fun resetViewport() {
        state = state.copy(
            navigationState = state.navigationState.resetViewport(),
        )
    }

    /**
     * Toggles the expansion state of a directory in the explorer.
     *
     * @param path The directory path to toggle
     */
    fun toggleDirectoryExpanded(path: String) {
        val explorerState = state.navigationState.explorerState
        val expandedPaths = explorerState.expandedPaths
        val newExpandedPaths = if (expandedPaths.contains(path)) {
            expandedPaths - path
        } else {
            expandedPaths + path
        }
        state = state.copy(
            navigationState = state.navigationState.copy(
                explorerState = explorerState.copy(expandedPaths = newExpandedPaths),
            ),
        )
    }

    /**
     * Navigates back in history. Returns true if navigation occurred.
     */
    fun navigateBack(): Boolean {
        val previousPath = navigationHistory.navigateBack()
        return if (previousPath != null) {
            val explorerState = state.navigationState.explorerState
            val expandedPaths = expandPathAndAncestors(explorerState.expandedPaths, previousPath)
            state = state.copy(
                navigationState = state.navigationState.copy(
                    focusPath = previousPath,
                    viewportResetToken = state.navigationState.viewportResetToken + 1,
                    explorerState = explorerState.copy(expandedPaths = expandedPaths),
                ),
            )
            true
        } else {
            false
        }
    }

    private fun expandPathAndAncestors(
        expandedPaths: Set<String>,
        path: String,
    ): Set<String> {
        if (path.isBlank()) return expandedPaths

        var currentPath = ""
        var newExpandedPaths = expandedPaths
        for (segment in path.split('/')) {
            if (segment.isBlank()) continue
            currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"
            if (!newExpandedPaths.contains(currentPath)) {
                newExpandedPaths = newExpandedPaths + currentPath
            }
        }
        return newExpandedPaths
    }

    /**
     * Navigates forward in history. Returns true if navigation occurred.
     */
    fun navigateForward(): Boolean {
        val nextPath = navigationHistory.navigateForward()
        return if (nextPath != null) {
            val explorerState = state.navigationState.explorerState
            val expandedPaths = expandPathAndAncestors(explorerState.expandedPaths, nextPath)
            state = state.copy(
                navigationState = state.navigationState.copy(
                    focusPath = nextPath,
                    viewportResetToken = state.navigationState.viewportResetToken + 1,
                    explorerState = explorerState.copy(expandedPaths = expandedPaths),
                ),
            )
            true
        } else {
            false
        }
    }

    /**
     * Ensures all PRs have assigned colors.
     *
     * @param prs List of pull requests to assign colors to
     */
    fun ensurePrColors(prs: List<PullRequest>) {
        val prsNeedingColors = prs.filter { !state.colorState.prColorMap.containsKey(it.id) }
        if (prsNeedingColors.isNotEmpty()) {
            val newMap = LinkedHashMap(state.colorState.prColorMap)
            prsNeedingColors.forEach { pr ->
                newMap[pr.id] = PrColorAssigner.nextColor(newMap)
            }
            state = state.copy(
                colorState = state.colorState.copy(prColorMap = newMap),
            )
        }
    }

    /**
     * Shuffles all PR colors randomly.
     *
     * @param prs List of pull requests to shuffle colors for
     */
    fun shufflePrColors(prs: List<PullRequest>) {
        val newMap = LinkedHashMap<String, Color>()
        prs.forEach { pr ->
            newMap[pr.id] = PrColorAssigner.nextColor(newMap)
        }
        state = state.copy(
            colorState = state.colorState.copy(prColorMap = newMap),
        )
    }

    /**
     * Cycles a PR's color to the next in the palette.
     *
     * @param prId The PR ID to cycle the color for
     */
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
                prColorMap = state.colorState.prColorMap + (prId to AppColors.authorPalette[nextIndex]),
            ),
        )
    }
}
