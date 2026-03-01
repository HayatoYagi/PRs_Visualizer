package io.github.hayatoyagi.prvisualizer.ui.prlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.ui.shared.TooltipIconButton
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import io.github.hayatoyagi.prvisualizer.ui.theme.prColor

@Composable
fun PrListPane(
    filteredPrs: List<PullRequest>,
    visiblePrCount: Int,
    selectedPrIds: Set<String>,
    selectedPath: String?,
    prColorMap: Map<String, Color>,
    showDrafts: Boolean,
    onlyMine: Boolean,
    onShowDraftsChange: (Boolean) -> Unit,
    onOnlyMineChange: (Boolean) -> Unit,
    onTogglePr: (prId: String, checked: Boolean) -> Unit,
    onOpenPr: (PullRequest) -> Unit,
    onCyclePrColor: (String) -> Unit,
    onShuffleColors: () -> Unit,
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
        PrListHeader(
            visiblePrCount = visiblePrCount,
            showDrafts = showDrafts,
            onlyMine = onlyMine,
            onShowDraftsChange = onShowDraftsChange,
            onOnlyMineChange = onOnlyMineChange,
            canShuffleColors = prColorMap.isNotEmpty(),
            onShuffleColors = onShuffleColors,
        )
        HorizontalDivider(color = AppColors.prListDivider)
        PrListBody(
            filteredPrs = filteredPrs,
            selectedPrIds = selectedPrIds,
            selectedPath = selectedPath,
            prColorMap = prColorMap,
            onTogglePr = onTogglePr,
            onOpenPr = onOpenPr,
            onCyclePrColor = onCyclePrColor,
            isLoading = isLoading,
            contentModifier = Modifier.weight(1f),
        )
        Text(
            text = "Cmd+R: reset view",
            color = AppColors.textHint,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PrListHeader(
    visiblePrCount: Int,
    showDrafts: Boolean,
    onlyMine: Boolean,
    onShowDraftsChange: (Boolean) -> Unit,
    onOnlyMineChange: (Boolean) -> Unit,
    canShuffleColors: Boolean,
    onShuffleColors: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Open PRs", color = AppColors.textPaneTitle, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Visible PRs: $visiblePrCount",
                color = AppColors.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TooltipIconButton(
            tooltip = "Shuffle Colors",
            enabled = canShuffleColors,
            onClick = onShuffleColors,
        ) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = "Shuffle Colors",
                tint = if (canShuffleColors) AppColors.textPrimary else AppColors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
    PrFilterSwitch(checked = showDrafts, label = "Show draft", onCheckedChange = onShowDraftsChange)
    PrFilterSwitch(checked = onlyMine, label = "Only my PRs", onCheckedChange = onOnlyMineChange)
}

@Composable
private fun PrFilterSwitch(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, color = AppColors.textBodyMuted)
    }
}

@Composable
private fun PrListBody(
    filteredPrs: List<PullRequest>,
    selectedPrIds: Set<String>,
    selectedPath: String?,
    prColorMap: Map<String, Color>,
    onTogglePr: (prId: String, checked: Boolean) -> Unit,
    onOpenPr: (PullRequest) -> Unit,
    onCyclePrColor: (String) -> Unit,
    isLoading: Boolean,
    contentModifier: Modifier,
) {
    if (isLoading) {
        Box(
            modifier = contentModifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = AppColors.textPrimary)
        }
        return
    }
    LazyColumn(
        modifier = contentModifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(filteredPrs, key = { it.id }) { pr ->
            PrListItem(
                pr = pr,
                selectedPrIds = selectedPrIds,
                selectedPath = selectedPath,
                prColorMap = prColorMap,
                onTogglePr = onTogglePr,
                onOpenPr = onOpenPr,
                onCyclePrColor = onCyclePrColor,
            )
        }
    }
}

@Composable
private fun PrListItem(
    pr: PullRequest,
    selectedPrIds: Set<String>,
    selectedPath: String?,
    prColorMap: Map<String, Color>,
    onTogglePr: (prId: String, checked: Boolean) -> Unit,
    onOpenPr: (PullRequest) -> Unit,
    onCyclePrColor: (String) -> Unit,
) {
    val checked = selectedPrIds.contains(pr.id)
    val relatedToSelection = selectedPath != null && pr.files.any { it.path == selectedPath }
    val chipColor = prColor(pr, prColorMap)
    val listBorderColor = if (checked) chipColor else chipColor.copy(alpha = 0.45f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = prItemBorderWidth(relatedToSelection), color = listBorderColor, shape = MaterialTheme.shapes.medium)
            .padding(8.dp),
        color = if (pr.isDraft) AppColors.prItemDraft else AppColors.prItemNormal,
        onClick = { onOpenPr(pr) },
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onTogglePr(pr.id, it) },
            )
            ColorCycleChip(
                color = chipColor,
                onClick = { onCyclePrColor(pr.id) },
            )
            PrItemText(
                pr = pr,
                relatedToSelection = relatedToSelection,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PrItemText(
    pr: PullRequest,
    relatedToSelection: Boolean,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "#${pr.number} ${pr.title}",
            color = AppColors.textPrItem,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (relatedToSelection) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            text = "${pr.author}${if (pr.isDraft) " • draft" else ""}",
            color = AppColors.textMeta,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ColorCycleChip(
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(top = 4.dp, end = 4.dp)
            .size(24.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color),
        )
    }
}

private fun prItemBorderWidth(relatedToSelection: Boolean) = if (relatedToSelection) 3.dp else 2.dp
