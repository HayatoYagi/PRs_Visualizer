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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.FileCommit
import io.github.hayatoyagi.prvisualizer.github.GitHubApi
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import io.github.hayatoyagi.prvisualizer.ui.theme.prColor
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.launch

@Composable
fun FileDetailsDialog(
    filePath: String,
    fileName: String,
    totalLines: Int,
    fileOverlay: FileOverlay?,
    repoFullName: String,
    defaultBranch: String,
    prColorMap: Map<String, Color>,
    githubApi: GitHubApi,
    onDismiss: () -> Unit,
) {
    var commits by remember { mutableStateOf<List<FileCommit>?>(null) }
    var isLoadingCommits by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(filePath) {
        isLoadingCommits = true
        scope.launch {
            try {
                val owner = repoFullName.substringBefore('/')
                val repo = repoFullName.substringAfter('/')
                commits = githubApi.fetchFileCommits(owner, repo, filePath, limit = 10)
            } catch (e: Exception) {
                commits = emptyList()
            } finally {
                isLoadingCommits = false
            }
        }
    }

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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // File Information Section
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

                HorizontalDivider(color = AppColors.prListDivider)

                // Open File Button
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

                HorizontalDivider(color = AppColors.prListDivider)

                // Pull Requests Section
                if (fileOverlay != null && fileOverlay.prs.isNotEmpty()) {
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
                            items(fileOverlay.prs) { pr ->
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
                        }
                    }

                    HorizontalDivider(color = AppColors.prListDivider)
                }

                // Commit History Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Recent Commits",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPaneTitle,
                    )
                    if (isLoadingCommits) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp)
                                .background(AppColors.backgroundPaneList, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    } else if (commits.isNullOrEmpty()) {
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
                    } else {
                        val commitItems = commits.orEmpty()
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(AppColors.backgroundPaneList, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(commitItems) { commit ->
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
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = AppColors.textPrimary) }
        },
    )
}

private fun encodeGitHubPathPart(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}

private fun formatDate(isoDate: String): String = try {
    // ISO 8601 format: 2026-02-20T13:58:03Z
    // Returns MM/DD format without year for brevity in UI
    // Full date is visible on hover and in GitHub when clicking commit
    val date = isoDate.substringBefore('T')
    val parts = date.split('-')
    if (parts.size == 3) {
        "${parts[1]}/${parts[2]}"
    } else {
        isoDate.take(10)
    }
} catch (e: Exception) {
    isoDate.take(10)
}
