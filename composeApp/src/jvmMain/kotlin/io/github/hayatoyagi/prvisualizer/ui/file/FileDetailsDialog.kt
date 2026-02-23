package io.github.hayatoyagi.prvisualizer.ui.file

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.DialogState
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import io.github.hayatoyagi.prvisualizer.ui.theme.prColor
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val ISO_DATE_PREFIX_LENGTH = 10
private const val ISO_DATE_PARTS_COUNT = 3

@Composable
fun FileDetailsDialog(
    filePath: String,
    fileName: String,
    totalLines: Int,
    fileOverlay: FileOverlay?,
    repoFullName: String,
    defaultBranch: String,
    prColorMap: Map<String, Color>,
    commitsState: DialogState.FileDetails.CommitsState,
    onRetryLoadCommits: () -> Unit,
    onDismiss: () -> Unit,
) {
    val encodedBranch = encodeGitHubPathPart(defaultBranch)
    val encodedFilePath = filePath
        .split('/')
        .joinToString("/") { encodeGitHubPathPart(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.backgroundPane,
        titleContentColor = AppColors.textPaneTitle,
        textContentColor = AppColors.textBody,
        title = {
            Text(
                text = fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            FileDetailsDialogContent(
                filePath = filePath,
                totalLines = totalLines,
                fileOverlay = fileOverlay,
                repoFullName = repoFullName,
                encodedBranch = encodedBranch,
                encodedFilePath = encodedFilePath,
                prColorMap = prColorMap,
                commitsState = commitsState,
                onRetryLoadCommits = onRetryLoadCommits,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = AppColors.textPrimary) }
        },
    )
}

@Composable
private fun FileDetailsDialogContent(
    filePath: String,
    totalLines: Int,
    fileOverlay: FileOverlay?,
    repoFullName: String,
    encodedBranch: String,
    encodedFilePath: String,
    prColorMap: Map<String, Color>,
    commitsState: DialogState.FileDetails.CommitsState,
    onRetryLoadCommits: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FileInfoSection(filePath = filePath, totalLines = totalLines, fileOverlay = fileOverlay)
        HorizontalDivider(color = AppColors.prListDivider)
        OpenFileSection(repoFullName = repoFullName, encodedBranch = encodedBranch, encodedFilePath = encodedFilePath)
        HorizontalDivider(color = AppColors.prListDivider)
        RelatedPrsSection(fileOverlay = fileOverlay, prColorMap = prColorMap)
        CommitsSection(commitsState = commitsState, onRetryLoadCommits = onRetryLoadCommits)
    }
}

@Composable
private fun FileInfoSection(
    filePath: String,
    totalLines: Int,
    fileOverlay: FileOverlay?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "File Information",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPaneTitle,
        )
        Text("Path: $filePath", color = AppColors.textTooltip, style = MaterialTheme.typography.bodySmall)
        Text("Lines: $totalLines", color = AppColors.textTooltip, style = MaterialTheme.typography.bodySmall)
        if (fileOverlay != null) {
            Text(
                text = "PRs: ${fileOverlay.prs.size}",
                color = if (fileOverlay.prs.size > 1) AppColors.textTooltipMultiPr else AppColors.textTooltip,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun OpenFileSection(
    repoFullName: String,
    encodedBranch: String,
    encodedFilePath: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(
            onClick = { openUrl("https://github.com/$repoFullName/blob/$encodedBranch/$encodedFilePath") },
            modifier = Modifier.weight(1f),
        ) {
            Text("🔗 Open File on GitHub")
        }
    }
}

@Composable
private fun RelatedPrsSection(
    fileOverlay: FileOverlay?,
    prColorMap: Map<String, Color>,
) {
    val overlay = fileOverlay?.takeIf { it.prs.isNotEmpty() } ?: return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Related Pull Requests",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPaneTitle,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .background(AppColors.backgroundPaneList, RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(overlay.prs) { pr ->
                RelatedPrItem(pr = pr, prColorMap = prColorMap)
            }
        }
    }
    HorizontalDivider(color = AppColors.prListDivider)
}

@Composable
private fun RelatedPrItem(
    pr: io.github.hayatoyagi.prvisualizer.PullRequest,
    prColorMap: Map<String, Color>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = prColor(pr, prColorMap),
                shape = RoundedCornerShape(4.dp),
            )
            .background(
                if (pr.isDraft) AppColors.prItemDraft else AppColors.prItemNormal,
                RoundedCornerShape(4.dp),
            )
            .clickable { openUrl(pr.url) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(prColor(pr, prColorMap)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "#${pr.number} ${pr.title}",
                color = AppColors.textPrItem,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "${pr.author}${if (pr.isDraft) " • draft" else ""}",
                color = AppColors.textMeta,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = "→",
            color = AppColors.textPrItem,
            modifier = Modifier.padding(end = 4.dp),
        )
    }
}

@Composable
private fun CommitsSection(
    commitsState: DialogState.FileDetails.CommitsState,
    onRetryLoadCommits: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Recent Commits",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPaneTitle,
        )
        when (commitsState) {
            DialogState.FileDetails.CommitsState.Loading -> CommitsLoadingState()
            is DialogState.FileDetails.CommitsState.Failed -> CommitsErrorState(
                message = commitErrorMessage(commitsState.error),
                onRetryLoadCommits = onRetryLoadCommits,
            )
            is DialogState.FileDetails.CommitsState.Ready -> CommitsReadyState(commitsState)
        }
    }
}

@Composable
private fun CommitsLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .background(AppColors.backgroundPaneList, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun CommitsErrorState(
    message: String,
    onRetryLoadCommits: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.backgroundPaneList, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            color = AppColors.textError,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onRetryLoadCommits) {
            Text("Retry")
        }
    }
}

@Composable
private fun CommitsReadyState(
    commitsState: DialogState.FileDetails.CommitsState.Ready,
) {
    if (commitsState.commits.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.backgroundPaneList, RoundedCornerShape(8.dp))
                .padding(16.dp),
        ) {
            Text(
                text = "No commits found",
                color = AppColors.textBodyMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .background(AppColors.backgroundPaneList, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(commitsState.commits) { commit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.prItemNormal, RoundedCornerShape(4.dp))
                    .clickable { openUrl(commit.url) }
                    .padding(8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = commit.sha,
                    color = AppColors.textMeta,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = commit.message,
                        color = AppColors.textPrItem,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "${commit.author} • ${formatDate(commit.date)}",
                        color = AppColors.textMeta,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = "→",
                    color = AppColors.textPrItem,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun commitErrorMessage(error: AppError): String = when (error) {
    is AppError.Network -> "Network error: ${error.message}"
    is AppError.ApiError -> "GitHub error ${error.statusCode}: ${error.message}"
    is AppError.AuthExpired -> error.message
    is AppError.OAuthFailed -> "OAuth failed: ${error.message}"
    is AppError.Unknown -> "Error: ${error.message}"
}

private fun encodeGitHubPathPart(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

private fun formatDate(isoDate: String): String {
    // ISO 8601 format: 2026-02-20T13:58:03Z
    // Returns MM/DD format without year for brevity in UI
    // Full date is visible on hover and in GitHub when clicking commit
    val date = isoDate.substringBefore('T')
    val parts = date.split('-')
    return if (parts.size == ISO_DATE_PARTS_COUNT) {
        "${parts[1]}/${parts[2]}"
    } else {
        isoDate.take(ISO_DATE_PREFIX_LENGTH)
    }
}
