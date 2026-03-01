package io.github.hayatoyagi.prvisualizer.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.AuthState
import io.github.hayatoyagi.prvisualizer.SnapshotFetchState
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import io.github.hayatoyagi.prvisualizer.ui.toolbar.sections.AuthSection
import io.github.hayatoyagi.prvisualizer.ui.toolbar.sections.ConnectionSection
import io.github.hayatoyagi.prvisualizer.ui.toolbar.sections.DevicePromptSection
import io.github.hayatoyagi.prvisualizer.ui.toolbar.sections.RepositorySection

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
    authState: AuthState,
    snapshotFetchState: SnapshotFetchState,
    currentUser: String,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onOpenRepoDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val model = toolbarModel(authState = authState, snapshotFetchState = snapshotFetchState)
    val repoFullName = "${owner.trim()}/${repo.trim()}".trim('/').ifBlank { "Repository not selected" }
    val toolbarTextStyle = MaterialTheme.typography.bodySmall

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.backgroundHeader)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AuthSection(
            isLoggedIn = model.isLoggedIn,
            isAuthorizing = model.isAuthorizing,
            onLogin = onLogin,
            onLogout = onLogout,
        )
        DevicePromptSection(
            devicePrompt = model.devicePrompt,
            toolbarTextStyle = toolbarTextStyle,
        )
        ConnectionSection(
            connectionError = model.connectionError,
            statusText = statusText(model = model, currentUser = currentUser),
            toolbarTextStyle = toolbarTextStyle,
        )
        Spacer(modifier = Modifier.weight(1f))
        RepositorySection(
            isConnecting = model.isConnecting,
            canRefresh = model.isLoggedIn,
            canOpenRepoDialog = model.isLoggedIn,
            repoFullName = repoFullName,
            toolbarTextStyle = toolbarTextStyle,
            onRefresh = onRefresh,
            onOpenRepoDialog = onOpenRepoDialog,
        )
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
