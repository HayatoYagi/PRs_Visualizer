package io.github.hayatoyagi.prvisualizer.ui.prlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
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

@Composable
fun PrListPane(
    filteredPrs: List<PullRequest>,
    selectedPrIds: Set<String>,
    selectedPath: String?,
    query: String,
    showDrafts: Boolean,
    onlyMine: Boolean,
    onQueryChange: (String) -> Unit,
    onShowDraftsChange: (Boolean) -> Unit,
    onOnlyMineChange: (Boolean) -> Unit,
    onTogglePr: (prId: String, checked: Boolean) -> Unit,
    onOpenPr: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(340.dp)
            .fillMaxHeight()
            .background(Color(0xFF18212B))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Open PRs", color = Color(0xFFE8F1F8), style = MaterialTheme.typography.titleLarge)
        TextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = showDrafts, onCheckedChange = onShowDraftsChange)
            Text("Show draft", color = Color(0xFFD1DEEB))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = onlyMine, onCheckedChange = onOnlyMineChange)
            Text("Only my PRs", color = Color(0xFFD1DEEB))
        }
        HorizontalDivider(color = Color(0xFF2C3D4E))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(filteredPrs, key = { it.id }) { pr ->
                val checked = selectedPrIds.contains(pr.id)
                val relatedToSelection = selectedPath != null && pr.files.any { it.path == selectedPath }
                val listBorderColor = if (checked) prColor(pr) else prColor(pr).copy(alpha = 0.45f)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (relatedToSelection) 3.dp else 2.dp,
                            color = listBorderColor,
                            shape = MaterialTheme.shapes.medium,
                        )
                        .padding(8.dp),
                    color = if (pr.isDraft) Color(0xFF253748) else Color(0xFF203041),
                    onClick = { onOpenPr(pr.url) },
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onTogglePr(pr.id, it) },
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 8.dp)
                                .width(12.dp)
                                .fillMaxHeight(0.2f)
                                .background(prColor(pr)),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${pr.number} ${pr.title}",
                                color = Color(0xFFEAF2F8),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (relatedToSelection) FontWeight.Bold else FontWeight.Normal,
                            )
                            Text(
                                text = "${pr.author}${if (pr.isDraft) " • draft" else ""}",
                                color = Color(0xFFB5C8D8),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "Cmd+R: reset view  /  Cmd+F: clear search",
            color = Color(0xFF8FA8BC),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
