package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.RepoState
import io.github.hayatoyagi.prvisualizer.SessionState
import io.github.hayatoyagi.prvisualizer.github.GitHubApi
import io.github.hayatoyagi.prvisualizer.github.GitHubApiException
import io.github.hayatoyagi.prvisualizer.github.GitHubAuthExpiredException
import io.github.hayatoyagi.prvisualizer.github.GitHubOAuthDesktopAuthenticator
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
        scope.launch { connect() }
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
            GitHubTokenStore.loadToken(getSessionState().oauthToken)
        }
        if (restoredToken.isBlank()) return
        updateSession { it.copy(oauthToken = restoredToken) }
        if (getSessionState().githubSnapshot == null) {
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
                    scope.launch {
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
        val token = getSessionState().oauthToken
        if (token.isBlank()) return
        updateSession { it.copy(isConnecting = true, connectionError = null) }
        runCatching {
            apiFactory(token.trim()).fetchSnapshot(
                owner = getRepoState().owner.trim(),
                repo = getRepoState().repo.trim(),
            )
        }.onSuccess { snapshot ->
            updateSession { session ->
                session.copy(
                    githubSnapshot = snapshot,
                    currentUserOverride = snapshot.viewerLogin?.takeIf { it.isNotBlank() } ?: session.currentUserOverride,
                )
            }
            onSnapshotLoaded()
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
        val session = getSessionState()
        if (session.oauthToken.isBlank()) return
        if (session.isLoadingRepositories) return
        if (repositoriesLoaded && session.repositoryOptions.isNotEmpty()) return
        loadRepositoryOptionsInternal()
    }

    private suspend fun loadRepositoryOptionsInternal() {
        val token = getSessionState().oauthToken
        if (token.isBlank()) return
        updateSession { it.copy(isLoadingRepositories = true) }
        runCatching {
            apiFactory(token.trim()).fetchAccessibleRepositoryNames()
        }.onSuccess { repos ->
            repositoriesLoaded = true
            updateSession { it.copy(repositoryOptions = repos) }
            if (getRepoState().repo.isBlank() && repos.isNotEmpty()) {
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
        scope.launch(Dispatchers.IO) { GitHubTokenStore.clearToken() }
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
        setSessionState(update(getSessionState()))
    }
}
