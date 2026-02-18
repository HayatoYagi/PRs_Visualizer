package io.github.hayatoyagi.prvisualizer.github.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.hayatoyagi.prvisualizer.github.GitHubApi
import io.github.hayatoyagi.prvisualizer.github.GitHubOAuthDesktopAuthenticator
import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class GitHubSessionState(
    private val uiScope: CoroutineScope,
    private val authenticator: GitHubOAuthDesktopAuthenticator,
    private val apiFactory: (String) -> GitHubApi,
    initialToken: String,
    initialUser: String,
) {
    var githubSnapshot by mutableStateOf<GitHubSnapshot?>(null)
    var connectionError by mutableStateOf<String?>(null)
    var isConnecting by mutableStateOf(false)
    var isAuthorizing by mutableStateOf(false)
    var oauthToken by mutableStateOf(initialToken)
    var currentUserOverride by mutableStateOf(initialUser)
    var deviceUserCode by mutableStateOf<String?>(null)
    var deviceVerificationUrl by mutableStateOf<String?>(null)

    val currentUser: String
        get() = githubSnapshot?.viewerLogin ?: currentUserOverride

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
            deviceUserCode = null
            deviceVerificationUrl = null
            connect(owner = owner, repo = repo)
        }.onFailure { error ->
            connectionError = error.message?.take(220) ?: "OAuth failed"
        }
        isAuthorizing = false
    }

    suspend fun refresh(owner: String, repo: String) {
        connect(owner = owner, repo = repo)
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
            connectionError = error.message?.take(220) ?: "Unknown error"
        }
        isConnecting = false
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
