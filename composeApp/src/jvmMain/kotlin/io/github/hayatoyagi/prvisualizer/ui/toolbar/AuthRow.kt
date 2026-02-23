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
    val isAuthorizing = authState is AuthState.Authorizing
    val isConnecting = snapshotFetchState is SnapshotFetchState.Fetching
    val isLoggedIn = authState is AuthState.Authenticated
    val authError = (authState as? AuthState.Failed)?.error
    val fetchError = (snapshotFetchState as? SnapshotFetchState.Failed)?.error
    val connectionError: AppError? = authError ?: fetchError
    val hasSnapshot = snapshotFetchState is SnapshotFetchState.Ready
    val devicePrompt = (authState as? AuthState.Authorizing)
        ?.takeIf { !it.deviceUserCode.isNullOrBlank() && !it.deviceVerificationUrl.isNullOrBlank() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.backgroundHeader)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!isLoggedIn) {
            Button(
                enabled = !isAuthorizing && oauthClientId.isNotBlank(),
                onClick = onLogin,
            ) {
                Text(if (isAuthorizing) "Authorizing..." else "Login with GitHub")
            }
        }
        Button(
            enabled = !isConnecting && isLoggedIn,
            onClick = onRefresh,
        ) {
            Text(if (isConnecting) "Refreshing..." else "Refresh")
        }
        if (oauthClientId.isBlank()) {
            Text(
                text = "Missing GITHUB_CLIENT_ID in .env",
                color = AppColors.textWarning,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        if (devicePrompt != null) {
            SelectionContainer {
                Text(
                    text = "GitHub code: ${devicePrompt.deviceUserCode} @ ${devicePrompt.deviceVerificationUrl}",
                    color = AppColors.textDeviceCode,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            Button(onClick = { copyToClipboard(devicePrompt.deviceUserCode.orEmpty()) }) { Text("Copy Code") }
            Button(onClick = { openUrl(devicePrompt.deviceVerificationUrl.orEmpty()) }) { Text("Open Verify Page") }
        }
        Text(
            text = if (!hasSnapshot) {
                if (!isLoggedIn) {
                    "Not connected (not logged in)"
                } else {
                    "Logged in as: $currentUser (not connected)"
                }
            } else {
                "Logged in as: $currentUser"
            },
            color = AppColors.textSecondary,
            modifier = Modifier.padding(top = 14.dp),
        )
        if (connectionError != null) {
            val (color, text) = when (connectionError) {
                is AppError.AuthExpired -> AppColors.textWarning to connectionError.message
                is AppError.Network -> AppColors.textError to "Network error: ${connectionError.message}"
                is AppError.ApiError -> AppColors.textError to "GitHub error ${connectionError.statusCode}: ${connectionError.message}"
                is AppError.OAuthFailed -> AppColors.textError to "OAuth failed: ${connectionError.message}"
                is AppError.Unknown -> AppColors.textError to "Error: ${connectionError.message}"
            }
            SelectionContainer {
                Text(
                    text = text,
                    color = color,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}
