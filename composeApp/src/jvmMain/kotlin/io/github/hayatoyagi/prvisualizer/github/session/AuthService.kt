package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.github.GitHubOAuthDesktopAuthenticator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AuthService {
    suspend fun restoreToken(fallbackToken: String): String

    suspend fun login(
        clientId: String,
        onDeviceFlowStart: (GitHubOAuthDesktopAuthenticator.DeviceFlowPrompt) -> Unit,
    ): Result<String>

    suspend fun clearToken()
}

class AuthServiceImpl(
    private val authenticator: GitHubOAuthDesktopAuthenticator = GitHubOAuthDesktopAuthenticator(),
) : AuthService {
    override suspend fun restoreToken(fallbackToken: String): String = withContext(Dispatchers.IO) {
        GitHubTokenStore.loadToken(fallbackToken)
    }

    override suspend fun login(
        clientId: String,
        onDeviceFlowStart: (GitHubOAuthDesktopAuthenticator.DeviceFlowPrompt) -> Unit,
    ): Result<String> = runCatching {
        authenticator.authenticate(
            clientId = clientId,
            onDeviceFlowStart = onDeviceFlowStart,
        )
    }.onSuccess { token ->
        withContext(Dispatchers.IO) {
            GitHubTokenStore.saveToken(token)
        }
    }

    override suspend fun clearToken() {
        withContext(Dispatchers.IO) {
            GitHubTokenStore.clearToken()
        }
    }
}
