package io.github.hayatoyagi.prvisualizer.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.AuthState
import io.github.hayatoyagi.prvisualizer.SnapshotFetchState
import io.github.hayatoyagi.prvisualizer.ui.shared.copyToClipboard
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private data class AuthRowModel(
    val isAuthorizing: Boolean,
    val isConnecting: Boolean,
    val isLoggedIn: Boolean,
    val hasSnapshot: Boolean,
    val connectionError: AppError?,
    val devicePrompt: AuthState.Authorizing?,
)

/**
 * Displays the authentication row with login controls and status.
 *
 * @param oauthClientId The GitHub OAuth client ID
 * @param authState The current authentication state
 * @param snapshotFetchState The current snapshot fetch state
 * @param currentUser The current user's login name
 * @param onLogin Callback to initiate login
 * @param onRefresh Callback to refresh the connection
 * @param modifier Modifier for the row
 */
@Composable
fun AuthRow(
    oauthClientId: String,
    authState: AuthState,
    snapshotFetchState: SnapshotFetchState,
    currentUser: String,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val model = authRowModel(authState = authState, snapshotFetchState = snapshotFetchState)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.backgroundHeader)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AuthActionButtons(model = model, oauthClientId = oauthClientId, onLogin = onLogin, onRefresh = onRefresh)
        MissingClientIdNotice(oauthClientId = oauthClientId)
        DevicePromptSection(devicePrompt = model.devicePrompt)
        ConnectionStatusText(model = model, currentUser = currentUser)
        ConnectionErrorText(connectionError = model.connectionError)
    }
}

@Composable
private fun AuthActionButtons(
    model: AuthRowModel,
    oauthClientId: String,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
) {
    if (!model.isLoggedIn) {
        Button(
            enabled = !model.isAuthorizing && oauthClientId.isNotBlank(),
            onClick = onLogin,
        ) {
            Text(if (model.isAuthorizing) "Authorizing..." else "Login with GitHub")
        }
    }
    Button(
        enabled = !model.isConnecting && model.isLoggedIn,
        onClick = onRefresh,
    ) {
        Text(if (model.isConnecting) "Refreshing..." else "Refresh")
    }
}

@Composable
private fun MissingClientIdNotice(oauthClientId: String) {
    if (oauthClientId.isNotBlank()) return
    Text(
        text = "Missing GITHUB_CLIENT_ID in .env",
        color = AppColors.textWarning,
        modifier = Modifier.padding(top = 14.dp),
    )
}

@Composable
private fun DevicePromptSection(devicePrompt: AuthState.Authorizing?) {
    val prompt = devicePrompt ?: return
    SelectionContainer {
        Text(
            text = "GitHub code: ${prompt.deviceUserCode} @ ${prompt.deviceVerificationUrl}",
            color = AppColors.textDeviceCode,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
    Button(onClick = { copyToClipboard(prompt.deviceUserCode.orEmpty()) }) { Text("Copy Code") }
    Button(onClick = { openUrl(prompt.deviceVerificationUrl.orEmpty()) }) { Text("Open Verify Page") }
}

@Composable
private fun ConnectionStatusText(
    model: AuthRowModel,
    currentUser: String,
) {
    Text(
        text = statusText(model = model, currentUser = currentUser),
        color = AppColors.textSecondary,
        modifier = Modifier.padding(top = 14.dp),
    )
}

@Composable
private fun ConnectionErrorText(connectionError: AppError?) {
    val error = connectionError ?: return
    val (color, text) = connectionErrorPresentation(error)
    SelectionContainer {
        Text(
            text = text,
            color = color,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

private fun authRowModel(
    authState: AuthState,
    snapshotFetchState: SnapshotFetchState,
): AuthRowModel {
    val authError = (authState as? AuthState.Failed)?.error
    val fetchError = (snapshotFetchState as? SnapshotFetchState.Failed)?.error
    return AuthRowModel(
        isAuthorizing = authState is AuthState.Authorizing,
        isConnecting = snapshotFetchState is SnapshotFetchState.Fetching,
        isLoggedIn = authState is AuthState.Authenticated,
        hasSnapshot = snapshotFetchState is SnapshotFetchState.Ready,
        connectionError = authError ?: fetchError,
        devicePrompt = (authState as? AuthState.Authorizing)
            ?.takeIf { !it.deviceUserCode.isNullOrBlank() && !it.deviceVerificationUrl.isNullOrBlank() },
    )
}

private fun statusText(
    model: AuthRowModel,
    currentUser: String,
): String = if (model.hasSnapshot) {
    "Logged in as: $currentUser"
} else if (!model.isLoggedIn) {
    "Not connected (not logged in)"
} else {
    "Logged in as: $currentUser (not connected)"
}

private fun connectionErrorPresentation(error: AppError): Pair<androidx.compose.ui.graphics.Color, String> = when (error) {
    is AppError.AuthExpired -> AppColors.textWarning to error.message
    is AppError.Network -> AppColors.textError to "Network error: ${error.message}"
    is AppError.ApiError -> AppColors.textError to "GitHub error ${error.statusCode}: ${error.message}"
    is AppError.OAuthFailed -> AppColors.textError to "OAuth failed: ${error.message}"
    is AppError.Unknown -> AppColors.textError to "Error: ${error.message}"
}
