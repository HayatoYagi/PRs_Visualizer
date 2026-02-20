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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.ui.shared.prColor
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun PrListPane(
    filteredPrs: List<PullRequest>,
    selectedPrIds: Set<String>,
    selectedPath: String?,
    prColorMap: Map<String, Color>,
    query: String,
    showDrafts: Boolean,
    onlyMine: Boolean,
    onQueryChange: (String) -> Unit,
    onShowDraftsChange: (Boolean) -> Unit,
    onOnlyMineChange: (Boolean) -> Unit,
    onTogglePr: (prId: String, checked: Boolean) -> Unit,
    onOpenPr: (String) -> Unit,
    onCyclePrColor: (String) -> Unit,
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
        Text("Open PRs", color = AppColors.textPaneTitle, style = MaterialTheme.typography.titleLarge)
        TextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = showDrafts, onCheckedChange = onShowDraftsChange)
            Text("Show draft", color = AppColors.textBodyMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = onlyMine, onCheckedChange = onOnlyMineChange)
            Text("Only my PRs", color = AppColors.textBodyMuted)
        }
        HorizontalDivider(color = AppColors.prListDivider)
        if (isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AppColors.textPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filteredPrs, key = { it.id }) { pr ->
                val checked = selectedPrIds.contains(pr.id)
                val relatedToSelection = selectedPath != null && pr.files.any { it.path == selectedPath }
                val listBorderColor = if (checked) prColor(pr, prColorMap) else prColor(pr, prColorMap).copy(alpha = 0.45f)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (relatedToSelection) 3.dp else 2.dp,
                            color = listBorderColor,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(8.dp),
                    color = if (pr.isDraft) AppColors.prItemDraft else AppColors.prItemNormal,
                    onClick = { onOpenPr(pr.url) },
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onTogglePr(pr.id, it) },
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(top = 4.dp, end = 4.dp)
                                .size(24.dp)
                                .clickable { onCyclePrColor(pr.id) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(prColor(pr, prColorMap)),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
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
                }
            }
        }
        }
        Text(
            text = "Cmd+R: reset view  /  Cmd+F: clear search",
            color = AppColors.textHint,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
