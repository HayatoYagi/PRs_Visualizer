package io.github.hayatoyagi.prvisualizer.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.ui.shared.ExplorerRow
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private data class StatusLabel(val text: String, val color: Color)

@Composable
fun ExplorerPane(
    rows: List<ExplorerRow>,
    focusPath: String,
    selectedPath: String?,
    onSelectDirectory: (String) -> Unit,
    onSelectFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(340.dp)
            .fillMaxHeight()
            .background(AppColors.backgroundPane)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Explorer", color = AppColors.textPaneTitle, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Current: /${focusPath.ifBlank { "" }}",
            color = AppColors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.backgroundPaneList),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(rows, key = { if (it.isDirectory) "d:${it.path}" else "f:${it.path}" }) { row ->
                val isCurrentDir = row.isDirectory && row.path == focusPath
                val isAncestor = row.isDirectory && focusPath.startsWith("${row.path}/")
                val isSelectedFile = !row.isDirectory && row.path == selectedPath
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isCurrentDir -> AppColors.explorerSelectionFocused
                                isSelectedFile -> AppColors.explorerSelectionFile
                                else -> Color.Transparent
                            },
                        )
                        .clickable {
                            if (row.isDirectory) onSelectDirectory(row.path) else onSelectFile(row.path)
                        }
                        .padding(vertical = 4.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.width((row.depth * 12).dp))
                    
                    // Show status label with color
                    if (row.dominantType != null || row.hasConflict) {
                        val statusLabel = when {
                            row.hasConflict -> StatusLabel("CONFLICT", AppColors.treemapConflictStripe)
                            row.dominantType == ChangeType.Addition -> StatusLabel("ADD", AppColors.treemapAddition)
                            row.dominantType == ChangeType.Modification -> StatusLabel("MOD", AppColors.treemapModification)
                            row.dominantType == ChangeType.Deletion -> StatusLabel("DEL", AppColors.treemapDeletion)
                            else -> null
                        }
                        
                        statusLabel?.let {
                            Text(
                                text = "[${it.text}]",
                                color = it.color,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                    
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
            }
        }
    }
}
