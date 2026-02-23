package io.github.hayatoyagi.prvisualizer.ui.prlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.PrFileChange
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun PrDetailsDialog(
    pr: PullRequest,
    onDismiss: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.backgroundPane,
        titleContentColor = AppColors.textPaneTitle,
        textContentColor = AppColors.textBody,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "PR #${pr.number}",
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { onOpenInBrowser(pr.url) },
                ) {
                    Text("Open", color = AppColors.textSecondary)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // PR title
                Text(
                    text = pr.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textBody,
                )

                // PR metadata
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Author:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textMeta,
                    )
                    Text(
                        text = pr.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textBody,
                    )
                    if (pr.isDraft) {
                        Text(
                            text = "• draft",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textMeta,
                        )
                    }
                }

                HorizontalDivider(color = AppColors.prListDivider)

                // Files changed section
                Text(
                    text = "Files changed (${pr.files.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textBody,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.backgroundPaneList,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(pr.files) { file ->
                            FileChangeItem(file)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { 
                Text("Close", color = AppColors.textPrimary)
            }
        },
    )
}

@Composable
private fun FileChangeItem(file: PrFileChange) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Change type indicator
        val changeTypeText = when (file.changeType) {
            ChangeType.Addition -> "+"
            ChangeType.Modification -> "M"
            ChangeType.Deletion -> "−"
        }
        val changeTypeColor = when (file.changeType) {
            ChangeType.Addition -> AppColors.treemapAddition
            ChangeType.Modification -> AppColors.treemapModification
            ChangeType.Deletion -> AppColors.treemapDeletion
        }
        
        Text(
            text = changeTypeText,
            color = changeTypeColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 4.dp),
        )

        // File path
        Text(
            text = file.path,
            color = AppColors.textBody,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )

        // Changes count
        Text(
            text = "+${file.additions} −${file.deletions}",
            color = AppColors.textMeta,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
