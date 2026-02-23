package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.AuthState
import io.github.hayatoyagi.prvisualizer.RepoSelectionState
import io.github.hayatoyagi.prvisualizer.SessionState
import io.github.hayatoyagi.prvisualizer.SnapshotFetchState
import io.github.hayatoyagi.prvisualizer.github.GitHubApi
import io.github.hayatoyagi.prvisualizer.github.GitHubApiException
import io.github.hayatoyagi.prvisualizer.github.GitHubAuthExpiredException
import io.github.hayatoyagi.prvisualizer.github.GitHubOAuthDesktopAuthenticator
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Encapsulates all GitHub session logic: token restore, OAuth login, snapshot fetching,
 * and repository listing. Communicates state changes back to the caller via lambdas,
 * making the class independently testable without a ViewModel.
 */
class GitHubSessionManager(
    private val scope: CoroutineScope,
    private val getSessionState: () -> SessionState,
    private val setSessionState: (SessionState) -> Unit,
    private val getRepoState: () -> RepoState,
    private val getRepoSelectionState: () -> RepoSelectionState,
    private val setRepoSelectionState: (RepoSelectionState) -> Unit,
    private val onSnapshotLoaded: () -> Unit,
    private val selectRepo: (String) -> Unit,
    private val authenticator: GitHubOAuthDesktopAuthenticator = GitHubOAuthDesktopAuthenticator(),
    private val apiFactory: (String) -> GitHubApi = ::GitHubApi,
) {
    private var repositoriesLoaded = false
    private var restoreAttempted = false

    fun initializeSession() {
        scope.launch { restoreTokenAndConnectIfNeeded() }
    }

    fun loginAndConnect(clientId: String) {
        scope.launch { loginAndConnectInternal(clientId) }
    }

    fun refresh() {
        scope.launch { connectWithResolvedRepository() }
    }

    fun ensureRepositoryOptions() {
        scope.launch { ensureRepositoryOptionsInternal() }
    }

    fun loadRepositoryOptions() {
        scope.launch { loadRepositoryOptionsInternal() }
    }

    private suspend fun restoreTokenAndConnectIfNeeded() {
        if (restoreAttempted) return
        restoreAttempted = true
        val restoredToken = withContext(Dispatchers.IO) {
            GitHubTokenStore.loadToken(getSessionState().authState.oauthToken)
        }
        if (restoredToken.isBlank()) return
        updateAuthState { it.copy(oauthToken = restoredToken, error = null) }
        if (getSessionState().snapshotFetchState.snapshot == null) {
            connectWithResolvedRepository()
        }
    }

    private suspend fun loginAndConnectInternal(clientId: String) {
        updateAuthState {
            it.copy(
                isAuthorizing = true,
                error = null,
                deviceUserCode = null,
                deviceVerificationUrl = null,
            )
        }
        runCatching {
            authenticator.authenticate(
                clientId = clientId.trim(),
                onDeviceFlowStart = { prompt ->
                    scope.launch {
                        updateAuthState {
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
            updateAuthState {
                it.copy(
                    oauthToken = token,
                    error = null,
                    deviceUserCode = null,
                    deviceVerificationUrl = null,
                )
            }
            connectWithResolvedRepository()
        }.onFailure { error ->
            updateAuthState { it.copy(error = AppError.OAuthFailed(error.message ?: "OAuth failed")) }
        }
        updateAuthState { it.copy(isAuthorizing = false) }
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
        val token = getSessionState().authState.oauthToken
        val selectedRepo = getRepoState() as? RepoState.Selected ?: return
        if (token.isBlank()) return

        updateSnapshotFetchState { it.copy(isFetching = true, error = null) }
        runCatching {
            apiFactory(token.trim()).fetchSnapshot(
                owner = selectedRepo.owner.trim(),
                repo = selectedRepo.repo.trim(),
            )
        }.onSuccess { snapshot ->
            updateSessionState { session ->
                session.copy(
                    snapshotFetchState = session.snapshotFetchState.copy(
                        snapshot = snapshot,
                        error = null,
                    ),
                    authState = session.authState.copy(
                        currentUserOverride = snapshot.viewerLogin?.takeIf { it.isNotBlank() } ?: session.authState.currentUserOverride,
                    ),
                )
            }
            onSnapshotLoaded()
        }.onFailure { error ->
            if (handleAuthExpired(error)) return
            updateSnapshotFetchState { it.copy(error = toConnectionError(error)) }
        }
        updateSnapshotFetchState { it.copy(isFetching = false) }
    }

    private suspend fun ensureRepositoryOptionsInternal() {
        val session = getSessionState()
        val selection = getRepoSelectionState()
        if (session.authState.oauthToken.isBlank()) return
        if (selection.isLoading) return
        if (repositoriesLoaded && selection.options.isNotEmpty()) return
        loadRepositoryOptionsInternal()
    }

    private suspend fun loadRepositoryOptionsInternal() {
        val token = getSessionState().authState.oauthToken
        if (token.isBlank()) return
        setRepoSelectionState(getRepoSelectionState().copy(isLoading = true, error = null))
        runCatching {
            apiFactory(token.trim()).fetchAccessibleRepositoryNames()
        }.onSuccess { repos ->
            repositoriesLoaded = true
            setRepoSelectionState(
                getRepoSelectionState().copy(
                    options = repos,
                    isLoading = false,
                    error = null,
                ),
            )
            if (getRepoState() is RepoState.Unselected && repos.isNotEmpty()) {
                selectRepo(repos.first())
            }
        }.onFailure { error ->
            if (handleAuthExpired(error)) return@onFailure
            setRepoSelectionState(
                getRepoSelectionState().copy(
                    isLoading = false,
                    error = AppError.Network(error.message ?: "Failed to load repositories"),
                ),
            )
        }
    }

    private fun handleAuthExpired(error: Throwable): Boolean {
        if (error !is GitHubAuthExpiredException) return false
        scope.launch(Dispatchers.IO) { GitHubTokenStore.clearToken() }
        repositoriesLoaded = false
        updateSessionState {
            it.copy(
                authState = AuthState(
                    oauthToken = "",
                    error = AppError.AuthExpired(),
                ),
                snapshotFetchState = SnapshotFetchState(),
            )
        }
        setRepoSelectionState(RepoSelectionState())
        return true
    }

    private fun toConnectionError(error: Throwable): AppError = when (error) {
        is java.net.ConnectException, is java.net.UnknownHostException ->
            AppError.Network(error.message ?: "Network error")
        is GitHubApiException ->
            AppError.ApiError(error.statusCode, error.message ?: "API error")
        else -> AppError.Unknown(error.message ?: "Unknown error")
    }

    private fun updateAuthState(update: (AuthState) -> AuthState) {
        updateSessionState { it.copy(authState = update(it.authState)) }
    }

    private fun updateSnapshotFetchState(update: (SnapshotFetchState) -> SnapshotFetchState) {
        updateSessionState { it.copy(snapshotFetchState = update(it.snapshotFetchState)) }
    }

    private fun updateSessionState(update: (SessionState) -> SessionState) {
        setSessionState(update(getSessionState()))
    }
}
