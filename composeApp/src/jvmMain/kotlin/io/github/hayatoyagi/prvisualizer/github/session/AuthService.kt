package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.github.GitHubOAuthDesktopAuthenticator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AuthService {
    /**
     * Restores a previously saved OAuth token.
     *
     * @param fallbackToken Token to use if no saved token is found
     * @return The restored token or the fallback token
     */
    suspend fun restoreToken(fallbackToken: String): String

    /**
     * Initiates GitHub login flow using device authorization.
     *
     * @param clientId The GitHub OAuth client ID
     * @param onDeviceFlowStart Callback when device flow starts with prompt info
     * @return Result containing the OAuth token
     */
    suspend fun login(
        clientId: String,
        onDeviceFlowStart: (GitHubOAuthDesktopAuthenticator.DeviceFlowPrompt) -> Unit,
    ): Result<String>

    /**
     * Clears the saved OAuth token.
     */
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
