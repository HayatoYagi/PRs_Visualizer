package io.github.hayatoyagi.prvisualizer.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.DialogState
import io.github.hayatoyagi.prvisualizer.SnapshotFetchState
import io.github.hayatoyagi.prvisualizer.VisualizerUiState
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.ui.file.FileDetailsDialog
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrDetailsDialog
import io.github.hayatoyagi.prvisualizer.ui.repo.RepoPickerDialog
import io.github.hayatoyagi.prvisualizer.ui.shared.findFileNode
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private const val DEFAULT_BRANCH = "main"

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
    uiState: VisualizerUiState,
    snapshotFetchState: SnapshotFetchState,
    prColorMap: Map<String, androidx.compose.ui.graphics.Color>,
    repoSelectionState: io.github.hayatoyagi.prvisualizer.RepoSelectionState,
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
        is DialogState.FileDetails -> FileDetailsDialogHost(
            dialogState = dialogState,
            uiState = uiState,
            selectedRepo = selectedRepo,
            snapshotFetchState = snapshotFetchState,
            prColorMap = prColorMap,
            onRetryLoadCommits = onRetryLoadCommits,
            onDismiss = onDismissDialog,
        )
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
    uiState: VisualizerUiState,
    selectedRepo: RepoState.Selected?,
    snapshotFetchState: SnapshotFetchState,
    prColorMap: Map<String, androidx.compose.ui.graphics.Color>,
    onRetryLoadCommits: () -> Unit,
    onDismiss: () -> Unit,
) {
    val filePath = dialogState.filePath
    val fileNode = remember(uiState.focusRoot, filePath) { findFileNode(uiState.focusRoot, filePath) } ?: return
    FileDetailsDialog(
        filePath = filePath,
        fileName = filePath.substringAfterLast('/'),
        totalLines = fileNode.totalLines,
        fileOverlay = uiState.fileOverlayByPath[filePath],
        repoFullName = "${selectedRepo?.owner.orEmpty().trim()}/${selectedRepo?.repo.orEmpty().trim()}",
        defaultBranch = defaultBranch(snapshotFetchState),
        prColorMap = prColorMap,
        commitsState = dialogState.commitsState,
        onRetryLoadCommits = onRetryLoadCommits,
        onDismiss = onDismiss,
    )
}

private fun defaultBranch(snapshotFetchState: SnapshotFetchState): String = when (snapshotFetchState) {
    is SnapshotFetchState.Ready -> snapshotFetchState.snapshot.defaultBranch
    SnapshotFetchState.Idle, SnapshotFetchState.Fetching, is SnapshotFetchState.Failed -> DEFAULT_BRANCH
}
