package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.AuthState
import io.github.hayatoyagi.prvisualizer.RepoSelectionState
import io.github.hayatoyagi.prvisualizer.SnapshotFetchState
import io.github.hayatoyagi.prvisualizer.github.GitHubAuthExpiredException
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Orchestrates authentication, repository resolution, and snapshot fetching.
 * Concrete side effects and API operations are delegated to dedicated services.
 */
class GitHubSessionManager(
    private val scope: CoroutineScope,
    private val getAuthState: () -> AuthState,
    private val setAuthState: (AuthState) -> Unit,
    private val getSnapshotFetchState: () -> SnapshotFetchState,
    private val setSnapshotFetchState: (SnapshotFetchState) -> Unit,
    private val getRepoState: () -> RepoState,
    private val getRepoSelectionState: () -> RepoSelectionState,
    private val setRepoSelectionState: (RepoSelectionState) -> Unit,
    private val onSnapshotLoaded: () -> Unit,
    private val selectRepo: (String) -> Unit,
    private val authService: AuthService = AuthServiceImpl(),
    private val repoSelectionService: RepoSelectionService = RepoSelectionServiceImpl(),
    private val snapshotFetchService: SnapshotFetchService = SnapshotFetchServiceImpl(),
) {
    private var restoreAttempted = false

    /**
     * Initializes the session by restoring saved token and connecting if available.
     */
    fun initializeSession() {
        scope.launch { restoreTokenAndConnectIfNeeded() }
    }

    /**
     * Initiates login flow and establishes connection.
     *
     * @param clientId The GitHub OAuth client ID
     */
    fun loginAndConnect(clientId: String) {
        scope.launch { loginAndConnectInternal(clientId) }
    }

    /**
     * Refreshes the connection by fetching latest data.
     */
    fun refresh() {
        scope.launch { connectWithResolvedRepository() }
    }

    /**
     * Ensures repository options are loaded if needed.
     */
    fun ensureRepositoryOptions() {
        scope.launch {
            val token = authenticatedTokenOrBlank()
            val selectionState = getRepoSelectionState()
            if (token.isBlank()) return@launch
            if (selectionState is RepoSelectionState.Loading) return@launch
            val hasLoadedOnce = hasLoadedRepositoryOptions(selectionState)
            val options = selectionOptions(selectionState)
            if (hasLoadedOnce && options.isNotEmpty()) return@launch
            loadRepositoryOptionsInternal()
        }
    }

    /**
     * Loads or reloads the list of available repositories.
     */
    fun loadRepositoryOptions() {
        scope.launch { loadRepositoryOptionsInternal() }
    }

    private suspend fun restoreTokenAndConnectIfNeeded() {
        if (restoreAttempted) return
        restoreAttempted = true

        val fallbackToken = authenticatedTokenOrBlank()
        val restoredToken = authService.restoreToken(fallbackToken)
        if (restoredToken.isBlank()) return

        setAuthState(AuthState.Authenticated(restoredToken))
        if (getSnapshotFetchState() !is SnapshotFetchState.Ready) {
            connectWithResolvedRepository()
        }
    }

    private suspend fun loginAndConnectInternal(clientId: String) {
        setAuthState(AuthState.Authorizing())

        authService.login(
            clientId = clientId.trim(),
            onDeviceFlowStart = { prompt ->
                setAuthState(
                    AuthState.Authorizing(
                        deviceUserCode = prompt.userCode,
                        deviceVerificationUrl = prompt.verificationUriComplete ?: prompt.verificationUri,
                    ),
                )
            },
        ).onSuccess { token ->
            setAuthState(AuthState.Authenticated(token))
            connectWithResolvedRepository()
        }.onFailure { error ->
            setAuthState(AuthState.Failed(AppError.OAuthFailed(error.message ?: "OAuth failed")))
        }
    }

    private suspend fun connectWithResolvedRepository() {
        if (!resolveRepositoryIfNeeded()) return
        connect()
    }

    private suspend fun resolveRepositoryIfNeeded(): Boolean {
        if (getRepoState() is RepoState.Selected) return true
        loadRepositoryOptionsInternal()
        return getRepoState() is RepoState.Selected
    }

    private suspend fun connect() {
        val token = authenticatedTokenOrBlank()
        val selectedRepo = getRepoState() as? RepoState.Selected ?: return
        if (token.isBlank()) return

        setSnapshotFetchState(SnapshotFetchState.Fetching)

        snapshotFetchService.fetchSnapshot(
            token = token,
            owner = selectedRepo.owner,
            repo = selectedRepo.repo,
        ).onSuccess { snapshot ->
            setSnapshotFetchState(SnapshotFetchState.Ready(snapshot))
            onSnapshotLoaded()
        }.onFailure { error ->
            if (handleAuthExpired(error)) return
            setSnapshotFetchState(SnapshotFetchState.Failed(AppError.from(error)))
        }
    }

    private suspend fun loadRepositoryOptionsInternal() {
        val token = authenticatedTokenOrBlank()
        if (token.isBlank()) return

        val previousOptions = selectionOptions(getRepoSelectionState())
        setRepoSelectionState(RepoSelectionState.Loading)

        repoSelectionService.fetchRepositoryOptions(token)
            .onSuccess { repos ->
                setRepoSelectionState(RepoSelectionState.Ready(repos))
                if (getRepoState() is RepoState.Unselected && repos.isNotEmpty()) {
                    selectRepo(repos.first())
                }
            }
            .onFailure { error ->
                if (handleAuthExpired(error)) return@onFailure
                setRepoSelectionState(
                    RepoSelectionState.Error(
                        options = previousOptions,
                        error = AppError.Network(error.message ?: "Failed to load repositories"),
                    ),
                )
            }
    }

    private suspend fun handleAuthExpired(error: Throwable): Boolean {
        if (error !is GitHubAuthExpiredException) return false

        authService.clearToken()
        setAuthState(AuthState.Failed(AppError.AuthExpired()))
        setSnapshotFetchState(SnapshotFetchState.Idle)
        setRepoSelectionState(RepoSelectionState.Idle)
        return true
    }

    private fun authenticatedTokenOrBlank(): String = when (val authState = getAuthState()) {
        is AuthState.Authenticated -> authState.oauthToken
        AuthState.Unauthenticated, is AuthState.Authorizing, is AuthState.Failed -> ""
    }

    private fun hasLoadedRepositoryOptions(selectionState: RepoSelectionState): Boolean =
        selectionState is RepoSelectionState.Ready || selectionState is RepoSelectionState.Error

    private fun selectionOptions(selectionState: RepoSelectionState): List<String> = when (selectionState) {
        RepoSelectionState.Idle, RepoSelectionState.Loading -> emptyList()
        is RepoSelectionState.Ready -> selectionState.options
        is RepoSelectionState.Error -> selectionState.options
    }
}
