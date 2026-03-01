package io.github.hayatoyagi.prvisualizer.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.AuthState
import io.github.hayatoyagi.prvisualizer.SnapshotFetchState
import io.github.hayatoyagi.prvisualizer.ui.shared.copyToClipboard
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.shared.TooltipIconButton
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private data class ToolbarModel(
    val isAuthorizing: Boolean,
    val isConnecting: Boolean,
    val isLoggedIn: Boolean,
    val hasSnapshot: Boolean,
    val connectionError: AppError?,
    val devicePrompt: AuthState.Authorizing?,
)

@Composable
fun ToolbarRow(
    owner: String,
    repo: String,
    oauthClientId: String,
    authState: AuthState,
    snapshotFetchState: SnapshotFetchState,
    currentUser: String,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onOpenRepoDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val model = toolbarModel(authState = authState, snapshotFetchState = snapshotFetchState)
    val repoFullName = "${owner.trim()}/${repo.trim()}".trim('/').ifBlank { "Repository not selected" }
    val toolbarTextStyle = MaterialTheme.typography.bodySmall

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(AppColors.backgroundHeader)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        // Auth action buttons (login/refresh)
        if (!model.isLoggedIn) {
            TooltipIconButton(
                tooltip = if (model.isAuthorizing) "Authorizing..." else "Login with GitHub",
                enabled = !model.isAuthorizing && oauthClientId.isNotBlank(),
                onClick = onLogin,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Login,
                    contentDescription = if (model.isAuthorizing) "Authorizing..." else "Login with GitHub",
                    tint = if (!model.isAuthorizing && oauthClientId.isNotBlank()) {
                        AppColors.textPrimary
                    } else {
                        AppColors.textSecondary
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        // Missing client ID notice
        if (oauthClientId.isBlank()) {
            Text(
                text = "Missing GITHUB_CLIENT_ID in .env",
                color = AppColors.textWarning,
                style = toolbarTextStyle,
            )
        }

        // Device prompt section
        model.devicePrompt?.let { prompt ->
            SelectionContainer {
                Text(
                    text = "Code: ${prompt.deviceUserCode} @ ${prompt.deviceVerificationUrl}",
                    color = AppColors.textDeviceCode,
                    style = toolbarTextStyle,
                )
            }
            TooltipIconButton(
                tooltip = "Copy Code",
                onClick = { copyToClipboard(prompt.deviceUserCode.orEmpty()) },
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy Code",
                    tint = AppColors.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            TooltipIconButton(
                tooltip = "Open Verify Page",
                onClick = { openUrl(prompt.deviceVerificationUrl.orEmpty()) },
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInBrowser,
                    contentDescription = "Open Verify Page",
                    tint = AppColors.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Connection status and error
        model.connectionError?.let { error ->
            val (color, text) = connectionErrorPresentation(error)
            SelectionContainer {
                Text(
                    text = text,
                    color = color,
                    style = toolbarTextStyle,
                )
            }
        } ?: run {
            Text(
                text = statusText(model = model, currentUser = currentUser),
                color = AppColors.textSecondary,
                style = toolbarTextStyle,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Repository action buttons
        TooltipIconButton(
            tooltip = if (model.isConnecting) "Refreshing..." else "Refresh Repository",
            enabled = !model.isConnecting && model.isLoggedIn,
            onClick = onRefresh,
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = if (model.isConnecting) "Refreshing..." else "Refresh",
                tint = if (!model.isConnecting && model.isLoggedIn) {
                    AppColors.textPrimary
                } else {
                    AppColors.textSecondary
                },
                modifier = Modifier.size(20.dp),
            )
        }
        // Keep repository label and picker trigger adjacent for discoverability.
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = repoFullName,
                color = AppColors.textPrimary,
                style = toolbarTextStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TooltipIconButton(
                tooltip = "Select Repository",
                enabled = model.isLoggedIn,
                onClick = onOpenRepoDialog,
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = "Select Repo",
                    tint = if (model.isLoggedIn) AppColors.textPrimary else AppColors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        }
    }
}

private fun toolbarModel(
    authState: AuthState,
    snapshotFetchState: SnapshotFetchState,
): ToolbarModel {
    val authError = (authState as? AuthState.Failed)?.error
    val fetchError = (snapshotFetchState as? SnapshotFetchState.Failed)?.error
    return ToolbarModel(
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
    model: ToolbarModel,
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
