package io.github.hayatoyagi.prvisualizer.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.filetree.findFileNode
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.state.DialogState
import io.github.hayatoyagi.prvisualizer.state.RepoSelectionState
import io.github.hayatoyagi.prvisualizer.state.SnapshotFetchState
import io.github.hayatoyagi.prvisualizer.ui.file.FileDetailsDialog
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrDetailsDialog
import io.github.hayatoyagi.prvisualizer.ui.repo.RepoPickerDialog
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

/**
 * Centralized dialog routing host that manages all application dialogs.
 *
 * Handles display and routing for:
 * - Repository picker dialog
 * - File details dialog
 * - PR details dialog
 * - Error dialogs
 */
@Composable
fun DialogHost(
    dialogState: DialogState,
    selectedRepo: RepoState.Selected?,
    ready: SnapshotFetchState.Ready?,
    prColorMap: Map<String, Color>,
    repoSelectionState: RepoSelectionState,
    onReloadRepoOptions: () -> Unit,
    onDismissRepoDialog: () -> Unit,
    onSelectRepo: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetryLoadCommits: () -> Unit,
    onDismissDialog: () -> Unit,
    onDismissErrorDialog: () -> Unit,
    onOpenPrInBrowser: (String) -> Unit,
    onSelectFile: (String) -> Unit,
) {
    when (dialogState) {
        is DialogState.RepoPicker -> RepoPickerDialog(
            initialQuery = "${selectedRepo?.owner.orEmpty()}/${selectedRepo?.repo.orEmpty()}".trim().trim('/'),
            repoSelectionState = repoSelectionState,
            onReload = onReloadRepoOptions,
            onDismiss = onDismissRepoDialog,
            onSelect = { fullName ->
                onSelectRepo(fullName)
                onRefresh()
            },
        )
        is DialogState.FileDetails -> if (ready != null) {
            FileDetailsDialogHost(
                dialogState = dialogState,
                ready = ready,
                selectedRepo = selectedRepo,
                prColorMap = prColorMap,
                onRetryLoadCommits = onRetryLoadCommits,
                onDismiss = onDismissDialog,
            )
        }
        is DialogState.PrDetails -> PrDetailsDialog(
            pr = dialogState.pr,
            onDismiss = onDismissDialog,
            onOpenInBrowser = { url ->
                onOpenPrInBrowser(url)
            },
            onSelectFile = { filePath ->
                onSelectFile(filePath)
            },
        )
        is DialogState.AuthError -> ErrorDialog(
            title = "Authentication error",
            error = dialogState.error,
            onDismiss = onDismissErrorDialog,
        )
        is DialogState.SnapshotFetchError -> ErrorDialog(
            title = "Failed to load repository",
            error = dialogState.error,
            onDismiss = onDismissErrorDialog,
        )
        is DialogState.DeviceFlowPrompt -> DeviceFlowDialog(
            userCode = dialogState.userCode,
            verificationUrl = dialogState.verificationUrl,
            browserOpenedAutomatically = dialogState.browserOpenedAutomatically,
            onDismiss = onDismissDialog,
        )
        is DialogState.None -> Unit
    }
}

@Composable
private fun ErrorDialog(
    title: String,
    error: AppError,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(errorDialogMessage(error)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = AppColors.backgroundPane,
        titleContentColor = AppColors.textPaneTitle,
        textContentColor = AppColors.textBody,
    )
}

private fun errorDialogMessage(error: AppError): String = when (error) {
    is AppError.Network -> "Network error: ${error.message}"
    is AppError.ApiError -> "GitHub error ${error.statusCode}: ${error.message}"
    is AppError.AuthExpired -> error.message
    is AppError.OAuthFailed -> "OAuth failed: ${error.message}"
    is AppError.Unknown -> "Error: ${error.message}"
}

@Composable
private fun FileDetailsDialogHost(
    dialogState: DialogState.FileDetails,
    ready: SnapshotFetchState.Ready,
    selectedRepo: RepoState.Selected?,
    prColorMap: Map<String, Color>,
    onRetryLoadCommits: () -> Unit,
    onDismiss: () -> Unit,
) {
    val filePath = dialogState.filePath
    val fileNode = remember(ready.focusRoot, filePath) { findFileNode(ready.focusRoot, filePath) } ?: return
    FileDetailsDialog(
        filePath = filePath,
        fileName = filePath.substringAfterLast('/'),
        totalLines = fileNode.totalLines,
        fileOverlay = ready.fileOverlayByPath[filePath],
        repoFullName = "${selectedRepo?.owner.orEmpty().trim()}/${selectedRepo?.repo.orEmpty().trim()}",
        defaultBranch = dialogState.defaultBranch,
        prColorMap = prColorMap,
        commitsState = dialogState.commitsState,
        onRetryLoadCommits = onRetryLoadCommits,
        onDismiss = onDismiss,
    )
}
