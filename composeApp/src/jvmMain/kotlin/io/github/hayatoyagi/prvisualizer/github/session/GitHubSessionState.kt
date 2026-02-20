package io.github.hayatoyagi.prvisualizer.github.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.github.GitHubApi
import io.github.hayatoyagi.prvisualizer.github.GitHubApiException
import io.github.hayatoyagi.prvisualizer.github.GitHubAuthExpiredException
import io.github.hayatoyagi.prvisualizer.github.GitHubOAuthDesktopAuthenticator
import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GitHubSessionState(
    private val uiScope: CoroutineScope,
    private val authenticator: GitHubOAuthDesktopAuthenticator,
    private val apiFactory: (String) -> GitHubApi,
    initialToken: String,
    initialUser: String,
) {
    var githubSnapshot by mutableStateOf<GitHubSnapshot?>(null)
    var connectionError by mutableStateOf<AppError?>(null)
    var isConnecting by mutableStateOf(false)
    var isAuthorizing by mutableStateOf(false)
    var oauthToken by mutableStateOf(initialToken)
    var currentUserOverride by mutableStateOf(initialUser)
    var deviceUserCode by mutableStateOf<String?>(null)
    var deviceVerificationUrl by mutableStateOf<String?>(null)
    var repositoryOptions by mutableStateOf<List<String>>(emptyList())
    var isLoadingRepositories by mutableStateOf(false)
    private var repositoriesLoaded: Boolean = false
    private var restoreAttempted: Boolean = false

    val currentUser: String
        get() = githubSnapshot?.viewerLogin ?: currentUserOverride

    suspend fun restoreTokenAndConnectIfNeeded(owner: String, repo: String) {
        if (restoreAttempted) return
        restoreAttempted = true

        val restoredToken = withContext(Dispatchers.IO) {
            GitHubTokenStore.loadToken(oauthToken)
        }
        if (restoredToken.isBlank()) return

        oauthToken = restoredToken
        if (githubSnapshot == null) {
            connect(owner = owner, repo = repo)
        }
    }

    suspend fun loginAndConnect(clientId: String, owner: String, repo: String) {
        isAuthorizing = true
        connectionError = null
        deviceUserCode = null
        deviceVerificationUrl = null
        runCatching {
            authenticator.authenticate(
                clientId = clientId.trim(),
                onDeviceFlowStart = { prompt ->
                    uiScope.launch {
                        deviceUserCode = prompt.userCode
                        deviceVerificationUrl = prompt.verificationUriComplete ?: prompt.verificationUri
                    }
                },
            )
        }.onSuccess { token ->
            oauthToken = token
            withContext(Dispatchers.IO) {
                GitHubTokenStore.saveToken(token)
            }
            deviceUserCode = null
            deviceVerificationUrl = null
            connect(owner = owner, repo = repo)
        }.onFailure { error ->
            connectionError = AppError.OAuthFailed(error.message ?: "OAuth failed")
        }
        isAuthorizing = false
    }

    suspend fun refresh(owner: String, repo: String) {
        connect(owner = owner, repo = repo)
    }

    suspend fun ensureRepositoryOptions() {
        if (oauthToken.isBlank()) return
        if (isLoadingRepositories) return
        if (repositoriesLoaded && repositoryOptions.isNotEmpty()) return
        loadRepositoryOptions()
    }

    suspend fun loadRepositoryOptions() {
        if (oauthToken.isBlank()) return
        isLoadingRepositories = true
        runCatching {
            apiFactory(oauthToken.trim()).fetchAccessibleRepositoryNames()
        }.onSuccess { repos ->
            repositoryOptions = repos
            repositoriesLoaded = true
        }.onFailure { error ->
            if (handleAuthExpired(error)) return@onFailure
            connectionError = AppError.Network(error.message ?: "Failed to load repositories")
        }
        isLoadingRepositories = false
    }

    private suspend fun connect(owner: String, repo: String) {
        if (oauthToken.isBlank()) return
        isConnecting = true
        connectionError = null
        runCatching {
            apiFactory(oauthToken.trim()).fetchSnapshot(owner = owner.trim(), repo = repo.trim())
        }.onSuccess { snapshot ->
            githubSnapshot = snapshot
            if (!snapshot.viewerLogin.isNullOrBlank()) {
                currentUserOverride = snapshot.viewerLogin
            }
        }.onFailure { error ->
            if (handleAuthExpired(error)) {
                isConnecting = false
                return
            }
            connectionError = when (error) {
                is java.net.ConnectException, is java.net.UnknownHostException ->
                    AppError.Network(error.message ?: "Network error")
                is GitHubApiException ->
                    AppError.ApiError(error.statusCode, error.message ?: "API error")
                else -> AppError.Unknown(error.message ?: "Unknown error")
            }
        }
        isConnecting = false
    }

    private fun handleAuthExpired(error: Throwable): Boolean {
        if (error !is GitHubAuthExpiredException) return false
        oauthToken = ""
        githubSnapshot = null
        repositoryOptions = emptyList()
        repositoriesLoaded = false
        deviceUserCode = null
        deviceVerificationUrl = null
        uiScope.launch(Dispatchers.IO) {
            GitHubTokenStore.clearToken()
        }
        connectionError = AppError.AuthExpired()
        return true
    }
}

@Composable
fun rememberGitHubSessionState(
    initialToken: String,
    initialUser: String,
): GitHubSessionState {
    val uiScope = rememberCoroutineScope()
    return remember {
        GitHubSessionState(
            uiScope = uiScope,
            authenticator = GitHubOAuthDesktopAuthenticator(),
            apiFactory = ::GitHubApi,
            initialToken = initialToken,
            initialUser = initialUser,
        )
    }
}
