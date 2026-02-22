package io.github.hayatoyagi.prvisualizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hayatoyagi.prvisualizer.github.EnvConfig
import io.github.hayatoyagi.prvisualizer.github.GitHubApi
import io.github.hayatoyagi.prvisualizer.github.GitHubApiException
import io.github.hayatoyagi.prvisualizer.github.GitHubAuthExpiredException
import io.github.hayatoyagi.prvisualizer.github.GitHubOAuthDesktopAuthenticator
import io.github.hayatoyagi.prvisualizer.github.session.GitHubTokenStore
import io.github.hayatoyagi.prvisualizer.ui.shared.parentPathOf
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class VisualizerViewModel(
    initialOwner: String = EnvConfig.get("GITHUB_OWNER") ?: "HayatoYagi",
    initialRepo: String = EnvConfig.get("GITHUB_REPO") ?: "GitHub_PRs_Visualizer",
    initialToken: String = EnvConfig.get("GITHUB_TOKEN") ?: "",
    initialUser: String = EnvConfig.get("GITHUB_USER") ?: "hayatoy",
) : ViewModel() {
    // Main state container
    var state by mutableStateOf(
        VisualizerState(
            repoState = RepoState(owner = initialOwner, repo = initialRepo),
            sessionState = SessionState(oauthToken = initialToken, currentUserOverride = initialUser),
        ),
    )
        private set

    // Navigation history for back/forward buttons
    private val navigationHistory = NavigationHistory()

    // region: セッション管理
    private val authenticator = GitHubOAuthDesktopAuthenticator()
    private var repositoriesLoaded = false
    private var restoreAttempted = false

    fun initializeSession() {
        viewModelScope.launch { restoreTokenAndConnectIfNeeded() }
    }

    fun loginAndConnect(clientId: String) {
        viewModelScope.launch { loginAndConnectInternal(clientId) }
    }

    fun refresh() {
        viewModelScope.launch { connect() }
    }

    fun ensureRepositoryOptions() {
        viewModelScope.launch { ensureRepositoryOptionsInternal() }
    }

    fun loadRepositoryOptions() {
        viewModelScope.launch { loadRepositoryOptionsInternal() }
    }

    private suspend fun restoreTokenAndConnectIfNeeded() {
        if (restoreAttempted) return
        restoreAttempted = true
        val restoredToken = withContext(Dispatchers.IO) {
            GitHubTokenStore.loadToken(state.sessionState.oauthToken)
        }
        if (restoredToken.isBlank()) return
        updateSession { it.copy(oauthToken = restoredToken) }
        if (state.sessionState.githubSnapshot == null) {
            connect()
        }
    }

    private suspend fun loginAndConnectInternal(clientId: String) {
        updateSession {
            it.copy(
                isAuthorizing = true,
                connectionError = null,
                deviceUserCode = null,
                deviceVerificationUrl = null,
            )
        }
        runCatching {
            authenticator.authenticate(
                clientId = clientId.trim(),
                onDeviceFlowStart = { prompt ->
                    viewModelScope.launch {
                        updateSession {
                            it.copy(
                                deviceUserCode = prompt.userCode,
                                deviceVerificationUrl = prompt.verificationUriComplete ?: prompt.verificationUri,
                            )
                        }
                    }
                },
            )
        }.onSuccess { token ->
            withContext(Dispatchers.IO) { GitHubTokenStore.saveToken(token) }
            updateSession { it.copy(oauthToken = token, deviceUserCode = null, deviceVerificationUrl = null) }
            connect()
        }.onFailure { error ->
            updateSession { it.copy(connectionError = AppError.OAuthFailed(error.message ?: "OAuth failed")) }
        }
        updateSession { it.copy(isAuthorizing = false) }
    }

    private suspend fun connect() {
        if (state.sessionState.oauthToken.isBlank()) return
        updateSession { it.copy(isConnecting = true, connectionError = null) }
        runCatching {
            GitHubApi(state.sessionState.oauthToken.trim()).fetchSnapshot(
                owner = state.repoState.owner.trim(),
                repo = state.repoState.repo.trim(),
            )
        }.onSuccess { snapshot ->
            updateSession { session ->
                session.copy(
                    githubSnapshot = snapshot,
                    currentUserOverride = snapshot.viewerLogin?.takeIf { it.isNotBlank() } ?: session.currentUserOverride,
                )
            }
            resetNavigation()
            resetViewport()
        }.onFailure { error ->
            if (handleAuthExpired(error)) {
                updateSession { it.copy(isConnecting = false) }
                return
            }
            updateSession {
                it.copy(
                    connectionError = when (error) {
                        is java.net.ConnectException, is java.net.UnknownHostException ->
                            AppError.Network(error.message ?: "Network error")
                        is GitHubApiException ->
                            AppError.ApiError(error.statusCode, error.message ?: "API error")
                        else -> AppError.Unknown(error.message ?: "Unknown error")
                    },
                )
            }
        }
        updateSession { it.copy(isConnecting = false) }
    }

    private suspend fun ensureRepositoryOptionsInternal() {
        if (state.sessionState.oauthToken.isBlank()) return
        if (state.sessionState.isLoadingRepositories) return
        if (repositoriesLoaded && state.sessionState.repositoryOptions.isNotEmpty()) return
        loadRepositoryOptionsInternal()
    }

    private suspend fun loadRepositoryOptionsInternal() {
        if (state.sessionState.oauthToken.isBlank()) return
        updateSession { it.copy(isLoadingRepositories = true) }
        runCatching {
            GitHubApi(state.sessionState.oauthToken.trim()).fetchAccessibleRepositoryNames()
        }.onSuccess { repos ->
            repositoriesLoaded = true
            updateSession { it.copy(repositoryOptions = repos) }
            if (state.repoState.repo.isBlank() && repos.isNotEmpty()) {
                selectRepo(repos.first())
            }
        }.onFailure { error ->
            if (handleAuthExpired(error)) return@onFailure
            updateSession { it.copy(connectionError = AppError.Network(error.message ?: "Failed to load repositories")) }
        }
        updateSession { it.copy(isLoadingRepositories = false) }
    }

    private fun handleAuthExpired(error: Throwable): Boolean {
        if (error !is GitHubAuthExpiredException) return false
        viewModelScope.launch(Dispatchers.IO) { GitHubTokenStore.clearToken() }
        repositoriesLoaded = false
        updateSession {
            it.copy(
                oauthToken = "",
                githubSnapshot = null,
                repositoryOptions = emptyList(),
                deviceUserCode = null,
                deviceVerificationUrl = null,
                connectionError = AppError.AuthExpired(),
            )
        }
        return true
    }

    private fun updateSession(update: (SessionState) -> SessionState) {
        state = state.copy(sessionState = update(state.sessionState))
    }

    // region: ダイアログ管理
    fun openRepoDialog() {
        state = state.copy(
            dialogState = state.dialogState.copy(
                repoPickerQuery = "${state.repoState.owner}/${state.repoState.repo}".trim().trim('/'),
                isRepoDialogOpen = true,
            ),
        )
    }

    fun closeRepoDialog() {
        state = state.copy(
            dialogState = state.dialogState.copy(isRepoDialogOpen = false),
        )
    }

    fun updateRepoPickerQuery(q: String) {
        state = state.copy(
            dialogState = state.dialogState.copy(repoPickerQuery = q),
        )
    }

    // region: リポジトリ選択
    fun selectRepo(fullName: String) {
        val newOwner = fullName.substringBefore('/', state.repoState.owner)
        val newRepo = fullName.substringAfter('/', fullName)
        state = state.resetForNewRepo(owner = newOwner, repo = newRepo)
        navigationHistory.clear()
        navigationHistory.recordFocusPath(state.navigationState.focusPath)
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

    fun updateQuery(value: String) {
        state = state.copy(
            filterState = state.filterState.copy(query = value),
        )
    }

    fun clearQuery() {
        state = state.copy(
            filterState = state.filterState.copy(query = ""),
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
    fun selectDirectory(path: String) {
        navigationHistory.recordFocusPath(path)
        state = state.copy(
            navigationState = state.navigationState.copy(
                focusPath = path,
                viewportResetToken = state.navigationState.viewportResetToken + 1,
            ),
        )
    }

    fun selectFile(path: String) {
        val parentPath = parentPathOf(path)
        navigationHistory.recordFocusPath(parentPath)
        state = state.copy(
            navigationState = state.navigationState.copy(
                selectedPath = path,
                focusPath = parentPath,
                viewportResetToken = state.navigationState.viewportResetToken + 1,
            ),
        )
    }

    fun changeFocusPath(path: String) {
        navigationHistory.recordFocusPath(path)
        state = state.copy(
            navigationState = state.navigationState.copy(
                focusPath = path,
                viewportResetToken = state.navigationState.viewportResetToken + 1,
            ),
        )
    }

    fun updateSelectedPath(path: String?) {
        state = state.copy(
            navigationState = state.navigationState.copy(selectedPath = path),
        )
    }

    fun resetNavigation() {
        state = state.copy(
            navigationState = state.navigationState.resetNavigation(),
        )
        navigationHistory.clear()
        navigationHistory.recordFocusPath(state.navigationState.focusPath)
    }

    fun resetViewport() {
        state = state.copy(
            navigationState = state.navigationState.resetViewport(),
        )
    }

    /**
     * Navigates back in history. Returns true if navigation occurred.
     */
    fun navigateBack(): Boolean {
        val previousPath = navigationHistory.navigateBack()
        return if (previousPath != null) {
            state = state.copy(
                navigationState = state.navigationState.copy(
                    focusPath = previousPath,
                    viewportResetToken = state.navigationState.viewportResetToken + 1,
                ),
            )
            true
        } else {
            false
        }
    }

    /**
     * Navigates forward in history. Returns true if navigation occurred.
     */
    fun navigateForward(): Boolean {
        val nextPath = navigationHistory.navigateForward()
        return if (nextPath != null) {
            state = state.copy(
                navigationState = state.navigationState.copy(
                    focusPath = nextPath,
                    viewportResetToken = state.navigationState.viewportResetToken + 1,
                ),
            )
            true
        } else {
            false
        }
    }

    /**
     * Returns true if back navigation is possible.
     */
    fun canNavigateBack(): Boolean = navigationHistory.canNavigateBack()

    /**
     * Returns true if forward navigation is possible.
     */
    fun canNavigateForward(): Boolean = navigationHistory.canNavigateForward()

    // region: 色管理
    fun ensurePrColors(prs: List<PullRequest>) {
        val prsNeedingColors = prs.filter { !state.colorState.prColorMap.containsKey(it.id) }
        if (prsNeedingColors.isNotEmpty()) {
            val newMap = state.colorState.prColorMap.toMutableMap()
            prsNeedingColors.forEach { pr ->
                newMap[pr.id] = randomColorAvoidingMap(newMap)
            }
            state = state.copy(
                colorState = state.colorState.copy(prColorMap = newMap),
            )
        }
    }

    fun shufflePrColors(prs: List<PullRequest>) {
        val newMap = mutableMapOf<String, Color>()
        prs.forEach { pr ->
            newMap[pr.id] = randomColorAvoidingMap(newMap)
        }
        state = state.copy(
            colorState = state.colorState.copy(prColorMap = newMap),
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
                prColorMap = state.colorState.prColorMap + (prId to AppColors.authorPalette[nextIndex]),
            ),
        )
    }

    private fun randomColorAvoidingMap(assignedMap: Map<String, Color>): Color {
        // Avoid the 5 most recently assigned colors (map preserves insertion order)
        val recentColors = assignedMap.values
            .toList()
            .takeLast(5)
            .toSet()
        val availableColors = AppColors.authorPalette.filter { it !in recentColors }
        return if (availableColors.isNotEmpty()) {
            availableColors[Random.nextInt(availableColors.size)]
        } else {
            AppColors.authorPalette[Random.nextInt(AppColors.authorPalette.size)]
        }
    }
}
