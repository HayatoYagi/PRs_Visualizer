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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.state.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.state.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.explorer.badge.ExplorerBadgeSize
import io.github.hayatoyagi.prvisualizer.ui.explorer.badge.ExplorerStatusBadge
import io.github.hayatoyagi.prvisualizer.ui.explorer.badge.ExplorerStatusKind
import io.github.hayatoyagi.prvisualizer.ui.shared.TooltipIconButton
import io.github.hayatoyagi.prvisualizer.ui.shared.copyToClipboard
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private val CHEVRON_ICON_WIDTH = 12.dp
private val CHEVRON_ICON_PADDING = 4.dp
private val CHEVRON_TOTAL_WIDTH = CHEVRON_ICON_WIDTH + CHEVRON_ICON_PADDING
private val INDENT_PER_LEVEL = 12.dp

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
    root: FileNode.Directory?,
    fileOverlayByPath: Map<String, FileOverlay>,
    directoryOverlayByPath: Map<String, DirectoryOverlay>,
    focusPath: String,
    selectedPath: String?,
    expandedPaths: Set<String>,
    onSelectDirectory: (String) -> Unit,
    onSelectFile: (String) -> Unit,
    onToggleExpanded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(root, fileOverlayByPath, directoryOverlayByPath, expandedPaths) {
        root?.let {
            buildExplorerRows(
                root = it,
                fileOverlayByPath = fileOverlayByPath,
                directoryOverlayByPath = directoryOverlayByPath,
                expandedPaths = expandedPaths,
            )
        } ?: emptyList()
    }

    Column(
        modifier = modifier
            .width(340.dp)
            .fillMaxHeight()
            .background(AppColors.backgroundPane)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExplorerHeader(focusPath = focusPath)
        ExplorerBody(
            rows = rows,
            focusPath = focusPath,
            selectedPath = selectedPath,
            expandedPaths = expandedPaths,
            onSelectDirectory = onSelectDirectory,
            onSelectFile = onSelectFile,
            onToggleExpanded = onToggleExpanded,
            contentModifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun ExplorerHeader(focusPath: String) {
    val currentPath = "/${focusPath.ifBlank { "" }}"
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Current: $currentPath",
            color = AppColors.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TooltipIconButton(
            tooltip = "Copy current path",
            onClick = { copyToClipboard(currentPath) },
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copy current path",
                tint = AppColors.textSecondary,
            )
        }
    }
}

@Composable
private fun ExplorerBody(
    rows: List<ExplorerRow>,
    focusPath: String,
    selectedPath: String?,
    expandedPaths: Set<String>,
    onSelectDirectory: (String) -> Unit,
    onSelectFile: (String) -> Unit,
    onToggleExpanded: (String) -> Unit,
    contentModifier: Modifier,
) {
    LazyColumn(
        modifier = contentModifier.background(AppColors.backgroundPaneList),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(rows, key = { if (it.isDirectory) "d:${it.path}" else "f:${it.path}" }) { row ->
            ExplorerRowItem(
                row = row,
                focusPath = focusPath,
                selectedPath = selectedPath,
                expandedPaths = expandedPaths,
                onSelectDirectory = onSelectDirectory,
                onSelectFile = onSelectFile,
                onToggleExpanded = onToggleExpanded,
            )
        }
    }
}

@Composable
private fun ExplorerRowItem(
    row: ExplorerRow,
    focusPath: String,
    selectedPath: String?,
    expandedPaths: Set<String>,
    onSelectDirectory: (String) -> Unit,
    onSelectFile: (String) -> Unit,
    onToggleExpanded: (String) -> Unit,
) {
    val isCurrentDir = row.isDirectory && row.path == focusPath
    val isAncestor = row.isDirectory && focusPath.startsWith("${row.path}/")
    val isSelectedFile = !row.isDirectory && row.path == selectedPath
    val statusKind = row.statusKindOrNull()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(explorerRowBackgroundColor(isCurrentDir = isCurrentDir, isSelectedFile = isSelectedFile))
            .clickable { onExplorerRowClick(row = row, onSelectDirectory = onSelectDirectory, onSelectFile = onSelectFile) }
            .padding(vertical = 4.dp, horizontal = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(INDENT_PER_LEVEL * row.depth))
            ExplorerChevron(
                row = row,
                isCurrentDir = isCurrentDir,
                isAncestor = isAncestor,
                expandedPaths = expandedPaths,
                onToggleExpanded = onToggleExpanded,
            )
            Text(
                text = explorerLabel(row),
                color = explorerLabelColor(isCurrentDir = isCurrentDir, isAncestor = isAncestor),
                style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun ExplorerChevron(
    row: ExplorerRow,
    isCurrentDir: Boolean,
    isAncestor: Boolean,
    expandedPaths: Set<String>,
    onToggleExpanded: (String) -> Unit,
) {
    if (!row.isDirectory) {
        Spacer(modifier = Modifier.width(CHEVRON_TOTAL_WIDTH))
        return
    }
    val isExpanded = expandedPaths.contains(row.path)
    Text(
        text = if (isExpanded) "▼" else "▶",
        color = explorerChevronColor(isCurrentDir = isCurrentDir, isAncestor = isAncestor),
        modifier = Modifier
            .padding(end = CHEVRON_ICON_PADDING)
            .clickable { onToggleExpanded(row.path) },
    )
}

private fun explorerLabel(row: ExplorerRow): String = if (row.isDirectory) "${row.name}/" else row.name

private fun explorerLabelColor(
    isCurrentDir: Boolean,
    isAncestor: Boolean,
): Color = when {
    isCurrentDir -> Color.White
    isAncestor -> AppColors.explorerAncestorText
    else -> AppColors.textBody
}

private fun explorerChevronColor(
    isCurrentDir: Boolean,
    isAncestor: Boolean,
): Color = when {
    isCurrentDir -> Color.White
    isAncestor -> AppColors.explorerAncestorText
    else -> AppColors.textSecondary
}

private fun explorerRowBackgroundColor(
    isCurrentDir: Boolean,
    isSelectedFile: Boolean,
): Color = when {
    isCurrentDir -> AppColors.explorerSelectionFocused
    isSelectedFile -> AppColors.explorerSelectionFile
    else -> Color.Transparent
}

private fun onExplorerRowClick(
    row: ExplorerRow,
    onSelectDirectory: (String) -> Unit,
    onSelectFile: (String) -> Unit,
) {
    if (row.isDirectory) onSelectDirectory(row.path) else onSelectFile(row.path)
}
