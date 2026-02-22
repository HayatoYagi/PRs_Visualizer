package io.github.hayatoyagi.prvisualizer.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.ui.explorer.badge.ExplorerBadgeSize
import io.github.hayatoyagi.prvisualizer.ui.explorer.badge.ExplorerStatusBadge
import io.github.hayatoyagi.prvisualizer.ui.explorer.badge.ExplorerStatusKind
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private fun ExplorerRow.statusKindOrNull(): ExplorerStatusKind? {
    if (hasConflict) return ExplorerStatusKind.Conflict
    return when (dominantType) {
        ChangeType.Addition -> ExplorerStatusKind.Addition
        ChangeType.Modification -> ExplorerStatusKind.Modification
        ChangeType.Deletion -> ExplorerStatusKind.Deletion
        null -> null
    }
}

@Composable
fun ExplorerPane(
    rows: List<ExplorerRow>,
    focusPath: String,
    selectedPath: String?,
    onSelectDirectory: (String) -> Unit,
    onSelectFile: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Column(
        modifier = modifier
            .width(340.dp)
            .fillMaxHeight()
            .background(AppColors.backgroundPane)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Explorer", color = AppColors.textPaneTitle, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                ExplorerStatusBadge(kind = ExplorerStatusKind.Conflict, withLabel = true, size = ExplorerBadgeSize.Legend)
                ExplorerStatusBadge(kind = ExplorerStatusKind.Addition, withLabel = true, size = ExplorerBadgeSize.Legend)
                ExplorerStatusBadge(kind = ExplorerStatusKind.Modification, withLabel = true, size = ExplorerBadgeSize.Legend)
                ExplorerStatusBadge(kind = ExplorerStatusKind.Deletion, withLabel = true, size = ExplorerBadgeSize.Legend)
            }
        }
        Text(
            text = "Current: /${focusPath.ifBlank { "" }}",
            color = AppColors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AppColors.textPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AppColors.backgroundPaneList),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(rows, key = { if (it.isDirectory) "d:${it.path}" else "f:${it.path}" }) { row ->
                    val isCurrentDir = row.isDirectory && row.path == focusPath
                    val isAncestor = row.isDirectory && focusPath.startsWith("${row.path}/")
                    val isSelectedFile = !row.isDirectory && row.path == selectedPath
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    isCurrentDir -> AppColors.explorerSelectionFocused
                                    isSelectedFile -> AppColors.explorerSelectionFile
                                    else -> Color.Transparent
                                },
                            ).clickable {
                                if (row.isDirectory) onSelectDirectory(row.path) else onSelectFile(row.path)
                            }.padding(vertical = 4.dp, horizontal = 6.dp),
                    ) {
                        val statusKind = row.statusKindOrNull()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 30.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(modifier = Modifier.width((row.depth * 12).dp))
                            Text(
                                text = if (row.isDirectory) "${row.name}/" else row.name,
                                color = when {
                                    isCurrentDir -> Color.White
                                    isAncestor -> AppColors.explorerAncestorText
                                    else -> AppColors.textBody
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(24.dp),
                        ) {
                            statusKind?.let {
                                ExplorerStatusBadge(kind = it, withLabel = false, size = ExplorerBadgeSize.Row)
                            }
                        }
                    }
                }
            }
        }
    }
}
