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
import io.github.hayatoyagi.prvisualizer.ui.shared.ExplorerRow

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
            .background(Color(0xFF18212B))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Explorer", color = Color(0xFFE8F1F8), style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Current: /${focusPath.ifBlank { "" }}",
            color = Color(0xFF9EC4DD),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13202B)),
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
                                isCurrentDir -> Color(0xFF2A455B)
                                isSelectedFile -> Color(0xFF2B3A4A)
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
                    Text(
                        text = if (row.isDirectory) "[D]" else "[F]",
                        color = if (row.isDirectory) Color(0xFFFFD37A) else Color(0xFF96B2C8),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (row.isDirectory) "${row.name}/" else row.name,
                        color = when {
                            isCurrentDir -> Color.White
                            isAncestor -> Color(0xFFFFE4A5)
                            else -> Color(0xFFD7E4EE)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
