package io.github.hayatoyagi.prvisualizer.ui.explorer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.ui.shared.ExplorerRow
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

private data class StatusLabel(
    val symbol: String,
    val color: Color,
    val isConflict: Boolean = false,
)

@Composable
fun ExplorerPane(
    rows: List<ExplorerRow>,
    focusPath: String,
    selectedPath: String?,
    onSelectDirectory: (String) -> Unit,
    onSelectFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    @Composable
    fun LegendBadge(symbol: String, label: String, color: Color, isConflict: Boolean = false) {
        val textStyle = TextStyle(
            fontSize = if (isConflict) 10.sp else 9.sp,
            fontWeight = if (isConflict) FontWeight.ExtraBold else FontWeight.Bold,
            lineHeight = 10.sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = color.copy(alpha = if (isConflict) 0.28f else 0.22f),
                        shape = if (isConflict) RectangleShape else MaterialTheme.shapes.extraSmall,
                    )
                    .then(
                        if (isConflict) Modifier.border(1.dp, color, RectangleShape) else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = symbol,
                    color = color,
                    style = textStyle,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = label,
                color = AppColors.textSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }

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
                LegendBadge("!", "Conf", AppColors.treemapConflictStripe, isConflict = true)
                LegendBadge("+", "Add", AppColors.treemapAddition)
                LegendBadge("~", "Mod", AppColors.treemapModification)
                LegendBadge("-", "Del", AppColors.treemapDeletion)
            }
        }
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
                Box(
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
                ) {
                    val statusLabel = when {
                        row.hasConflict -> StatusLabel("!", AppColors.treemapConflictStripe, isConflict = true)
                        row.dominantType == ChangeType.Addition -> StatusLabel("+", AppColors.treemapAddition)
                        row.dominantType == ChangeType.Modification -> StatusLabel("~", AppColors.treemapModification)
                        row.dominantType == ChangeType.Deletion -> StatusLabel("-", AppColors.treemapDeletion)
                        else -> null
                    }

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
                        statusLabel?.let {
                            val textStyle = TextStyle(
                                fontSize = if (it.isConflict) 11.sp else 10.sp,
                                fontWeight = if (it.isConflict) FontWeight.ExtraBold else FontWeight.Bold,
                                lineHeight = 11.sp,
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        color = it.color.copy(alpha = if (it.isConflict) 0.28f else 0.22f),
                                        shape = if (it.isConflict) RectangleShape else MaterialTheme.shapes.extraSmall,
                                    )
                                    .then(
                                        if (it.isConflict) Modifier.border(1.dp, it.color, RectangleShape) else Modifier,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = it.symbol,
                                    color = it.color,
                                    style = textStyle,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
